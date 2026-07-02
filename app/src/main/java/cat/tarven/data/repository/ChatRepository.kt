package cat.tarven.data.repository

import android.content.Context
import cat.tarven.data.model.Conversation
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天数据仓库 — 管理对话和消息的持久化
 */
@Singleton
class ChatRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val gson = Gson()
    private val conversationCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Conversation>>()
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
        conversationCache[file.absolutePath] = Pair(file.lastModified(), updated)
        return updated
    }

    /**
     * 删除对话
     */
    fun deleteConversation(characterId: String, conversationId: String) {
        val file = File(File(chatsDir, characterId), "$conversationId.json")
        conversationCache.remove(file.absolutePath)
        file.delete()
    }

    /**
     * 删除某角色的所有对话
     */
    fun deleteAllConversations(characterId: String) {
        val dir = File(chatsDir, characterId)
        dir.listFiles()?.forEach { conversationCache.remove(it.absolutePath) }
        dir.deleteRecursively()
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
                    val path = file.absolutePath
                    val lastModified = file.lastModified()
                    val cached = conversationCache[path]
                    if (cached != null && cached.first == lastModified) {
                        allConversations.add(cached.second)
                    } else {
                        val conv = gson.fromJson(file.readText(), Conversation::class.java)
                        conversationCache[path] = Pair(lastModified, conv)
                        allConversations.add(conv)
                    }
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to parse conversation file: ${file.name}")
                }
            }
        }
        
        return allConversations.sortedByDescending { it.updatedAt }
    }
}
