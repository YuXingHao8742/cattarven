package cat.tarven.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * 角色数据模型
 * 兼容 SillyTavern V2 角色卡 JSON 格式
 */
data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerializedName("first_mes")
    val firstMessage: String = "",
    @SerializedName("mes_example")
    val messageExample: String = "",
    @SerializedName("system_prompt")
    val systemPrompt: String = "",
    @SerializedName("creator_notes")
    val creatorNotes: String = "",
    @SerializedName("post_history_instructions")
    val postHistoryInstructions: String = "",
    val tags: List<String> = emptyList(),
    @SerializedName("alternate_greetings")
    val alternateGreetings: List<String> = emptyList(),
    val creator: String = "",
    val avatarUri: String? = null,
    val worldInfo: WorldInfo? = null,
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    val regexRules: List<RegexRule> = emptyList()
) {
    /**
     * 构建完整的角色设定文本，用于作为 system message 发送给 API
     */
    fun buildCharacterPrompt(): String {
        return description.trim()
    }

    /**
     * 将内部 Character 对象转换为 SillyTavern V2 标准格式
     */
    fun toCharacterCardV2(): CharacterCardV2 {
        val regexScripts = regexRules.map { rule ->
            mapOf(
                "id" to rule.id,
                "scriptName" to rule.name,
                "findRegex" to rule.pattern,
                "replaceString" to rule.replacement,
                "disabled" to !rule.isEnabled,
                "markdownOnly" to true,
                "promptOnly" to false
            )
        }

        val entries = worldInfo?.entries?.map { entry ->
            CharacterBookEntry(
                uid = entry.id.hashCode(),
                key = entry.keys,
                keysecondary = entry.keysecondary,
                comment = entry.comment,
                content = entry.content,
                constant = entry.constant,
                selective = entry.selective,
                insertion_order = entry.insertionOrder,
                enabled = !entry.disable,
                position = entry.position,
                depth = entry.depth,
                role = when (entry.role) {
                    "system" -> 0
                    "user" -> 1
                    "assistant" -> 2
                    else -> 0
                }
            )
        } ?: emptyList()

        val characterBook = if (entries.isNotEmpty()) {
            CharacterBook(
                name = this.name,
                description = "World info for ${this.name}",
                entries = entries
            )
        } else {
            null
        }

        val extensions = if (regexScripts.isNotEmpty()) {
            mapOf("regex_scripts" to regexScripts)
        } else {
            null
        }

        val data = CharacterCardData(
            name = this.name,
            description = this.description,
            personality = this.personality,
            scenario = this.scenario,
            firstMes = this.firstMessage,
            mesExample = this.messageExample,
            systemPrompt = this.systemPrompt,
            creatorNotes = this.creatorNotes,
            postHistoryInstructions = this.postHistoryInstructions,
            tags = this.tags,
            alternateGreetings = this.alternateGreetings,
            creator = this.creator,
            characterVersion = "",
            extensions = extensions,
            characterBook = characterBook
        )

        return CharacterCardV2(data = data)
    }
}

/**
 * SillyTavern V2 角色卡格式（用于导入）
 */
data class CharacterCardV2(
    val spec: String = "chara_card_v2",
    @SerializedName("spec_version")
    val specVersion: String = "2.0",
    val data: CharacterCardData = CharacterCardData()
)

data class CharacterCardData(
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerializedName("first_mes")
    val firstMes: String = "",
    @SerializedName("mes_example")
    val mesExample: String = "",
    @SerializedName("system_prompt")
    val systemPrompt: String = "",
    @SerializedName("creator_notes")
    val creatorNotes: String = "",
    @SerializedName("post_history_instructions")
    val postHistoryInstructions: String = "",
    val tags: List<String> = emptyList(),
    @SerializedName("alternate_greetings")
    val alternateGreetings: List<String> = emptyList(),
    val creator: String = "",
    @SerializedName("character_version")
    val characterVersion: String = "",
    val extensions: Map<String, Any>? = null,
    @SerializedName("character_book")
    val characterBook: CharacterBook? = null
) {
    fun toCharacter(): Character {
        // 解析 character_book 为 WorldInfo
        val worldInfo = characterBook?.toWorldInfo()
        
        // 解析 extensions.regex_scripts 为 RegexRule 列表
        val regexRules = try {
            val regexScripts = extensions?.get("regex_scripts")
            if (regexScripts != null) {
                val gson = Gson()
                val jsonStr = gson.toJson(regexScripts)
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val rawList: List<Map<String, Any>> = gson.fromJson(jsonStr, listType)
                rawList.mapNotNull { map ->
                    val disabled = map["disabled"] as? Boolean ?: false
                    val markdownOnly = map["markdownOnly"] as? Boolean ?: true
                    val promptOnly = map["promptOnly"] as? Boolean ?: false
                    // 只导入 markdownOnly（显示用）的规则，跳过 promptOnly 的
                    if (promptOnly && !markdownOnly) return@mapNotNull null
                    RegexRule(
                        id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                        name = (map["scriptName"] as? String) ?: "",
                        pattern = (map["findRegex"] as? String) ?: "",
                        replacement = (map["replaceString"] as? String) ?: "",
                        isEnabled = !disabled
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
        
        return Character(
            name = name,
            description = description,
            personality = personality,
            scenario = scenario,
            firstMessage = firstMes,
            messageExample = mesExample,
            systemPrompt = systemPrompt,
            creatorNotes = creatorNotes,
            postHistoryInstructions = postHistoryInstructions,
            tags = tags,
            alternateGreetings = alternateGreetings,
            creator = creator,
            worldInfo = worldInfo,
            regexRules = regexRules
        )
    }
}

/**
 * SillyTavern V2 角色卡中的 character_book（内嵌世界书）
 * entries 在 JSON 中可能是字典 {"0": {...}, "1": {...}} 或列表 [{...}, {...}]
 */
data class CharacterBook(
    val name: String? = null,
    val description: String? = null,
    val entries: Any? = null // 可能是 Map 或 List
) {
    fun toWorldInfo(): WorldInfo {
        if (entries == null) return WorldInfo()
        val gson = Gson()
        val jsonStr = gson.toJson(entries)

        // 尝试作为 Map<String, RawEntry> 解析
        val entryList = try {
            val mapType = object : TypeToken<Map<String, RawWorldInfoEntry>>() {}.type
            val map: Map<String, RawWorldInfoEntry> = gson.fromJson(jsonStr, mapType)
            map.values.toList()
        } catch (e: Exception) {
            // 尝试作为 List<RawEntry> 解析
            try {
                val listType = object : TypeToken<List<RawWorldInfoEntry>>() {}.type
                gson.fromJson<List<RawWorldInfoEntry>>(jsonStr, listType)
            } catch (e2: Exception) {
                emptyList()
            }
        }

        return WorldInfo(
            entries = entryList.filter { 
                val isDisabled = it.disable ?: !(it.enabled ?: true)
                !isDisabled
            }.map { raw ->
                WorldInfoEntry(
                    keys = raw.keys ?: raw.key ?: emptyList(),
                    content = raw.content ?: "",
                    insertionOrder = raw.insertion_order ?: raw.insertionorder ?: raw.order ?: 0,
                    comment = raw.comment ?: "",
                    constant = raw.constant ?: false,
                    disable = raw.disable ?: !(raw.enabled ?: true),
                    selective = raw.selective ?: false,
                    keysecondary = raw.secondary_keys ?: raw.keysecondary ?: emptyList(),
                    position = when (val p = raw.position) {
                        is Number -> p.toInt()
                        is String -> p.toIntOrNull() ?: 1
                        else -> 1
                    },
                    depth = raw.depth ?: 4,
                    role = when (raw.role) {
                        0, "0" -> "system"
                        1, "1" -> "user"
                        2, "2" -> "assistant"
                        else -> "system"
                    }
                )
            }
        )
    }
}

/**
 * SillyTavern 原始世界书词条格式（用于反序列化）
 */
data class RawWorldInfoEntry(
    val uid: Int? = null,
    val key: List<String>? = null,
    val keys: List<String>? = null,
    val keysecondary: List<String>? = null,
    val secondary_keys: List<String>? = null,
    val comment: String? = null,
    val content: String? = null,
    val constant: Boolean? = null,
    val disable: Boolean? = null,
    val enabled: Boolean? = null,
    val selective: Boolean? = null,
    val insertionorder: Int? = null,
    val insertion_order: Int? = null,
    val order: Int? = null,
    val position: Any? = null,
    val depth: Int? = null,
    val role: Any? = null
)

/**
 * SillyTavern V2 角色卡中 character_book.entries 的标准格式（用于导出序列化）
 */
data class CharacterBookEntry(
    val uid: Int = 0,
    val key: List<String> = emptyList(),
    val keysecondary: List<String> = emptyList(),
    val comment: String = "",
    val content: String = "",
    val constant: Boolean = false,
    val selective: Boolean = false,
    val insertion_order: Int = 0,
    val enabled: Boolean = true,
    val position: Int = 1,
    val depth: Int = 4,
    val role: Int = 0
)

