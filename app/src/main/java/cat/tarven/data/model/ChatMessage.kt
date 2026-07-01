package cat.tarven.data.model

import java.util.UUID

/**
 * 消息附件（图片或文本文件）
 */
@androidx.annotation.Keep
data class Attachment(
    val type: String,        // "image" 或 "text"
    val fileName: String,    // 原始文件名
    val filePath: String,    // 本地缓存路径
    val mimeType: String     // MIME 类型
)

@androidx.annotation.Keep
data class MessageSwipe(
    val content: String,
    val reasoningContent: String? = null
)

/**
 * 聊天消息数据模型
 */
@androidx.annotation.Keep
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
    val propName: String? = null,
    val attachments: List<Attachment> = emptyList()
) {
    // 动态获取当前展示的回复内容
    val displayContent: String
        get() = if (swipes.isNotEmpty() && currentSwipeIndex in swipes.indices) swipes[currentSwipeIndex].content else content

    val displayReasoning: String?
        get() = if (swipes.isNotEmpty() && currentSwipeIndex in swipes.indices) swipes[currentSwipeIndex].reasoningContent else null
}

@androidx.annotation.Keep
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

