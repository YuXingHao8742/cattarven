package cat.tarven.data.model

import java.util.UUID

@androidx.annotation.Keep
data class RegexRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val pattern: String = "",
    val replacement: String = "",
    val isEnabled: Boolean = true
)
