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
import androidx.compose.ui.platform.LocalContext
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
fun AppearanceSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {

// === 显示与外观 ===
            SectionHeader("🎨 显示与外观")
            Spacer(modifier = Modifier.height(8.dp))

            // 界面字号
            Text(
                text = "📐 界面字号: ${settingsViewModel.appFontSize.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Slider(
                value = settingsViewModel.appFontSize,
                onValueChange = { settingsViewModel.updateAppFontSize(it) },
                valueRange = 10f..22f,
                steps = 11,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = TavernGold,
                    activeTrackColor = TavernPurple,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // 聊天字号
            Text(
                text = "📝 聊天字号: ${settingsViewModel.chatFontSize.toInt()}px",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Slider(
                value = settingsViewModel.chatFontSize,
                onValueChange = { settingsViewModel.updateChatFontSize(it) },
                valueRange = 10f..24f,
                steps = 13,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = TavernGold,
                    activeTrackColor = TavernPurple,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // 深色模式
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌙 深色模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settingsViewModel.isDarkMode,
                    onCheckedChange = { settingsViewModel.updateDarkMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TavernGold,
                        checkedTrackColor = TavernGoldDark.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.textMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // 背景图片
            val context = androidx.compose.ui.platform.LocalContext.current
            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { settingsViewModel.setBackgroundFromUri(context, it) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🖼️ 背景图片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }) {
                    Text("选择图片", color = TavernPurpleLight)
                }
                if (settingsViewModel.backgroundImagePath.isNotEmpty()) {
                    IconButton(onClick = { settingsViewModel.clearBackground(context) }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除背景", tint = ErrorRed)
                    }
                }
            }

            // 背景模糊度 (仅当有背景图片时显示)
            if (settingsViewModel.backgroundImagePath.isNotEmpty()) {
                Text(
                    text = "🔮 背景模糊度: ${settingsViewModel.backgroundBlurRadius.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.Slider(
                    value = settingsViewModel.backgroundBlurRadius,
                    onValueChange = { settingsViewModel.updateBackgroundBlurRadius(it) },
                    valueRange = 0f..25f,
                    steps = 24,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = TavernGold,
                        activeTrackColor = TavernPurple,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))
}