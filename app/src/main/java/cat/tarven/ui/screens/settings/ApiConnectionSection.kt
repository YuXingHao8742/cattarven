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
fun ApiConnectionSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {
    var showApiKey by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var presetDropdownExpanded by remember { mutableStateOf(false) }

// === API 连接 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("API 连接")
                TextButton(onClick = { showSavePresetDialog = true }) {
                    Text("保存预设", color = TavernPurpleLight)
                }
            }

            if (settingsViewModel.apiPresets.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { presetDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("从预设加载...")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = presetDropdownExpanded,
                        onDismissRequest = { presetDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.surfaceElevated)
                    ) {
                        settingsViewModel.apiPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(preset.name, color = MaterialTheme.colorScheme.onSurface)
                                        Text(preset.apiUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.textMuted)
                                    }
                                },
                                onClick = {
                                    settingsViewModel.applyPreset(preset)
                                    presetDropdownExpanded = false
                                },
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        settingsViewModel.deletePreset(preset.id)
                                        if (settingsViewModel.apiPresets.isEmpty()) {
                                            presetDropdownExpanded = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, "删除预设", tint = ErrorRed)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = settingsViewModel.apiUrl,
                onValueChange = { settingsViewModel.updateApiUrl(it) },
                label = { Text("API 地址") },
                placeholder = { Text("https://api.openai.com", color = MaterialTheme.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settingsViewModel.apiKey,
                onValueChange = { settingsViewModel.updateApiKey(it) },
                label = { Text("API 密钥") },
                placeholder = { Text("sk-...", color = MaterialTheme.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (showApiKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "切换显示",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settingsViewModel.modelName,
                onValueChange = { settingsViewModel.updateModelName(it) },
                label = { Text("模型名称") },
                placeholder = { Text("gpt-4o / claude-3.5-sonnet / ...", color = MaterialTheme.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 连接测试按钮
            Button(
                onClick = { settingsViewModel.testConnection() },
                enabled = !settingsViewModel.isTestingConnection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TavernGoldDark,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (settingsViewModel.isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试中...")
                } else {
                    Text("测试连接", fontWeight = FontWeight.SemiBold)
                }
            }

            // 测试结果
            settingsViewModel.connectionTestResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.startsWith("✅")) SuccessGreen else ErrorRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                )
            }

            // 如果有可用模型，显示列表
            if (settingsViewModel.availableModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击选择模型：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                val modelListState = androidx.compose.foundation.lazy.rememberLazyListState()

                // 自动滚动到已选中的模型或搜索匹配的模型
                androidx.compose.runtime.LaunchedEffect(settingsViewModel.modelName, settingsViewModel.availableModels) {
                    val models = settingsViewModel.availableModels
                    val currentModel = settingsViewModel.modelName
                    if (currentModel.isNotBlank() && models.isNotEmpty()) {
                        // 优先精确匹配
                        var targetIndex = models.indexOf(currentModel)
                        // 无精确匹配时模糊搜索
                        if (targetIndex < 0) {
                            targetIndex = models.indexOfFirst { it.contains(currentModel, ignoreCase = true) }
                        }
                        if (targetIndex >= 0) {
                            modelListState.animateScrollToItem(targetIndex)
                        }
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = modelListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    items(settingsViewModel.availableModels.size) { index ->
                        val model = settingsViewModel.availableModels[index]
                        val isSelected = model == settingsViewModel.modelName
                        val isMatch = settingsViewModel.modelName.isNotBlank() &&
                            model.contains(settingsViewModel.modelName, ignoreCase = true)
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isSelected -> TavernGold
                                isMatch -> TavernPurpleLight
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsViewModel.updateModelName(model) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            if (showSavePresetDialog) {
                AlertDialog(
                    onDismissRequest = { showSavePresetDialog = false },
                    title = { Text("保存 API 预设", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        OutlinedTextField(
                            value = presetNameInput,
                            onValueChange = { presetNameInput = it },
                            label = { Text("预设名称") },
                            placeholder = { Text("例如：默认 GPT-4o") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (presetNameInput.isNotBlank()) {
                                    settingsViewModel.saveCurrentAsPreset(presetNameInput)
                                    presetNameInput = ""
                                    showSavePresetDialog = false
                                }
                            }
                        ) {
                            Text("保存", color = TavernPurpleLight)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSavePresetDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.surfaceElevated
                )
            }
}