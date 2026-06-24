package cat.tarven.data.model

import java.util.UUID

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
    val alternateGreetings: List<String> = emptyList(),
    val currentGreetingIndex: Int = 0
)

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
