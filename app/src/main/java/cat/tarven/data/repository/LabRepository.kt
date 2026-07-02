package cat.tarven.data.repository

import android.content.Context
import cat.tarven.data.model.LabLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val logsDir: File
        get() = File(context.filesDir, "lab_logs").also { it.mkdirs() }

    fun saveLog(characterName: String, requestObj: Any) {
        try {
            val jsonStr = gson.toJson(requestObj)
            val log = LabLog(
                characterName = characterName,
                requestJson = jsonStr
            )
            val file = File(logsDir, "${log.id}.json")
            file.writeText(gson.toJson(log))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllLogs(): List<LabLog> {
        val dir = logsDir
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), LabLog::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    fun clearAllLogs() {
        logsDir.listFiles()?.forEach { it.delete() }
    }
}
