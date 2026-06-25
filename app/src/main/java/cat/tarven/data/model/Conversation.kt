package cat.tarven.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 对话数据模型
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("character_id")
    val characterId: String = "",
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 带有角色信息的对话模型（用于主页列表展示）
 */
data class ConversationWithCharacter(
    val conversation: Conversation,
    val character: Character
)
