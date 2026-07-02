package cat.tarven.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.SettingsViewModel



@Composable
fun ParameterSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {

// === 生成参数 ===
            SectionHeader("生成参数")

            // Temperature
            ParameterSlider(
                label = "Temperature",
                value = settingsViewModel.temperature,
                onValueChange = { settingsViewModel.updateTemperature(it) },
                valueRange = 0f..2f,
                steps = 39
            )

            // Max Tokens
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Max Tokens",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = settingsViewModel.maxTokens.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { settingsViewModel.updateMaxTokens(it.coerceIn(1, 128000)) }
                    },
                    singleLine = true,
                    modifier = Modifier.width(120.dp),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Top P
            ParameterSlider(
                label = "Top P",
                value = settingsViewModel.topP,
                onValueChange = { settingsViewModel.updateTopP(it) },
                valueRange = 0f..1f,
                steps = 19
            )

            // Frequency Penalty
            ParameterSlider(
                label = "Frequency Penalty",
                value = settingsViewModel.frequencyPenalty,
                onValueChange = { settingsViewModel.updateFrequencyPenalty(it) },
                valueRange = -2f..2f,
                steps = 39
            )

            // Presence Penalty
            ParameterSlider(
                label = "Presence Penalty",
                value = settingsViewModel.presencePenalty,
                onValueChange = { settingsViewModel.updatePresencePenalty(it) },
                valueRange = -2f..2f,
                steps = 39
            )

            // 流式传输开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "流式传输 (SSE)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settingsViewModel.streamEnabled,
                    onCheckedChange = { settingsViewModel.updateStreamEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TavernGold,
                        checkedTrackColor = TavernGoldDark.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.textMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // HTML 渲染开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "富文本 (HTML) 渲染",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settingsViewModel.enableHtmlRendering,
                    onCheckedChange = { settingsViewModel.updateEnableHtmlRendering(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TavernGold,
                        checkedTrackColor = TavernGoldDark.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.textMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 自动生成备选回复数
            Text(
                text = "每次生成备选回复数: ${settingsViewModel.autoSwipeCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Slider(
                value = settingsViewModel.autoSwipeCount.toFloat(),
                onValueChange = { settingsViewModel.updateAutoSwipeCount(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = TavernGold,
                    activeTrackColor = TavernPurple,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Text(
                text = "发送消息时，AI 会在后台自动生成此数量的独立回复供您左右滑动挑选。数量越多，所需等待时间和 Token 消耗越大。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.textMuted
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))
}