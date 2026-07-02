package cat.tarven.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.api.ChatCompletionRequest
import cat.tarven.data.api.ContextBuilder
import cat.tarven.data.model.Attachment
import cat.tarven.data.model.Character
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.Conversation
import cat.tarven.data.model.MessageRole
import cat.tarven.data.repository.ChatRepository
import cat.tarven.data.repository.SettingsRepository
import cat.tarven.engine.ChatGenerationManager
import cat.tarven.engine.GenerationState
import cat.tarven.utils.MacroSubstitutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val settingsRepo: SettingsRepository,
    private val charRepo: cat.tarven.data.repository.CharacterRepository,
    private val generationManager: ChatGenerationManager,
    private val application: Application
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()

    var currentCharacter by mutableStateOf<Character?>(null)
        private set
    var currentConversation by mutableStateOf<Conversation?>(null)
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var editVersion by mutableStateOf(0)
        private set
    var chatInputText by mutableStateOf("")
    val pendingAttachments = mutableStateListOf<Attachment>()

    init {
        // 观察全局多通道生成引擎的状态流，同步更新本地 UI 状态
        viewModelScope.launch {
            generationManager.generationStates.collect { statesMap ->
                val currentId = currentConversation?.id ?: return@collect
                val state = statesMap[currentId]

                when (state) {
                    null, is GenerationState.Idle -> {
                        isGenerating = false
                    }
                    is GenerationState.Generating -> {
                        isGenerating = true
                        messages.clear()
                        messages.addAll(state.messages)
                    }
                    is GenerationState.Completed -> {
                        isGenerating = false
                        messages.clear()
                        messages.addAll(state.messages)
                        // 同步更新 currentConversation 的 messages 快照
                        currentConversation = currentConversation?.copy(
                            messages = state.messages,
                            updatedAt = System.currentTimeMillis()
                        )
                        // 消费完成状态，重置为 Idle
                        generationManager.acknowledgeCompletion(currentId)
                    }
                    is GenerationState.Error -> {
                        isGenerating = false
                        errorMessage = state.error
                        messages.clear()
                        messages.addAll(state.messages)
                        currentConversation = currentConversation?.copy(
                            messages = state.messages,
                            updatedAt = System.currentTimeMillis()
                        )
                        generationManager.acknowledgeCompletion(currentId)
                    }
                }
            }
        }
    }

    /**
     * 初始化聊天 — 加载角色和对话
     */
    fun initChat(convWithChar: cat.tarven.data.model.ConversationWithCharacter) {
        if (currentConversation?.id == convWithChar.conversation.id) {
            currentCharacter = convWithChar.character
            currentConversation = convWithChar.conversation
            return
        }

        // 不再调用 stopGenerating()！如果后台正在生成其他对话，让它继续
        currentCharacter = convWithChar.character
        currentConversation = convWithChar.conversation

        // 如果生成引擎正在为这个对话生成，从引擎获取最新状态
        val genState = generationManager.generationStates.value[convWithChar.conversation.id]
        if (genState is GenerationState.Generating) {
            messages.clear()
            messages.addAll(genState.messages)
            isGenerating = true
            return
        }

        // 否则从传入的对话加载消息
        messages.clear()
        messages.addAll(convWithChar.conversation.messages)

        // 收集所有开场白（主开场白 + 备用开场白）
        val allGreetings = buildList {
            if (convWithChar.character.firstMessage.isNotBlank()) add(convWithChar.character.firstMessage)
            addAll(convWithChar.character.alternateGreetings)
        }.distinct() // 去重，防止某些卡片主备重复

        // 如果是全新对话且有开场白，自动添加
        if (messages.isEmpty() && allGreetings.isNotEmpty()) {
            val firstMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = MacroSubstitutor.substituteParams(allGreetings.first(), convWithChar.character, settingsRepo.userName),
                name = convWithChar.character.name,
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
     * 切换 Swipe（翻页）
     */
    fun switchSwipe(messageId: String, newIndex: Int) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = messages[index]
            if (msg.swipes.isNotEmpty() && newIndex in msg.swipes.indices) {
                // 更新当前索引，并同步 content（兼容旧代码）
                messages[index] = msg.copy(
                    currentSwipeIndex = newIndex,
                    content = msg.swipes[newIndex].content
                )
                saveMessages()
            } else if (msg.alternateGreetings.isNotEmpty() && newIndex in msg.alternateGreetings.indices) {
                // 兼容旧版主开场白切换
                val character = currentCharacter ?: return
                messages[index] = msg.copy(
                    content = MacroSubstitutor.substituteParams(msg.alternateGreetings[newIndex], character, settingsRepo.userName),
                    currentGreetingIndex = newIndex
                )
                saveMessages()
            }
        }
    }

    /**
     * 发送用户消息并请求 AI 回复
     */
    fun sendMessage(content: String, propName: String? = null, attachments: List<Attachment> = emptyList()) {
        val character = currentCharacter ?: return
        val conversation = currentConversation ?: return
        val hasAttachments = attachments.isNotEmpty()
        if (content.isBlank() && !hasAttachments || generationManager.isGenerating(conversation.id)) return

        if (!settingsRepo.isApiConfigured()) {
            errorMessage = "请先在设置中配置 API 地址、密钥和模型名称"
            return
        }

        if (!isNetworkAvailable()) {
            errorMessage = "无网络连接，请检查网络设置"
            return
        }

        // 添加用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content,
            name = settingsRepo.userName.takeIf { it.isNotBlank() } ?: "User",
            propName = propName,
            attachments = attachments
        )
        messages.add(userMessage)
        saveMessages()

        // 构建 API 消息列表
        errorMessage = null
        val settings = ContextBuilder.Settings(
            systemPrompt = settingsRepo.systemPrompt,
            userName = settingsRepo.userName,
            userPersona = settingsRepo.userPersona,
            statusBarStripRegex = settingsRepo.statusBarStripRegex
        )
        val apiMessages = ContextBuilder.build(character, messages, settings)

        val request = ChatCompletionRequest(
            model = settingsRepo.modelName,
            messages = apiMessages,
            temperature = settingsRepo.temperature.toDouble(),
            maxTokens = settingsRepo.maxTokens,
            topP = settingsRepo.topP.toDouble(),
            frequencyPenalty = settingsRepo.frequencyPenalty.toDouble(),
            presencePenalty = settingsRepo.presencePenalty.toDouble(),
            stream = settingsRepo.streamEnabled,
            n = settingsRepo.autoSwipeCount
        )

        // 委托给全局生成引擎
        if (settingsRepo.streamEnabled) {
            generationManager.startStreamGeneration(
                conversationId = conversation.id,
                characterId = conversation.characterId,
                character = character,
                currentMessages = messages.toList(),
                request = request
            )
        } else {
            generationManager.startNonStreamGeneration(
                conversationId = conversation.id,
                characterId = conversation.characterId,
                character = character,
                currentMessages = messages.toList(),
                request = request
            )
        }
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
     * 停止生成
     */
    fun stopGenerating() {
        val conversation = currentConversation ?: return
        generationManager.stopGeneration(conversation.id)
        // isGenerating 会通过 StateFlow collect 自动更新

        // 同步本地 messages 状态（防止 collect 延迟导致 UI 闪烁）
        if (messages.isNotEmpty() && messages.last().isStreaming) {
            val lastIndex = messages.lastIndex
            messages[lastIndex] = messages[lastIndex].copy(isStreaming = false)
            if (messages[lastIndex].content.isBlank()) {
                messages.removeAt(lastIndex)
            }
        }
        isGenerating = false
        saveMessages()
    }

    /**
     * 重新生成最后一条 AI 回复
     */
    fun regenerateLastResponse() {
        val conversation = currentConversation ?: return
        if (generationManager.isGenerating(conversation.id)) return

        if (messages.isNotEmpty() && messages.last().role == MessageRole.ASSISTANT) {
            regenerateMessage(messages.last().id)
        }
    }

    /**
     * 重新生成指定消息
     */
    fun regenerateMessage(messageId: String) {
        val conversation = currentConversation ?: return
        if (generationManager.isGenerating(conversation.id)) return

        if (!isNetworkAvailable()) {
            errorMessage = "无网络连接，请检查网络设置"
            return
        }

        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0) return

        if (messages[index].role == MessageRole.ASSISTANT) {
            // 删除这条消息之后的所有消息，不删除本消息
            val messagesToRemove = messages.lastIndex - index
            for (i in 0 until messagesToRemove) {
                messages.removeAt(messages.lastIndex)
            }
            saveMessages()

            // 重新触发生成
            val lastUserIndex = messages.indexOfLast { it.role == MessageRole.USER }
            if (lastUserIndex >= 0) {
                errorMessage = null
                val character = currentCharacter ?: return
                val settings = ContextBuilder.Settings(
                    systemPrompt = settingsRepo.systemPrompt,
                    userName = settingsRepo.userName,
                    userPersona = settingsRepo.userPersona,
                    statusBarStripRegex = settingsRepo.statusBarStripRegex
                )
                val apiMessages = ContextBuilder.build(character, messages, settings, lastUserIndex)
                val request = ChatCompletionRequest(
                    model = settingsRepo.modelName,
                    messages = apiMessages,
                    temperature = settingsRepo.temperature.toDouble(),
                    maxTokens = settingsRepo.maxTokens,
                    topP = settingsRepo.topP.toDouble(),
                    frequencyPenalty = settingsRepo.frequencyPenalty.toDouble(),
                    presencePenalty = settingsRepo.presencePenalty.toDouble(),
                    stream = settingsRepo.streamEnabled,
                    n = settingsRepo.autoSwipeCount
                )

                // 委托给全局生成引擎，追加 Swipe
                if (settingsRepo.streamEnabled) {
                    generationManager.startStreamGeneration(
                        conversationId = conversation.id,
                        characterId = conversation.characterId,
                        character = character,
                        currentMessages = messages.toList(),
                        request = request,
                        targetMessageIndex = index
                    )
                } else {
                    generationManager.startNonStreamGeneration(
                        conversationId = conversation.id,
                        characterId = conversation.characterId,
                        character = character,
                        currentMessages = messages.toList(),
                        request = request,
                        targetMessageIndex = index
                    )
                }
            }
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
            val msg = messages[index]
            val updatedSwipes = msg.swipes.toMutableList()
            val activeIdx = msg.currentSwipeIndex
            if (activeIdx in updatedSwipes.indices) {
                updatedSwipes[activeIdx] = updatedSwipes[activeIdx].copy(content = newContent)
            }
            messages[index] = msg.copy(content = newContent, swipes = updatedSwipes)
            saveMessages()
            editVersion++
        }
    }

    /**
     * 一键转生：开一个新档，并用指定的文本作为用户的第一句话发送
     */
    fun reincarnate(content: String) {
        val character = currentCharacter ?: return

        // 1. 创建新对话
        val newConv = chatRepo.saveConversation(
            Conversation(
                characterId = character.id,
                title = "与 ${character.name} 的转生对话"
            )
        )
        currentConversation = newConv
        messages.clear()

        // 2. 添加开场白
        val allGreetings = buildList {
            if (character.firstMessage.isNotBlank()) add(character.firstMessage)
            addAll(character.alternateGreetings)
        }.distinct()

        if (allGreetings.isNotEmpty()) {
            val firstMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = MacroSubstitutor.substituteParams(allGreetings.first(), character, settingsRepo.userName),
                name = character.name,
                alternateGreetings = allGreetings,
                currentGreetingIndex = 0
            )
            messages.add(firstMsg)
            saveMessages()
        }

        // 3. 作为用户发送总结内容并触发 AI 生成
        sendMessage(content)
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

        // 收集所有开场白（主开场白 + 备用开场白），与 initChat 保持一致
        val allGreetings = buildList {
            if (character.firstMessage.isNotBlank()) add(character.firstMessage)
            addAll(character.alternateGreetings)
        }.distinct()

        if (allGreetings.isNotEmpty()) {
            val firstMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = MacroSubstitutor.substituteParams(allGreetings.first(), character, settingsRepo.userName),
                name = character.name,
                alternateGreetings = allGreetings,
                currentGreetingIndex = 0
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
     * 检查网络连通性
     */
    private fun isNetworkAvailable(): Boolean {
        val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetwork != null
    }

    /**
     * 保存消息到本地
     */
    private fun saveMessages() {
        val conversation = currentConversation ?: return
        val updated = conversation.copy(
            messages = messages.toList(),
            updatedAt = System.currentTimeMillis()
        )
        currentConversation = updated
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.saveConversation(updated)
        }
    }
}
