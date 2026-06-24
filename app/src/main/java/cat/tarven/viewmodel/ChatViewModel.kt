package cat.tarven.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.api.ApiMessage
import cat.tarven.data.api.ChatCompletionRequest
import cat.tarven.data.api.OpenAIService
import cat.tarven.data.model.Character
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.Conversation
import cat.tarven.data.model.MessageRole
import cat.tarven.data.repository.ChatRepository
import cat.tarven.data.repository.LabRepository
import cat.tarven.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepo = ChatRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val charRepo = cat.tarven.data.repository.CharacterRepository(application)
    private val apiService = OpenAIService()
    private val labRepository = LabRepository(application)

    val messages = mutableStateListOf<ChatMessage>()

    var currentCharacter by mutableStateOf<Character?>(null)
        private set
    var currentConversation by mutableStateOf<Conversation?>(null)
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var streamingJob: Job? = null

    /**
     * 初始化聊天 — 加载角色和对话
     */
    fun initChat(character: Character) {
        currentCharacter = character
        val conversation = chatRepo.getOrCreateLatestConversation(character.id, character.name)
        currentConversation = conversation
        messages.clear()
        messages.addAll(conversation.messages)

        // 收集所有开场白（主开场白 + 备用开场白）
        val allGreetings = buildList {
            if (character.firstMessage.isNotBlank()) add(character.firstMessage)
            addAll(character.alternateGreetings)
        }.distinct() // 去重，防止某些卡片主备重复

        // 如果是全新对话且有开场白，自动添加
        if (messages.isEmpty() && allGreetings.isNotEmpty()) {
            val firstMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = substituteParams(allGreetings.first(), character),
                name = character.name,
                alternateGreetings = allGreetings,
                currentGreetingIndex = 0
            )
            messages.add(firstMsg)
            saveMessages()
        }
    }

    /**
     * 重新加载当前角色数据（在角色卡编辑后调用）
     */
    fun reloadCharacter() {
        currentCharacter?.id?.let { id ->
            charRepo.getCharacter(id)?.let { updatedChar ->
                currentCharacter = updatedChar
            }
        }
    }

    /**
     * 切换备用开场白
     */
    fun switchGreeting(messageId: String, newIndex: Int) {
        val character = currentCharacter ?: return
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = messages[index]
            if (msg.alternateGreetings.isNotEmpty() && newIndex in msg.alternateGreetings.indices) {
                messages[index] = msg.copy(
                    content = substituteParams(msg.alternateGreetings[newIndex], character),
                    currentGreetingIndex = newIndex
                )
                saveMessages()
            }
        }
    }

    /**
     * 发送用户消息并请求 AI 回复
     */
    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || isGenerating) return

        val character = currentCharacter ?: return

        if (!settingsRepo.isApiConfigured()) {
            errorMessage = "请先在设置中配置 API 地址、密钥和模型名称"
            return
        }

        // 添加用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = userInput,
            name = settingsRepo.userName
        )
        messages.add(userMessage)
        saveMessages()

        // 开始生成
        isGenerating = true
        errorMessage = null

        // 构建 API 消息列表
        val apiMessages = buildApiMessages(character)

        val request = ChatCompletionRequest(
            model = settingsRepo.modelName,
            messages = apiMessages,
            temperature = settingsRepo.temperature.toDouble(),
            maxTokens = settingsRepo.maxTokens,
            topP = settingsRepo.topP.toDouble(),
            frequencyPenalty = settingsRepo.frequencyPenalty.toDouble(),
            presencePenalty = settingsRepo.presencePenalty.toDouble(),
            stream = settingsRepo.streamEnabled
        )

        // 拦截并保存日志到实验室
        labRepository.saveLog(character.name, request)

        if (settingsRepo.streamEnabled) {
            streamResponse(request, character)
        } else {
            nonStreamResponse(request, character)
        }
    }

    /**
     * 将名字格式化为符合 OpenAI API 规范的字符串 (^[a-zA-Z0-9_-]{1,64}$)
     */
    private fun sanitizeApiName(name: String): String? {
        val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "")
        return if (sanitized.isNotBlank()) sanitized.take(64) else null
    }

    /**
     * 流式请求
     */
    private fun streamResponse(request: ChatCompletionRequest, character: Character) {
        // 创建一个占位的 assistant 消息，这里的 name 仅用于 UI 显示
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            name = character.name,
            isStreaming = true
        )
        messages.add(assistantMessage)
        val msgIndex = messages.lastIndex

        streamingJob = viewModelScope.launch {
            val contentBuilder = StringBuilder()

            // 复制一份 request，替换里面 messages 中的 name 字段，防止包含中文等非法字符导致 400 错误
            val safeRequest = request.copy(
                messages = request.messages.map { it.copy(name = it.name?.let { n -> sanitizeApiName(n) }) }
            )
            
            // 将发给大模型的最终 JSON 保存到本地实验室记录中
            labRepository.saveLog(character.name, safeRequest)

            apiService.streamChatCompletion(
                apiUrl = settingsRepo.apiUrl,
                apiKey = settingsRepo.apiKey,
                request = safeRequest
            )
            .catch { error ->
                // 流出错，更新消息或显示错误
                if (contentBuilder.isEmpty()) {
                    messages.removeAt(msgIndex)
                    errorMessage = "生成失败: ${error.message}"
                } else {
                    messages[msgIndex] = messages[msgIndex].copy(
                        content = contentBuilder.toString(),
                        isStreaming = false
                    )
                }
                isGenerating = false
                saveMessages()
            }
            .collect { token ->
                contentBuilder.append(token)
                messages[msgIndex] = messages[msgIndex].copy(
                    content = contentBuilder.toString()
                )
            }

            // 流正常结束
            if (msgIndex < messages.size) {
                messages[msgIndex] = messages[msgIndex].copy(
                    isStreaming = false
                )
            }
            isGenerating = false
            saveMessages()
        }
    }

    /**
     * 非流式请求
     */
    private fun nonStreamResponse(request: ChatCompletionRequest, character: Character) {
        viewModelScope.launch {
            val safeRequest = request.copy(
                messages = request.messages.map { it.copy(name = it.name?.let { n -> sanitizeApiName(n) }) }
            )
            labRepository.saveLog(character.name, safeRequest)

            val result = apiService.chatCompletion(
                apiUrl = settingsRepo.apiUrl,
                apiKey = settingsRepo.apiKey,
                request = safeRequest
            )

            result.fold(
                onSuccess = { content ->
                    val assistantMessage = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = content,
                        name = character.name
                    )
                    messages.add(assistantMessage)
                },
                onFailure = { error ->
                    errorMessage = "生成失败: ${error.message}"
                }
            )

            isGenerating = false
            saveMessages()
        }
    }

    /**
     * 构建 OpenAI 格式的消息数组
     * 新的七层排序逻辑:
     * ① 全局提示词 (system)
     * ② 角色系统提示词 (system)
     * ③ 玩家设定 (system)
     * ④ 主要设定前的世界书条目 (各自 role)
     * ⑤ 主要设定 (其 role)
     * ⑥ 主要设定后的世界书条目 (各自 role)
     * ⑦ 历史对话
     * ⑧ 写作规则 (其 role)
     */
    private fun buildApiMessages(character: Character): List<ApiMessage> {
        val apiMessages = mutableListOf<ApiMessage>()

        // --- 收集激活的世界书条目 ---
        val recentMessagesText = messages.takeLast(10).joinToString(" ") { it.content }
        val wiEntries = character.worldInfo?.entries ?: emptyList()
        val activatedEntries = wiEntries.filter { entry ->
            !entry.disable && (entry.constant || (entry.keys.isNotEmpty() && entry.keys.any { key ->
                recentMessagesText.contains(key, ignoreCase = true)
            }))
        }

        // 按标签分类
        val mainSettingEntry = activatedEntries.firstOrNull { it.tag == "main_setting" }
        val writingRulesEntry = activatedEntries.firstOrNull { it.tag == "writing_rules" }
        val normalEntries = activatedEntries.filter { it.tag == "normal" }
        val beforeMainEntries = normalEntries.filter { it.relativePosition == "before_main" }.sortedBy { it.insertionOrder }
        val afterMainEntries = normalEntries.filter { it.relativePosition == "after_main" }.sortedBy { it.insertionOrder }

        // ① 全局提示词
        val globalPrompt = substituteParams(settingsRepo.systemPrompt, character).trim()
        if (globalPrompt.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = globalPrompt))
        }

        // ② 角色系统提示词
        val charSystemPrompt = substituteParams(character.systemPrompt, character).trim()
        if (charSystemPrompt.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = charSystemPrompt))
        }

        // ③ 玩家设定（马甲）
        val userPersona = settingsRepo.userPersona.trim()
        if (userPersona.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = "[Player Character]\n$userPersona"))
        }

        // ④ 主要设定前的世界书条目
        beforeMainEntries.forEach { entry ->
            apiMessages.add(ApiMessage(role = entry.role, content = substituteParams(entry.content, character)))
        }

        // ⑤ 主要设定
        if (mainSettingEntry != null) {
            apiMessages.add(ApiMessage(role = mainSettingEntry.role, content = substituteParams(mainSettingEntry.content, character)))
        }

        // ⑥ 主要设定后的世界书条目
        afterMainEntries.forEach { entry ->
            apiMessages.add(ApiMessage(role = entry.role, content = substituteParams(entry.content, character)))
        }

        // ⑦ 历史对话
        val historyMessages = messages.filter { !it.isStreaming }.map {
            ApiMessage(role = it.role.value, content = it.content)
        }
        apiMessages.addAll(historyMessages)

        // ⑧ 写作规则
        if (writingRulesEntry != null) {
            apiMessages.add(ApiMessage(role = writingRulesEntry.role, content = substituteParams(writingRulesEntry.content, character)))
        }

        return apiMessages
    }

    /**
     * 保存世界书修改（从聊天页面调用）
     */
    fun saveWorldInfo(worldInfo: cat.tarven.data.model.WorldInfo) {
        currentCharacter?.let { char ->
            val updated = char.copy(worldInfo = worldInfo, updatedAt = System.currentTimeMillis())
            charRepo.saveCharacter(updated)
            currentCharacter = updated
        }
    }

    /**
     * 参数替换 — 借鉴 SillyTavern 的 substituteParams
     */
    private fun substituteParams(text: String, character: Character): String {
        return text
            .replace("{{char}}", character.name)
            .replace("{{user}}", settingsRepo.userName)
            .replace("{{charIfNotGroup}}", character.name)
            .replace("{{personality}}", character.personality)
            .replace("{{scenario}}", character.scenario)
            .replace("{{description}}", character.description)
    }

    /**
     * 停止生成
     */
    fun stopGenerating() {
        streamingJob?.cancel()
        streamingJob = null
        isGenerating = false

        // 如果最后一条消息是流式的，标记为完成
        if (messages.isNotEmpty() && messages.last().isStreaming) {
            val lastIndex = messages.lastIndex
            messages[lastIndex] = messages[lastIndex].copy(isStreaming = false)
            if (messages[lastIndex].content.isBlank()) {
                messages.removeAt(lastIndex)
            }
        }
        saveMessages()
    }

    /**
     * 重新生成最后一条 AI 回复
     */
    fun regenerateLastResponse() {
        if (isGenerating) return

        // 移除最后一条 assistant 消息
        while (messages.isNotEmpty() && messages.last().role == MessageRole.ASSISTANT) {
            messages.removeAt(messages.lastIndex)
        }
        saveMessages()

        // 如果有用户消息，取出最后一条重新发送
        if (messages.isNotEmpty() && messages.last().role == MessageRole.USER) {
            val lastUserMsg = messages.last().content
            messages.removeAt(messages.lastIndex)
            sendMessage(lastUserMsg)
        }
    }

    /**
     * 删除指定消息
     */
    fun deleteMessage(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages.removeAt(index)
            saveMessages()
        }
    }

    /**
     * 编辑消息
     */
    fun editMessage(messageId: String, newContent: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index] = messages[index].copy(content = newContent)
            saveMessages()
        }
    }

    /**
     * 新建对话
     */
    fun newConversation() {
        val character = currentCharacter ?: return
        val newConv = Conversation(
            characterId = character.id,
            title = "与 ${character.name} 的对话"
        )
        currentConversation = chatRepo.saveConversation(newConv)
        messages.clear()

        // 添加开场白
        if (character.firstMessage.isNotBlank()) {
            val firstMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = substituteParams(character.firstMessage, character),
                name = character.name
            )
            messages.add(firstMsg)
            saveMessages()
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        errorMessage = null
    }

    /**
     * 保存消息到本地
     */
    private fun saveMessages() {
        val conversation = currentConversation ?: return
        val updated = conversation.copy(
            messages = messages.toMutableList(),
            updatedAt = System.currentTimeMillis()
        )
        currentConversation = chatRepo.saveConversation(updated)
    }
}
