package cat.tarven.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.tarven.data.model.LabLog
import cat.tarven.data.repository.LabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LabViewModel @Inject constructor(
    private val repository: LabRepository
) : ViewModel() {

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
