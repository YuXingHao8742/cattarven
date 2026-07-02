package cat.tarven.utils

/**
 * 思维链（Reasoning）解析器
 * 从 AI 回复文本中分离 <think>...</think> 推理块和正文内容
 */
object ThinkBlockParser {

    private val thinkRegex = Regex("<think>\\n?(.*?)\\n?</think>\\n*", RegexOption.DOT_MATCHES_ALL)

    /**
     * 从文本中提取 <think> 块，分离推理内容和正文
     * @return Pair(推理内容, 正文内容)，推理内容为 null 表示没有 think 块
     */
    fun extractThinkBlock(text: String): Pair<String?, String> {
        val match = thinkRegex.find(text)
        return if (match != null) {
            val reasoning = match.groupValues[1].trim()
            val content = text.removeRange(match.range).trim()
            Pair(reasoning.ifBlank { null }, content)
        } else {
            Pair(null, text)
        }
    }
}
