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
fun PropsManagementSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {
    var showPropDialog by remember { mutableStateOf(false) }
    var editingProp by remember { mutableStateOf<cat.tarven.data.repository.PropItem?>(null) }
    var propNameInput by remember { mutableStateOf("") }
    var propContentInput by remember { mutableStateOf("") }

// === 上文状态栏剥离 ===
            SectionHeader("🧹 上文状态栏剥离")

            Text(
                text = "填入正则表达式后，发送给 AI 的历史消息中，除最后一条 AI 回复外，其余 AI 回复将自动剥离匹配到的内容（例如状态栏）。留空则不启用。仅影响发送，不影响显示。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = settingsViewModel.statusBarStripRegex,
                onValueChange = { settingsViewModel.updateStatusBarStripRegex(it) },
                label = { Text("状态栏匹配正则") },
                placeholder = { Text("例如: <status>[\\\\s\\\\S]*?</status>", color = MaterialTheme.textMuted) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))



            SectionHeader("🎒 快捷道具管理")
            
            Text(
                text = "你可以预设一些常用的长文本（如总结、要求等），然后在聊天界面一键发送。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            settingsViewModel.propItems.forEach { prop ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prop.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(text = prop.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.textMuted, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { 
                        editingProp = prop
                        propNameInput = prop.name
                        propContentInput = prop.content
                        showPropDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { settingsViewModel.deletePropItem(prop.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { 
                    editingProp = null
                    propNameInput = ""
                    propContentInput = ""
                    showPropDialog = true 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TavernPurpleLight),
                border = BorderStroke(1.dp, TavernPurpleDark)
            ) {
                Text("+ 添加新道具")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            if (showPropDialog) {
                AlertDialog(
                    onDismissRequest = { showPropDialog = false },
                    title = { Text(if (editingProp == null) "添加道具" else "编辑道具", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = propNameInput,
                                onValueChange = { propNameInput = it },
                                label = { Text("道具名称 (触发词)") },
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = propContentInput,
                                onValueChange = { propContentInput = it },
                                label = { Text("道具内容 (发送的文本)") },
                                minLines = 3,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (propNameInput.isNotBlank() && propContentInput.isNotBlank()) {
                                    val newProp = cat.tarven.data.repository.PropItem(
                                        id = editingProp?.id ?: java.util.UUID.randomUUID().toString(),
                                        name = propNameInput,
                                        content = propContentInput
                                    )
                                    settingsViewModel.savePropItem(newProp)
                                    showPropDialog = false
                                }
                            }
                        ) {
                            Text("保存", color = TavernPurpleLight)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPropDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.surfaceElevated
                )
            }
}