package cat.tarven.data.model

import java.util.UUID

data class MessageSwipe(
    val content: String,
    val reasoningContent: String? = null
)

/**
 * 聊天消息数据模型
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val name: String? = null,
    val alternateGreetings: List<String> = emptyList(), // 保留兼容旧版
    val currentGreetingIndex: Int = 0,
    val swipes: List<MessageSwipe> = emptyList(),
    val currentSwipeIndex: Int = 0,
    val propName: String? = null
) {
    // 动态获取当前展示的回复内容
    val displayContent: String
        get() = if (swipes.isNotEmpty() && currentSwipeIndex in swipes.indices) swipes[currentSwipeIndex].content else content

    val displayReasoning: String?
        get() = if (swipes.isNotEmpty() && currentSwipeIndex in swipes.indices) swipes[currentSwipeIndex].reasoningContent else null
}

enum class MessageRole(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    companion object {
        fun fromString(value: String): MessageRole {
            return entries.find { it.value == value } ?: USER
        }
    }
}
