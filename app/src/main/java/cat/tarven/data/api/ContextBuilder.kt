package cat.tarven.data.api

import cat.tarven.data.model.Character
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.MessageRole
import cat.tarven.utils.ImageCompressor
import cat.tarven.utils.MacroSubstitutor

/**
 * 上文构建器 — 按八层顺序构建发送给 API 的消息数组
 *
 * 构建顺序：
 * ① 全局系统提示词 (system)
 * ② 角色系统提示词 (system)
 * ③ 玩家设定/马甲 (system)
 * ④ 主要设定前的世界书条目 (各自 role)
 * ⑤ 主要设定条目 (其 role)
 * ⑥ 主要设定后的世界书条目 (各自 role)
 * ⑦ 历史对话消息 (交替 user/assistant)
 * ⑧ 写作规则条目 (其 role)
 */
object ContextBuilder {

    /**
     * 上文构建所需的设置参数
     */
    data class Settings(
        val systemPrompt: String,
        val userName: String,
        val userPersona: String,
        val statusBarStripRegex: String
    )

    /**
     * 构建 OpenAI 格式的消息数组
     */
    fun build(
        character: Character,
        messages: List<ChatMessage>,
        settings: Settings,
        maxIndex: Int? = null
    ): List<ApiMessage> {
        val apiMessages = mutableListOf<ApiMessage>()

        // 截取目标范围内的历史消息
        val messagesToInclude = if (maxIndex != null) messages.subList(0, maxIndex + 1) else messages.toList()

        // --- 收集激活的世界书条目 ---
        val recentMessagesText = messagesToInclude.takeLast(10).joinToString(" ") { it.content }
        val wiEntries = character.worldInfo?.entries ?: emptyList()
        val activatedEntries = wiEntries.filter { entry ->
            !entry.disable && (entry.constant || (entry.keys.isNotEmpty() && entry.keys.any { key ->
                recentMessagesText.contains(key, ignoreCase = true)
            }))
        }

        // 按标签分类
        val mainSettingEntry = activatedEntries.firstOrNull { it.tag == "main_setting" }
        val writingRulesEntry = activatedEntries.firstOrNull { it.tag == "writing_rules" }
        val normalEntries = activatedEntries.filter { it.tag == "normal" }
        val beforeMainEntries = normalEntries.filter { it.relativePosition == "before_main" }.sortedBy { it.insertionOrder }
        val afterMainEntries = normalEntries.filter { it.relativePosition == "after_main" }.sortedBy { it.insertionOrder }

        // ① 全局提示词
        val globalPrompt = MacroSubstitutor.substituteParams(settings.systemPrompt, character, settings.userName).trim()
        if (globalPrompt.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = globalPrompt))
        }

        // ② 角色系统提示词
        val charSystemPrompt = MacroSubstitutor.substituteParams(character.systemPrompt, character, settings.userName).trim()
        if (charSystemPrompt.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = charSystemPrompt))
        }

        // ③ 玩家设定（马甲）
        val userPersona = settings.userPersona.trim()
        if (userPersona.isNotBlank()) {
            apiMessages.add(ApiMessage(role = "system", content = "[Player Character]\n$userPersona"))
        }

        // ④ 主要设定前的世界书条目
        beforeMainEntries.forEach { entry ->
            apiMessages.add(ApiMessage(role = entry.role, content = MacroSubstitutor.substituteParams(entry.content, character, settings.userName)))
        }

        // ⑤ 主要设定
        if (mainSettingEntry != null) {
            apiMessages.add(ApiMessage(role = mainSettingEntry.role, content = MacroSubstitutor.substituteParams(mainSettingEntry.content, character, settings.userName)))
        }

        // ⑥ 主要设定后的世界书条目
        afterMainEntries.forEach { entry ->
            apiMessages.add(ApiMessage(role = entry.role, content = MacroSubstitutor.substituteParams(entry.content, character, settings.userName)))
        }

        // ⑦ 历史对话
        val nonStreamingMessages = messagesToInclude.filter { !it.isStreaming }
        val lastAssistantIndex = nonStreamingMessages.indexOfLast { it.role == MessageRole.ASSISTANT }
        val stripRegexPattern = settings.statusBarStripRegex.trim()
        val stripRegex = if (stripRegexPattern.isNotBlank()) {
            try { Regex(stripRegexPattern, RegexOption.DOT_MATCHES_ALL) } catch (_: Exception) { null }
        } else null

        val historyMessages = nonStreamingMessages.mapIndexed { index, msg ->
            // 对非最后一条 assistant 消息应用状态栏剥离
            val shouldStrip = stripRegex != null && msg.role == MessageRole.ASSISTANT && index != lastAssistantIndex
            val content = if (shouldStrip) {
                stripRegex!!.replace(msg.displayContent, "").trim()
            } else {
                msg.displayContent
            }

            if (msg.attachments.isNotEmpty()) {
                // 构建多模态 content
                val contentParts = mutableListOf<Map<String, Any>>()

                for (att in msg.attachments) {
                    if (att.type == "image") {
                        try {
                            val base64 = ImageCompressor.getCompressedImageBase64(att.filePath)
                            if (base64 != null) {
                                val dataUrl = "data:image/jpeg;base64,$base64"
                                contentParts.add(mapOf(
                                    "type" to "image_url",
                                    "image_url" to mapOf("url" to dataUrl)
                                ))
                            }
                        } catch (_: Exception) { /* 跳过无法读取的图片 */ }
                    } else {
                        try {
                            val file = java.io.File(att.filePath)
                            if (file.exists()) {
                                val fileContent = file.readText(Charsets.UTF_8)
                                contentParts.add(mapOf(
                                    "type" to "text",
                                    "text" to "[用户上传了文件: ${att.fileName}]\n文件内容:\n$fileContent"
                                ))
                            }
                        } catch (_: Exception) { /* 跳过无法读取的文件 */ }
                    }
                }

                if (content.isNotBlank()) {
                    contentParts.add(mapOf("type" to "text", "text" to content))
                }

                ApiMessage(role = msg.role.value, content = contentParts as Any)
            } else {
                ApiMessage(role = msg.role.value, content = content as Any)
            }
        }
        apiMessages.addAll(historyMessages)

        // ⑧ 写作规则
        if (writingRulesEntry != null) {
            apiMessages.add(ApiMessage(role = writingRulesEntry.role, content = MacroSubstitutor.substituteParams(writingRulesEntry.content, character, settings.userName)))
        }

        return apiMessages
    }
}
