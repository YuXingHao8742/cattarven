package cat.tarven.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import java.io.File

/**
 * 设置数据仓库 — 管理 API 连接和应用设置
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cattarven_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_URL = "api_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_FREQ_PENALTY = "frequency_penalty"
        private const val KEY_PRES_PENALTY = "presence_penalty"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_STREAM_ENABLED = "stream_enabled"
        private const val KEY_ENABLE_HTML = "enable_html"
        private const val KEY_AUTO_SWIPE_COUNT = "auto_swipe_count"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PERSONA = "user_persona"

        // 显示与外观
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_APP_FONT_SIZE = "app_font_size"
        private const val KEY_CHAT_FONT_SIZE = "chat_font_size"
        private const val KEY_BG_IMAGE_PATH = "background_image_path"
        private const val KEY_BG_BLUR_RADIUS = "background_blur_radius"
        private const val KEY_USER_AVATAR_PATH = "user_avatar_path"

        const val DEFAULT_SYSTEM_PROMPT =
            "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}. " +
            "Write 1 reply only in internet RP style, avoid repetition, be creative and drive the plot forward. " +
            "Write at least 1 paragraph, up to 4. Always stay in character and avoid repetition."
    }

    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_URL, value) }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_MODEL_NAME, value) }

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_TEMPERATURE, value) }

    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, 1000)
        set(value) = prefs.edit { putInt(KEY_MAX_TOKENS, value) }

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_TOP_P, value) }

    var frequencyPenalty: Float
        get() = prefs.getFloat(KEY_FREQ_PENALTY, 0.0f)
        set(value) = prefs.edit { putFloat(KEY_FREQ_PENALTY, value) }

    var presencePenalty: Float
        get() = prefs.getFloat(KEY_PRES_PENALTY, 0.0f)
        set(value) = prefs.edit { putFloat(KEY_PRES_PENALTY, value) }

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SYSTEM_PROMPT, value) }

    var streamEnabled: Boolean
        get() = prefs.getBoolean(KEY_STREAM_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_STREAM_ENABLED, value) }

    var enableHtmlRendering: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_HTML, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLE_HTML, value) }

    var autoSwipeCount: Int
        get() = prefs.getInt(KEY_AUTO_SWIPE_COUNT, 1)
        set(value) = prefs.edit { putInt(KEY_AUTO_SWIPE_COUNT, value) }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "You") ?: "You"
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userPersona: String
        get() = prefs.getString(KEY_USER_PERSONA, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USER_PERSONA, value) }

    // --- 显示与外观 ---
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit { putBoolean(KEY_IS_DARK_MODE, value) }

    var appFontSize: Float
        get() = prefs.getFloat(KEY_APP_FONT_SIZE, 14f)
        set(value) = prefs.edit { putFloat(KEY_APP_FONT_SIZE, value) }

    var chatFontSize: Float
        get() = prefs.getFloat(KEY_CHAT_FONT_SIZE, 15f)
        set(value) = prefs.edit { putFloat(KEY_CHAT_FONT_SIZE, value) }

    var backgroundImagePath: String
        get() = prefs.getString(KEY_BG_IMAGE_PATH, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BG_IMAGE_PATH, value) }

    var backgroundBlurRadius: Float
        get() = prefs.getFloat(KEY_BG_BLUR_RADIUS, 10f)
        set(value) = prefs.edit { putFloat(KEY_BG_BLUR_RADIUS, value) }

    var userAvatarPath: String
        get() = prefs.getString(KEY_USER_AVATAR_PATH, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USER_AVATAR_PATH, value) }

    /**
     * 将用户选择的图片复制到 App 内部缓存目录
     */
    fun copyImageToCache(context: Context, sourceUri: Uri): String? {
        return try {
            val bgDir = File(context.filesDir, "backgrounds")
            if (!bgDir.exists()) bgDir.mkdirs()
            val destFile = File(bgDir, "custom_bg_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清除缓存的背景图片
     */
    fun clearBackgroundImage(context: Context) {
        val path = backgroundImagePath
        if (path.isNotBlank()) {
            File(path).delete()
        }
        backgroundImagePath = ""
    }

    /**
     * 将用户头像复制到 App 内部缓存目录
     */
    fun copyUserAvatarToCache(context: Context, sourceUri: Uri): String? {
        return try {
            val avatarDir = File(context.filesDir, "avatars")
            if (!avatarDir.exists()) avatarDir.mkdirs()
            val destFile = File(avatarDir, "user_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清除用户头像
     */
    fun clearUserAvatar(context: Context) {
        val path = userAvatarPath
        if (path.isNotBlank()) {
            File(path).delete()
        }
        userAvatarPath = ""
    }

    /**
     * 检查 API 配置是否完整
     */
    fun isApiConfigured(): Boolean {
        return apiUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }

    // --- API Presets 管理 ---
    private val gson = com.google.gson.Gson()
    private val KEY_API_PRESETS = "api_presets"

    fun getApiPresets(): List<ApiPreset> {
        val json = prefs.getString(KEY_API_PRESETS, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ApiPreset>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveApiPreset(preset: ApiPreset) {
        val current = getApiPresets().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(preset)
        }
        prefs.edit { putString(KEY_API_PRESETS, gson.toJson(current)) }
    }

    fun deleteApiPreset(id: String) {
        val current = getApiPresets().filter { it.id != id }
        prefs.edit { putString(KEY_API_PRESETS, gson.toJson(current)) }
    }

    // --- Regex Rules 管理 ---
    private val KEY_REGEX_RULES = "regex_rules"

    fun getRegexRules(): List<cat.tarven.data.model.RegexRule> {
        val json = prefs.getString(KEY_REGEX_RULES, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<cat.tarven.data.model.RegexRule>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRegexRules(rules: List<cat.tarven.data.model.RegexRule>) {
        prefs.edit { putString(KEY_REGEX_RULES, gson.toJson(rules)) }
    }

    // --- Props 管理 ---
    private val KEY_PROP_ITEMS = "prop_items"

    fun getPropItems(): List<PropItem> {
        val json = prefs.getString(KEY_PROP_ITEMS, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<PropItem>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePropItem(prop: PropItem) {
        val current = getPropItems().toMutableList()
        val index = current.indexOfFirst { it.id == prop.id }
        if (index >= 0) {
            current[index] = prop
        } else {
            current.add(prop)
        }
        prefs.edit { putString(KEY_PROP_ITEMS, gson.toJson(current)) }
    }

    fun deletePropItem(id: String) {
        val current = getPropItems().filter { it.id != id }
        prefs.edit { putString(KEY_PROP_ITEMS, gson.toJson(current)) }
    }
}

data class ApiPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val apiUrl: String,
    val apiKey: String,
    val modelName: String = ""
)

data class PropItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val content: String
)
