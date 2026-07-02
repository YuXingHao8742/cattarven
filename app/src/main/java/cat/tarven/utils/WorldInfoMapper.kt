package cat.tarven.utils

import cat.tarven.data.model.RawWorldInfoEntry
import cat.tarven.data.model.WorldInfoEntry

object WorldInfoMapper {
    /**
     * 将原始世界书词条转换为内部使用的数据模型
     */
    fun RawWorldInfoEntry.toWorldInfoEntry(): WorldInfoEntry {
        return WorldInfoEntry(
            keys = this.keys ?: this.key ?: emptyList(),
            content = this.content ?: "",
            insertionOrder = this.insertion_order ?: this.insertionorder ?: this.order ?: 0,
            comment = this.comment ?: "",
            constant = this.constant ?: false,
            disable = this.disable ?: !(this.enabled ?: true),
            selective = this.selective ?: false,
            keysecondary = this.secondary_keys ?: this.keysecondary ?: emptyList(),
            position = when (val p = this.position) {
                is Number -> p.toInt()
                is String -> p.toIntOrNull() ?: 1
                else -> 1
            },
            depth = this.depth ?: 4,
            role = when (this.role) {
                0, "0" -> "system"
                1, "1" -> "user"
                2, "2" -> "assistant"
                else -> "system"
            }
        )
    }

    /**
     * 判断原始词条是否被禁用
     */
    fun RawWorldInfoEntry.isDisabled(): Boolean {
        return this.disable ?: !(this.enabled ?: true)
    }
}
