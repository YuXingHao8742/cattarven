package cat.tarven.utils

import cat.tarven.data.api.ChatCompletionRequest

/**
 * API 名称清理工具
 * 处理 OpenAI API 对 name 字段的格式限制 (^[a-zA-Z0-9_-]{1,64}$)
 */
object ApiNameSanitizer {

    /**
     * 将名字格式化为符合 OpenAI API 规范的字符串
     * 替换中文等非 [a-zA-Z0-9_-] 字符为空
     */
    fun sanitizeName(name: String): String? {
        val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "")
        return if (sanitized.isNotBlank()) sanitized.take(64) else null
    }

    /**
     * 清理请求中的 Base64 图片数据，防止实验室查看 JSON 时卡死
     */
    fun sanitizeRequestForLog(request: ChatCompletionRequest): ChatCompletionRequest {
        val safeMessages = request.messages.map { msg ->
            if (msg.content is List<*>) {
                val newContent = msg.content.map { part ->
                    if (part is Map<*, *> && part["type"] == "image_url") {
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "<图片 Base64 编码已隐藏>")
                        )
                    } else part
                }
                msg.copy(content = newContent)
            } else {
                msg
            }
        }
        return request.copy(messages = safeMessages)
    }
}
