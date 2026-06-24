package cat.tarven.data.model

/**
 * App 内部的世界书模型 — 不添加 SerializedName，保持 keys 字段名
 * SillyTavern 格式转换只在导入时处理
 */
data class WorldInfo(
    val entries: List<WorldInfoEntry> = emptyList()
)

data class WorldInfoEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val keys: List<String> = emptyList(),
    val content: String = "",
    val insertionOrder: Int = 0,
    val comment: String = "",
    val constant: Boolean = false,
    val disable: Boolean = false,
    val selective: Boolean = false,
    val keysecondary: List<String> = emptyList(),
    // 消息身份: "system" / "user" / "assistant"
    val role: String = "system",
    // 标签类型: "main_setting" / "writing_rules" / "normal"
    val tag: String = "normal",
    // 普通条目的相对位置: "before_main" / "after_main"
    val relativePosition: String = "after_main",
    // 以下字段保留兼容旧数据但不再在 UI 中展示
    val position: Int = 1,
    val depth: Int = 4
)
