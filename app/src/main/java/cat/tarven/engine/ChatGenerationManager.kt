package cat.tarven.engine

import android.app.Application
import cat.tarven.data.api.ChatCompletionRequest
import cat.tarven.data.api.OpenAIService
import cat.tarven.data.model.Character
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.MessageRole
import cat.tarven.data.model.MessageSwipe
import cat.tarven.data.repository.ChatRepository
import cat.tarven.data.repository.LabRepository
import cat.tarven.data.repository.SettingsRepository
import cat.tarven.utils.ApiNameSanitizer
import cat.tarven.utils.ThinkBlockParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 生成状态密封类
 */
sealed class GenerationState {
    /** 空闲 */
    data object Idle : GenerationState()

    /** 正在生成 */
    data class Generating(
        val conversationId: String,
        val characterId: String,
        val messages: List<ChatMessage>,
        val currentContent: String = "",
        val targetMessageIndex: Int = -1,
        val isNewMessage: Boolean = true
    ) : GenerationState()

    /** 生成完成 */
    data class Completed(
        val conversationId: String,
        val messages: List<ChatMessage>
    ) : GenerationState()

    /** 生成出错 */
    data class Error(
        val conversationId: String,
        val messages: List<ChatMessage>,
        val error: String
    ) : GenerationState()
}

/**
 * 全局多通道 AI 生成管理器 (Plan 3.0)
 *
 * 支持并发管理多个 Conversation 的生成请求，互不干扰。
 */
@Singleton
class ChatGenerationManager @Inject constructor(
    private val apiService: OpenAIService,
    private val chatRepo: ChatRepository,
    private val settingsRepo: SettingsRepository,
    private val labRepository: LabRepository,
    private val application: Application
) {
    /** 独立于 ViewModel 的协程作用域 — 生命周期跟随进程 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 管理所有正在进行的生成任务，Key 是 ConversationId */
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /** 
     * 多通道生成状态流 — UI 层通过 collect 获取特定对话的实时更新 
     * Key 是 ConversationId
     */
    private val _generationStates = MutableStateFlow<Map<String, GenerationState>>(emptyMap())
    val generationStates: StateFlow<Map<String, GenerationState>> = _generationStates.asStateFlow()

    /** 检查指定的对话是否正在生成 */
    fun isGenerating(conversationId: String?): Boolean {
        if (conversationId == null) return false
        val state = _generationStates.value[conversationId]
        return state is GenerationState.Generating
    }

    /** 更新指定对话的状态，触发 Flow 发射新 Map */
    private fun updateState(conversationId: String, state: GenerationState?) {
        _generationStates.update { currentMap ->
            if (state == null) {
                currentMap - conversationId
            } else {
                currentMap + (conversationId to state)
            }
        }
    }

    /** 安全启动保活服务 */
    private fun startForegroundService() {
        cat.tarven.service.GenerationService.start(application)
    }

    /** 如果没有任务了，停止保活服务 */
    private fun stopForegroundServiceIfEmpty() {
        if (activeJobs.isEmpty()) {
            cat.tarven.service.GenerationService.stop(application)
        }
    }

    /**
     * 启动流式生成
     */
    fun startStreamGeneration(
        conversationId: String,
        characterId: String,
        character: Character,
        currentMessages: List<ChatMessage>,
        request: ChatCompletionRequest,
        targetMessageIndex: Int? = null
    ) {
        // 先停止该对话可能正在跑的旧任务
        stopGeneration(conversationId)

        // 准备消息列表的可变副本
        val messagesCopy = currentMessages.toMutableList()
        val msgIndex: Int
        val isNewMessage: Boolean

        if (targetMessageIndex == null) {
            val assistantMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                name = character.name,
                isStreaming = true,
                swipes = listOf(MessageSwipe(content = "")),
                currentSwipeIndex = 0
            )
            messagesCopy.add(assistantMessage)
            msgIndex = messagesCopy.lastIndex
            isNewMessage = true
        } else {
            msgIndex = targetMessageIndex
            val msg = messagesCopy[msgIndex]
            val newSwipes = msg.swipes.toMutableList()
            newSwipes.add(MessageSwipe(content = ""))
            messagesCopy[msgIndex] = msg.copy(
                isStreaming = true,
                swipes = newSwipes,
                currentSwipeIndex = newSwipes.lastIndex,
                content = ""
            )
            isNewMessage = false
        }

        // 初始 Generating 状态
        updateState(conversationId, GenerationState.Generating(
            conversationId = conversationId,
            characterId = characterId,
            messages = messagesCopy.toList(),
            currentContent = "",
            targetMessageIndex = msgIndex,
            isNewMessage = isNewMessage
        ))

        val job = scope.launch {
            val contentBuilder = StringBuilder()

            try {
                // 清理 name 字段
                val safeRequest = request.copy(
                    messages = request.messages.map { it.copy(name = it.name?.let { n -> ApiNameSanitizer.sanitizeName(n) }) }
                )

                // 保存请求日志到实验室
                val logRequest = ApiNameSanitizer.sanitizeRequestForLog(safeRequest)
                labRepository.saveLog(character.name, logRequest)

                apiService.streamChatCompletion(
                    apiUrl = settingsRepo.apiUrl,
                    apiKey = settingsRepo.apiKey,
                    request = safeRequest
                )
                .catch { error ->
                    // 流出错处理
                    if (contentBuilder.isEmpty()) {
                        if (isNewMessage) {
                            if (msgIndex < messagesCopy.size) {
                                messagesCopy.removeAt(msgIndex)
                            }
                        } else {
                            if (msgIndex < messagesCopy.size) {
                                val msg = messagesCopy[msgIndex]
                                val rollbackSwipes = msg.swipes.toMutableList()
                                if (rollbackSwipes.isNotEmpty()) {
                                    rollbackSwipes.removeAt(rollbackSwipes.lastIndex)
                                }
                                val prevIdx = rollbackSwipes.lastIndex.coerceAtLeast(0)
                                messagesCopy[msgIndex] = msg.copy(
                                    isStreaming = false,
                                    swipes = rollbackSwipes,
                                    currentSwipeIndex = prevIdx,
                                    content = if (rollbackSwipes.isNotEmpty()) rollbackSwipes[prevIdx].content else msg.content
                                )
                            }
                        }
                        updateState(conversationId, GenerationState.Error(
                            conversationId = conversationId,
                            messages = messagesCopy.toList(),
                            error = "生成失败: ${error.message}"
                        ))
                    } else {
                        if (msgIndex < messagesCopy.size) {
                            messagesCopy[msgIndex] = messagesCopy[msgIndex].copy(
                                content = contentBuilder.toString(),
                                isStreaming = false
                            )
                        }
                        updateState(conversationId, GenerationState.Error(
                            conversationId = conversationId,
                            messages = messagesCopy.toList(),
                            error = "生成中断: ${error.message}"
                        ))
                    }
                    saveConversation(conversationId, characterId, messagesCopy)
                }
                .collect { token ->
                    contentBuilder.append(token)
                    if (msgIndex < messagesCopy.size) {
                        val snapshotMsg = messagesCopy[msgIndex]
                        val swipesList = snapshotMsg.swipes.toMutableList()
                        val activeSwipeIdx = snapshotMsg.currentSwipeIndex
                        if (activeSwipeIdx in swipesList.indices) {
                            swipesList[activeSwipeIdx] = swipesList[activeSwipeIdx].copy(content = contentBuilder.toString())
                        }
                        messagesCopy[msgIndex] = snapshotMsg.copy(
                            content = contentBuilder.toString(),
                            swipes = swipesList
                        )
                    }
                    updateState(conversationId, GenerationState.Generating(
                        conversationId = conversationId,
                        characterId = characterId,
                        messages = messagesCopy.toList(),
                        currentContent = contentBuilder.toString(),
                        targetMessageIndex = msgIndex,
                        isNewMessage = isNewMessage
                    ))
                }

                // 流正常结束：分离推理内容
                if (msgIndex < messagesCopy.size) {
                    val fullContent = contentBuilder.toString()
                    val (reasoning, cleanContent) = ThinkBlockParser.extractThinkBlock(fullContent)

                    val finalMsg = messagesCopy[msgIndex]
                    val updatedSwipes = finalMsg.swipes.toMutableList()
                    val activeIdx = finalMsg.currentSwipeIndex
                    if (activeIdx in updatedSwipes.indices) {
                        updatedSwipes[activeIdx] = updatedSwipes[activeIdx].copy(
                            content = cleanContent,
                            reasoningContent = reasoning
                        )
                    }
                    messagesCopy[msgIndex] = finalMsg.copy(
                        isStreaming = false,
                        content = cleanContent,
                        swipes = updatedSwipes
                    )
                }

                saveConversation(conversationId, characterId, messagesCopy)
                updateState(conversationId, GenerationState.Completed(
                    conversationId = conversationId,
                    messages = messagesCopy.toList()
                ))
            } finally {
                activeJobs.remove(conversationId)
                stopForegroundServiceIfEmpty()
            }
        }
        
        activeJobs[conversationId] = job
        startForegroundService()
    }

    /**
     * 启动非流式生成
     */
    fun startNonStreamGeneration(
        conversationId: String,
        characterId: String,
        character: Character,
        currentMessages: List<ChatMessage>,
        request: ChatCompletionRequest,
        targetMessageIndex: Int? = null
    ) {
        stopGeneration(conversationId)

        val messagesCopy = currentMessages.toMutableList()

        updateState(conversationId, GenerationState.Generating(
            conversationId = conversationId,
            characterId = characterId,
            messages = messagesCopy.toList(),
            currentContent = "",
            targetMessageIndex = targetMessageIndex ?: -1,
            isNewMessage = targetMessageIndex == null
        ))

        val job = scope.launch {
            try {
                val safeRequest = request.copy(
                    messages = request.messages.map { it.copy(name = it.name?.let { n -> ApiNameSanitizer.sanitizeName(n) }) }
                )

                val logRequest = ApiNameSanitizer.sanitizeRequestForLog(safeRequest)
                labRepository.saveLog(character.name, logRequest)

                val result = apiService.chatCompletion(
                    apiUrl = settingsRepo.apiUrl,
                    apiKey = settingsRepo.apiKey,
                    request = safeRequest
                )

                result.fold(
                    onSuccess = { rawContents ->
                        val parsedSwipes = rawContents.map { raw ->
                            val (reasoning, clean) = ThinkBlockParser.extractThinkBlock(raw)
                            MessageSwipe(content = clean, reasoningContent = reasoning)
                        }
                        if (targetMessageIndex == null) {
                            val assistantMessage = ChatMessage(
                                role = MessageRole.ASSISTANT,
                                content = parsedSwipes.first().content,
                                name = character.name,
                                swipes = parsedSwipes,
                                currentSwipeIndex = 0
                            )
                            messagesCopy.add(assistantMessage)
                        } else {
                            val msg = messagesCopy[targetMessageIndex]
                            val newSwipes = msg.swipes.toMutableList()
                            newSwipes.addAll(parsedSwipes)
                            messagesCopy[targetMessageIndex] = msg.copy(
                                swipes = newSwipes,
                                currentSwipeIndex = newSwipes.lastIndex,
                                content = parsedSwipes.last().content
                            )
                        }

                        saveConversation(conversationId, characterId, messagesCopy)
                        updateState(conversationId, GenerationState.Completed(
                            conversationId = conversationId,
                            messages = messagesCopy.toList()
                        ))
                    },
                    onFailure = { error ->
                        updateState(conversationId, GenerationState.Error(
                            conversationId = conversationId,
                            messages = messagesCopy.toList(),
                            error = "生成失败: ${error.message}"
                        ))
                    }
                )
            } finally {
                activeJobs.remove(conversationId)
                stopForegroundServiceIfEmpty()
            }
        }
        
        activeJobs[conversationId] = job
        startForegroundService()
    }

    /**
     * 停止指定对话的生成
     */
    fun stopGeneration(conversationId: String) {
        val job = activeJobs.remove(conversationId)
        job?.cancel()

        val currentState = _generationStates.value[conversationId]
        if (currentState is GenerationState.Generating) {
            // 保存已生成的内容
            val messages = currentState.messages.toMutableList()
            val msgIndex = currentState.targetMessageIndex
            if (msgIndex in messages.indices && messages[msgIndex].isStreaming) {
                val msg = messages[msgIndex]
                if (msg.content.isBlank() && currentState.isNewMessage) {
                    messages.removeAt(msgIndex)
                } else {
                    messages[msgIndex] = msg.copy(isStreaming = false)
                }
            }
            saveConversation(currentState.conversationId, currentState.characterId, messages)
            updateState(conversationId, GenerationState.Completed(
                conversationId = currentState.conversationId,
                messages = messages.toList()
            ))
        }

        stopForegroundServiceIfEmpty()
    }

    /**
     * 停止所有对话的生成
     */
    fun stopAllGenerations() {
        activeJobs.keys.toList().forEach { convId ->
            stopGeneration(convId)
        }
    }

    /**
     * 重置状态为 Idle 并从 Map 中移除
     */
    fun acknowledgeCompletion(conversationId: String) {
        updateState(conversationId, null)
    }

    /**
     * 持久化对话到磁盘
     */
    private fun saveConversation(conversationId: String, characterId: String, messages: List<ChatMessage>) {
        try {
            val existing = chatRepo.getConversation(characterId, conversationId)
            val conversation = (existing ?: cat.tarven.data.model.Conversation(
                id = conversationId,
                characterId = characterId
            )).copy(
                messages = messages,
                updatedAt = System.currentTimeMillis()
            )
            chatRepo.saveConversation(conversation)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "ChatGenerationManager: 保存对话失败")
        }
    }
}
