package cat.tarven.data.model

@androidx.annotation.Keep
data class LabLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val characterName: String,
    val requestJson: String
)
