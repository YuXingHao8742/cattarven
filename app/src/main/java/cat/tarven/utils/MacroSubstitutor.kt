package cat.tarven.utils

import cat.tarven.data.model.Character

/**
 * 宏变量替换器 — 借鉴 SillyTavern 的 substituteParams
 * 将 {{char}}、{{user}} 等占位符替换为实际值
 */
object MacroSubstitutor {

    /**
     * 执行宏变量替换
     * @param text 包含宏变量的文本
     * @param character 当前角色
     * @param userName 当前用户名
     */
    fun substituteParams(text: String, character: Character, userName: String): String {
        return text
            .replace("{{char}}", character.name)
            .replace("{{user}}", userName)
            .replace("{{charIfNotGroup}}", character.name)
            .replace("{{personality}}", character.personality)
            .replace("{{scenario}}", character.scenario)
            .replace("{{description}}", character.description)
    }
}
