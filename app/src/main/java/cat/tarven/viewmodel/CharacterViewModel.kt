package cat.tarven.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cat.tarven.data.model.Character
import cat.tarven.data.repository.CharacterRepository
import cat.tarven.data.repository.ChatRepository

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val characterRepo = CharacterRepository(application)
    private val chatRepo = ChatRepository(application)

    var characters by mutableStateOf<List<Character>>(emptyList())
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
        loadCharacters()
    }

    fun loadCharacters() {
        isLoading = true
        characters = characterRepo.getAllCharacters()
        isLoading = false
    }

    fun selectCharacter(character: Character) {
        selectedCharacter = character
    }

    fun saveCharacter(character: Character) {
        characterRepo.saveCharacter(character)
        loadCharacters()
        errorMessage = null
    }

    fun deleteCharacter(id: String) {
        characterRepo.deleteCharacter(id)
        chatRepo.deleteAllConversations(id)
        if (selectedCharacter?.id == id) {
            selectedCharacter = null
        }
        loadCharacters()
    }

    fun importCharacterFromJson(jsonString: String) {
        val result = characterRepo.importCharacter(jsonString)
        result.fold(
            onSuccess = {
                loadCharacters()
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
                    loadCharacters()
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

    val filteredCharacters: List<Character>
        get() {
            if (searchQuery.isBlank()) return characters
            return characters.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
}
