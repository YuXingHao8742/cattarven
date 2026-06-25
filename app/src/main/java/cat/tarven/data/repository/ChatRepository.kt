package cat.tarven.data.repository

import android.content.Context
import cat.tarven.data.model.Conversation
import com.google.gson.Gson
import java.io.File

/**
 * 聊天数据仓库 — 管理对话和消息的持久化
 */
class ChatRepository(private val context: Context) {

    private val gson = Gson()
    private val chatsDir: File
        get() = File(context.filesDir, "chats").also { it.mkdirs() }

    /**
     * 获取某角色的所有对话
     */
    fun getConversations(characterId: String): List<Conversation> {
        val dir = File(chatsDir, characterId)
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), Conversation::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    /**
     * 获取单个对话
     */
    fun getConversation(characterId: String, conversationId: String): Conversation? {
        val file = File(File(chatsDir, characterId), "$conversationId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), Conversation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存对话
     */
    fun saveConversation(conversation: Conversation): Conversation {
        val updated = conversation.copy(updatedAt = System.currentTimeMillis())
        val dir = File(chatsDir, updated.characterId)
        dir.mkdirs()
        val file = File(dir, "${updated.id}.json")
        file.writeText(gson.toJson(updated))
        return updated
    }

    /**
     * 删除对话
     */
    fun deleteConversation(characterId: String, conversationId: String) {
        File(File(chatsDir, characterId), "$conversationId.json").delete()
    }

    /**
     * 删除某角色的所有对话
     */
    fun deleteAllConversations(characterId: String) {
        File(chatsDir, characterId).deleteRecursively()
    }

    /**
     * 获取或创建某角色的最新对话
     */
    fun getOrCreateLatestConversation(characterId: String, characterName: String): Conversation {
        val conversations = getConversations(characterId)
        if (conversations.isNotEmpty()) {
            return conversations.first()
        }
        // 创建新对话
        val newConversation = Conversation(
            characterId = characterId,
            title = "与 $characterName 的对话"
        )
        return saveConversation(newConversation)
    }

    /**
     * 获取所有的对话 (用于主页展示)
     */
    fun getAllConversations(): List<Conversation> {
        val allConversations = mutableListOf<Conversation>()
        val dirs = chatsDir.listFiles { file -> file.isDirectory } ?: return emptyList()
        
        for (dir in dirs) {
            val files = dir.listFiles { file -> file.extension == "json" } ?: continue
            for (file in files) {
                try {
                    val conv = gson.fromJson(file.readText(), Conversation::class.java)
                    allConversations.add(conv)
                } catch (e: Exception) {
                    // Ignore corrupted files
                }
            }
        }
        
        return allConversations.sortedByDescending { it.updatedAt }
    }
}
