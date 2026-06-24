package cat.tarven.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.api.OpenAIService
import cat.tarven.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val apiService = OpenAIService()

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
    var autoSwipeCount by mutableStateOf(repository.autoSwipeCount)
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

    init {
        loadApiPresets()
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
}
