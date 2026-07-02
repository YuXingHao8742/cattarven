package cat.tarven.data.repository

import android.content.Context
import cat.tarven.data.model.Character
import cat.tarven.data.model.CharacterCardV2
import cat.tarven.data.model.WorldInfo
import cat.tarven.data.model.WorldInfoEntry
import cat.tarven.data.model.RawWorldInfoEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import cat.tarven.utils.WorldInfoMapper.isDisabled
import cat.tarven.utils.WorldInfoMapper.toWorldInfoEntry

/**
 * 角色数据仓库 — 管理角色卡的 CRUD
 */
@Singleton
class CharacterRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val gson = Gson()
    private val charactersDir: File
        get() = File(context.filesDir, "characters").also { it.mkdirs() }

    /**
     * 获取所有角色
     */
    fun getAllCharacters(): List<Character> {
        val dir = charactersDir
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), Character::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    /**
     * 获取单个角色
     */
    fun getCharacter(id: String): Character? {
        val file = File(charactersDir, "$id.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), Character::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存角色
     */
    fun saveCharacter(character: Character): Character {
        val updated = character.copy(updatedAt = System.currentTimeMillis())
        val file = File(charactersDir, "${updated.id}.json")
        file.writeText(gson.toJson(updated))
        return updated
    }

    /**
     * 删除角色
     */
    fun deleteCharacter(id: String) {
        File(charactersDir, "$id.json").delete()
    }

    /**
     * 从 JSON 字符串导入角色卡
     * 支持 SillyTavern V2 格式和简单格式
     */
    fun importCharacter(jsonString: String): Result<Character> {
        return try {
            // 先尝试 V2 格式
            val cardV2 = try {
                gson.fromJson(jsonString, CharacterCardV2::class.java)
            } catch (e: Exception) { null }

            val character = if ((cardV2?.spec == "chara_card_v2" || cardV2?.spec == "chara_card_v3") && cardV2.data.name.isNotBlank()) {
                cardV2.data.toCharacter()
            } else {
                // 尝试直接作为 CharacterCardData 解析（SillyTavern V1 或部分兼容格式），这样能正常解析根部的 character_book
                val flatCard = try {
                    gson.fromJson(jsonString, cat.tarven.data.model.CharacterCardData::class.java)
                } catch (e: Exception) { null }

                if (flatCard != null && flatCard.name.isNotBlank()) {
                    flatCard.toCharacter()
                } else {
                    // 尝试直接解析为 Character
                    try {
                        gson.fromJson(jsonString, Character::class.java)
                    } catch (e: Exception) {
                        // 尝试简单的 Map 格式（最坏的情况）
                        val map = gson.fromJson<Map<String, Any>>(
                            jsonString,
                            object : TypeToken<Map<String, Any>>() {}.type
                        )
                        Character(
                            name = map["name"]?.toString() ?: map["char_name"]?.toString() ?: "Unknown",
                            description = map["description"]?.toString() ?: map["char_persona"]?.toString() ?: "",
                            personality = map["personality"]?.toString() ?: "",
                            scenario = map["scenario"]?.toString() ?: map["world_scenario"]?.toString() ?: "",
                            firstMessage = map["first_mes"]?.toString() ?: map["char_greeting"]?.toString() ?: "",
                            messageExample = map["mes_example"]?.toString() ?: map["example_dialogue"]?.toString() ?: "",
                            alternateGreetings = (map["alternate_greetings"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        )
                    }
                }
            }

            if (character.name.isBlank()) {
                Result.failure(IllegalArgumentException("角色名称不能为空"))
            } else {
                val saved = saveCharacter(character)
                Result.success(saved)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从文件导入角色卡
     */
    fun importCharacterFromFile(file: File): Result<Character> {
        return try {
            val jsonString = file.readText()
            importCharacter(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从独立的 SillyTavern 世界书 JSON 文件导入
     * 支持格式: { "entries": { "1": { "key": [...], "content": "..." }, ... } }
     */
    fun importWorldInfoFromJson(jsonString: String): Result<WorldInfo> {
        return try {
            val jsonObj = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
            val entriesElement = jsonObj.get("entries") ?: return Result.failure(
                IllegalArgumentException("JSON 中没有 entries 字段")
            )
            
            val entryList = mutableListOf<WorldInfoEntry>()
            
            if (entriesElement.isJsonObject) {
                // 字典格式 {"1": {...}, "2": {...}}
                val entriesObj = entriesElement.asJsonObject
                for ((_, value) in entriesObj.entrySet()) {
                    val raw = gson.fromJson(value, RawWorldInfoEntry::class.java)
                    if (raw.isDisabled()) continue
                    entryList.add(raw.toWorldInfoEntry())
                }
            } else if (entriesElement.isJsonArray) {
                // 列表格式 [{...}, {...}]
                val arr = entriesElement.asJsonArray
                for (elem in arr) {
                    val raw = gson.fromJson(elem, RawWorldInfoEntry::class.java)
                    if (raw.isDisabled()) continue
                    entryList.add(raw.toWorldInfoEntry())
                }
            }
            
            if (entryList.isEmpty()) {
                Result.failure(IllegalArgumentException("没有找到有效的世界书词条"))
            } else {
                Result.success(WorldInfo(entries = entryList))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
