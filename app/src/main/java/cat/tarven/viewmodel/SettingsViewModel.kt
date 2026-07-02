package cat.tarven.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.api.OpenAIService
import cat.tarven.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val apiService: OpenAIService
) : ViewModel() {

    var apiUrl by mutableStateOf(repository.apiUrl)
        private set
    var apiKey by mutableStateOf(repository.apiKey)
        private set
    var modelName by mutableStateOf(repository.modelName)
        private set
    var temperature by mutableStateOf(repository.temperature)
        private set
    var maxTokens by mutableStateOf(repository.maxTokens)
        private set
    var topP by mutableStateOf(repository.topP)
        private set
    var frequencyPenalty by mutableStateOf(repository.frequencyPenalty)
        private set
    var presencePenalty by mutableStateOf(repository.presencePenalty)
        private set
    var systemPrompt by mutableStateOf(repository.systemPrompt)
        private set
    var streamEnabled by mutableStateOf(repository.streamEnabled)
        private set
    var enableHtmlRendering by mutableStateOf(repository.enableHtmlRendering)
        private set
    var userName by mutableStateOf(repository.userName)
        private set
    var userPersona by mutableStateOf(repository.userPersona)
        private set
    var statusBarStripRegex by mutableStateOf(repository.statusBarStripRegex)
        private set
    var autoSwipeCount by mutableStateOf(repository.autoSwipeCount)
        private set

    // 显示与外观
    var isDarkMode by mutableStateOf(repository.isDarkMode)
        private set
    var appFontSize by mutableStateOf(repository.appFontSize)
        private set
    var chatFontSize by mutableStateOf(repository.chatFontSize)
        private set
    var backgroundImagePath by mutableStateOf(repository.backgroundImagePath)
        private set
    var backgroundBlurRadius by mutableStateOf(repository.backgroundBlurRadius)
        private set
    var userAvatarPath by mutableStateOf(repository.userAvatarPath)
        private set

    // 连接测试状态
    var connectionTestResult by mutableStateOf<String?>(null)
        private set
    var isTestingConnection by mutableStateOf(false)
        private set
    var availableModels by mutableStateOf<List<String>>(emptyList())
        private set

    // API 预设列表
    var apiPresets by mutableStateOf<List<cat.tarven.data.repository.ApiPreset>>(emptyList())
        private set

    // 正则扩展列表
    var regexRules by mutableStateOf<List<cat.tarven.data.model.RegexRule>>(emptyList())
        private set

    // Props
    var propItems by mutableStateOf<List<cat.tarven.data.repository.PropItem>>(emptyList())
        private set

    init {
        loadSettings()
        apiPresets = repository.getApiPresets()
        regexRules = repository.getRegexRules()
        propItems = repository.getPropItems()
    }

    private fun loadSettings() {
        // Placeholder for future general settings loading if needed
    }

    private fun loadApiPresets() {
        apiPresets = repository.getApiPresets()
    }

    fun saveCurrentAsPreset(name: String) {
        val preset = cat.tarven.data.repository.ApiPreset(
            name = name,
            apiUrl = apiUrl,
            apiKey = apiKey,
            modelName = modelName
        )
        repository.saveApiPreset(preset)
        loadApiPresets()
    }

    fun deletePreset(id: String) {
        repository.deleteApiPreset(id)
        loadApiPresets()
    }

    fun applyPreset(preset: cat.tarven.data.repository.ApiPreset) {
        updateApiUrl(preset.apiUrl)
        updateApiKey(preset.apiKey)
        if (preset.modelName.isNotBlank()) {
            updateModelName(preset.modelName)
        }
    }

    fun updateApiUrl(value: String) {
        apiUrl = value
        repository.apiUrl = value
    }

    fun updateApiKey(value: String) {
        apiKey = value
        repository.apiKey = value
    }

    fun updateModelName(value: String) {
        modelName = value
        repository.modelName = value
    }

    fun updateTemperature(value: Float) {
        temperature = value
        repository.temperature = value
    }

    fun updateMaxTokens(value: Int) {
        maxTokens = value
        repository.maxTokens = value
    }

    fun updateTopP(value: Float) {
        topP = value
        repository.topP = value
    }

    fun updateFrequencyPenalty(value: Float) {
        frequencyPenalty = value
        repository.frequencyPenalty = value
    }

    fun updatePresencePenalty(value: Float) {
        presencePenalty = value
        repository.presencePenalty = value
    }

    fun updateSystemPrompt(value: String) {
        systemPrompt = value
        repository.systemPrompt = value
    }

    fun updateStreamEnabled(value: Boolean) {
        streamEnabled = value
        repository.streamEnabled = value
    }

    fun updateEnableHtmlRendering(enabled: Boolean) {
        enableHtmlRendering = enabled
        repository.enableHtmlRendering = enabled
    }

    fun updateAutoSwipeCount(count: Int) {
        autoSwipeCount = count
        repository.autoSwipeCount = count
    }

    fun updateUserName(name: String) {
        userName = name
        repository.userName = name
    }

    fun updateUserPersona(value: String) {
        userPersona = value
        repository.userPersona = value
    }

    fun updateStatusBarStripRegex(value: String) {
        statusBarStripRegex = value
        repository.statusBarStripRegex = value
    }

    // --- 显示与外观 ---
    fun updateDarkMode(value: Boolean) {
        isDarkMode = value
        repository.isDarkMode = value
    }

    fun updateAppFontSize(value: Float) {
        appFontSize = value
        repository.appFontSize = value
    }

    fun updateChatFontSize(value: Float) {
        chatFontSize = value
        repository.chatFontSize = value
    }

    fun updateBackgroundBlurRadius(value: Float) {
        backgroundBlurRadius = value
        repository.backgroundBlurRadius = value
    }

    fun setBackgroundFromUri(context: Context, uri: Uri) {
        // 先清除旧的
        if (backgroundImagePath.isNotBlank()) {
            repository.clearBackgroundImage(context)
        }
        val cachedPath = repository.copyImageToCache(context, uri)
        if (cachedPath != null) {
            backgroundImagePath = cachedPath
            repository.backgroundImagePath = cachedPath
        }
    }

    fun clearBackground(context: Context) {
        repository.clearBackgroundImage(context)
        backgroundImagePath = ""
    }

    fun setUserAvatarFromUri(context: Context, uri: Uri) {
        if (userAvatarPath.isNotBlank()) {
            repository.clearUserAvatar(context)
        }
        val cachedPath = repository.copyUserAvatarToCache(context, uri)
        if (cachedPath != null) {
            userAvatarPath = cachedPath
            repository.userAvatarPath = cachedPath
        }
    }

    fun clearUserAvatar(context: Context) {
        repository.clearUserAvatar(context)
        userAvatarPath = ""
    }



    fun testConnection() {
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            connectionTestResult = "❌ 请先填写 API 地址和密钥"
            return
        }

        isTestingConnection = true
        connectionTestResult = null

        viewModelScope.launch {
            val result = apiService.testConnection(apiUrl, apiKey)
            result.fold(
                onSuccess = { models ->
                    availableModels = models
                    connectionTestResult = "✅ 连接成功！发现 ${models.size} 个模型"
                },
                onFailure = { error ->
                    connectionTestResult = "❌ 连接失败: ${error.message}"
                }
            )
            isTestingConnection = false
        }
    }

    fun isApiConfigured(): Boolean = repository.isApiConfigured()

    // --- 正则扩展管理 ---
    private fun loadRegexRules() {
        regexRules = repository.getRegexRules()
    }

    fun addRegexRule(rule: cat.tarven.data.model.RegexRule) {
        val newList = regexRules + rule
        repository.saveRegexRules(newList)
        regexRules = newList
    }

    fun updateRegexRule(rule: cat.tarven.data.model.RegexRule) {
        val newList = regexRules.map { if (it.id == rule.id) rule else it }
        repository.saveRegexRules(newList)
        regexRules = newList
    }

    fun deleteRegexRule(id: String) {
        val newList = regexRules.filter { it.id != id }
        repository.saveRegexRules(newList)
        regexRules = newList
    }

    fun toggleRegexRule(id: String, isEnabled: Boolean) {
        val newList = regexRules.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
        repository.saveRegexRules(newList)
        regexRules = newList
    }

    // --- Props 管理 ---
    fun savePropItem(prop: cat.tarven.data.repository.PropItem) {
        repository.savePropItem(prop)
        propItems = repository.getPropItems()
    }

    fun deletePropItem(id: String) {
        repository.deletePropItem(id)
        propItems = repository.getPropItems()
    }
}
