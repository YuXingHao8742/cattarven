package cat.tarven.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cat.tarven.data.model.Character
import cat.tarven.data.repository.CharacterRepository
import cat.tarven.data.repository.ChatRepository

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val characterRepo = CharacterRepository(application)
    private val chatRepo = ChatRepository(application)

    var conversations by mutableStateOf<List<cat.tarven.data.model.ConversationWithCharacter>>(emptyList())
        private set
    var selectedConversation by mutableStateOf<cat.tarven.data.model.Conversation?>(null)
        private set
    var selectedCharacter by mutableStateOf<Character?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set

    // 编辑状态
    var editingCharacter by mutableStateOf<Character?>(null)
        private set

    init {
        loadConversations()
    }

    fun loadConversations() {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val allCharacters = characterRepo.getAllCharacters().associateBy { it.id }
            val allConversations = chatRepo.getAllConversations()
            
            val result = mutableListOf<cat.tarven.data.model.ConversationWithCharacter>()
            val processedCharacterIds = mutableSetOf<String>()
            
            // 添加所有实际的对话
            for (conv in allConversations) {
                val char = allCharacters[conv.characterId]
                if (char != null) {
                    result.add(cat.tarven.data.model.ConversationWithCharacter(conv, char))
                    processedCharacterIds.add(char.id)
                }
            }
            
            // 为没有任何对话的角色添加一个空对话作为入口
            for (char in allCharacters.values) {
                if (!processedCharacterIds.contains(char.id)) {
                    val emptyConv = cat.tarven.data.model.Conversation(
                        characterId = char.id,
                        title = "与 ${char.name} 的对话",
                        updatedAt = char.updatedAt
                    )
                    result.add(cat.tarven.data.model.ConversationWithCharacter(emptyConv, char))
                }
            }
            
            // 最终按时间倒序排列
            result.sortByDescending { it.conversation.updatedAt }
            
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                conversations = result
                isLoading = false
            }
        }
    }

    fun selectConversation(convWithChar: cat.tarven.data.model.ConversationWithCharacter) {
        selectedConversation = convWithChar.conversation
        selectedCharacter = convWithChar.character
    }

    fun saveCharacter(character: Character) {
        characterRepo.saveCharacter(character)
        loadConversations()
        errorMessage = null
    }

    fun deleteCharacter(id: String) {
        characterRepo.deleteCharacter(id)
        chatRepo.deleteAllConversations(id)
        if (selectedCharacter?.id == id) {
            selectedCharacter = null
            selectedConversation = null
        }
        loadConversations()
    }

    fun deleteConversation(characterId: String, conversationId: String) {
        chatRepo.deleteConversation(characterId, conversationId)
        if (selectedConversation?.id == conversationId) {
            selectedConversation = null
            selectedCharacter = null
        }
        loadConversations()
    }

    fun importCharacterFromJson(jsonString: String) {
        val result = characterRepo.importCharacter(jsonString)
        result.fold(
            onSuccess = {
                loadConversations()
                errorMessage = null
            },
            onFailure = { error ->
                errorMessage = "导入失败: ${error.message}"
            }
        )
    }

    fun importCharacterFromUri(uri: Uri) {
        try {
            val context = getApplication<Application>()
            val type = context.contentResolver.getType(uri)
            val isPng = type?.startsWith("image/png") == true || uri.toString().endsWith(".png", ignoreCase = true)

            val jsonString = if (isPng) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.use { cat.tarven.utils.ImageParser.extractCharacterJsonFromPng(it) }
                if (json.isNullOrBlank()) {
                    errorMessage = "未能从图片中提取到隐藏的角色数据"
                    return
                }
                json
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { it.bufferedReader().readText() } ?: ""
            }

            val result = characterRepo.importCharacter(jsonString)
            result.fold(
                onSuccess = { chara ->
                    if (isPng) {
                        val avatarPath = saveAvatarImage(uri)
                        if (avatarPath != null) {
                            characterRepo.saveCharacter(chara.copy(avatarUri = avatarPath))
                        }
                    }
                    loadConversations()
                    errorMessage = null
                },
                onFailure = { error ->
                    errorMessage = "导入失败: ${error.message}"
                }
            )
        } catch (e: Exception) {
            errorMessage = "读取文件失败: ${e.message}"
        }
    }

    fun saveAvatarImage(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val avatarDir = java.io.File(context.filesDir, "avatars").apply { mkdirs() }
            val fileName = "avatar_${System.currentTimeMillis()}.png"
            val file = java.io.File(avatarDir, fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            errorMessage = "保存头像失败: ${e.message}"
            null
        }
    }

    fun importWorldInfoFromUri(uri: Uri): cat.tarven.data.model.WorldInfo? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val jsonString = inputStream.bufferedReader().readText()
            inputStream.close()
            val result = characterRepo.importWorldInfoFromJson(jsonString)
            result.fold(
                onSuccess = { worldInfo ->
                    errorMessage = null
                    worldInfo
                },
                onFailure = { error ->
                    errorMessage = "导入世界书失败: ${error.message}"
                    null
                }
            )
        } catch (e: Exception) {
            errorMessage = "读取世界书文件失败: ${e.message}"
            null
        }
    }

    fun startEditCharacter(character: Character?) {
        editingCharacter = character ?: Character()
    }

    fun cancelEdit() {
        editingCharacter = null
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun clearError() {
        errorMessage = null
    }

    val filteredConversations: List<cat.tarven.data.model.ConversationWithCharacter>
        get() {
            if (searchQuery.isBlank()) return conversations
            return conversations.filter {
                it.character.name.contains(searchQuery, ignoreCase = true) ||
                it.character.description.contains(searchQuery, ignoreCase = true)
            }
        }
}
