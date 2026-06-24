package cat.tarven.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.model.LabLog
import cat.tarven.data.repository.LabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LabRepository(application)

    private val _logs = MutableStateFlow<List<LabLog>>(emptyList())
    val logs: StateFlow<List<LabLog>> = _logs.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val allLogs = repository.getAllLogs()
            _logs.value = allLogs
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLogs()
            loadLogs()
        }
    }
}
