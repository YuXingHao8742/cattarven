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
fun UserSettingSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {

// === 用户设置 ===
            SectionHeader("用户设置")
            val context = androidx.compose.ui.platform.LocalContext.current

            // 用户头像
            val avatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { settingsViewModel.setUserAvatarFromUri(context, it) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像预览
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { avatarPickerLauncher.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (settingsViewModel.userAvatarPath.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = "file://${settingsViewModel.userAvatarPath}",
                            contentDescription = "用户头像",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = settingsViewModel.userName.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👤 用户头像",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (settingsViewModel.userAvatarPath.isNotBlank()) "点击头像更换" else "点击头像设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.textMuted
                    )
                }
                if (settingsViewModel.userAvatarPath.isNotBlank()) {
                    IconButton(onClick = { settingsViewModel.clearUserAvatar(context) }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除头像", tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settingsViewModel.userName,
                onValueChange = { settingsViewModel.updateUserName(it) },
                label = { Text("用户名称") },
                placeholder = { Text("你的名字", color = MaterialTheme.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settingsViewModel.userPersona,
                onValueChange = { settingsViewModel.updateUserPersona(it) },
                label = { Text("玩家设定（马甲）") },
                placeholder = { Text("对自己角色的介绍，会作为玩家设定发送给 AI", color = MaterialTheme.textMuted) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

// === 系统提示词 ===
            SectionHeader("全局系统提示词")

            Text(
                text = "支持 {{char}} 和 {{user}} 变量替换",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = settingsViewModel.systemPrompt,
                onValueChange = { settingsViewModel.updateSystemPrompt(it) },
                label = { Text("System Prompt") },
                placeholder = { 
                    Text(cat.tarven.data.repository.SettingsRepository.DEFAULT_SYSTEM_PROMPT, color = MaterialTheme.textMuted)
                },
                minLines = 4,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))
}