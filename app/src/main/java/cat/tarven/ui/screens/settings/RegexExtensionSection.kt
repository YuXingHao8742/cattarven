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
fun RegexExtensionSection(settingsViewModel: SettingsViewModel, textFieldColors: androidx.compose.material3.TextFieldColors) {
    var showRegexDialog by remember { mutableStateOf(false) }
    var editingRegexRule by remember { mutableStateOf<cat.tarven.data.model.RegexRule?>(null) }
    var regexNameInput by remember { mutableStateOf("") }
    var regexPatternInput by remember { mutableStateOf("") }
    var regexReplacementInput by remember { mutableStateOf("") }

// === 正则脚本扩展 ===
            SectionHeader("正则脚本扩展 (Regex Extension)")
            
            Text(
                text = "自定义正则表达式来替换、过滤显示文本。这些规则会在展示前处理内容。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            settingsViewModel.regexRules.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = rule.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(text = "Find: ${rule.pattern}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.textMuted)
                        Text(text = "Replace: ${rule.replacement}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.textMuted)
                    }
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { settingsViewModel.toggleRegexRule(rule.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TavernGold,
                            checkedTrackColor = TavernGoldDark.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.textMuted,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    IconButton(onClick = { 
                        editingRegexRule = rule
                        regexNameInput = rule.name
                        regexPatternInput = rule.pattern
                        regexReplacementInput = rule.replacement
                        showRegexDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { settingsViewModel.deleteRegexRule(rule.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { 
                    editingRegexRule = null
                    regexNameInput = ""
                    regexPatternInput = ""
                    regexReplacementInput = ""
                    showRegexDialog = true 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TavernPurpleLight),
                border = BorderStroke(1.dp, TavernPurpleDark)
            ) {
                Text("+ 添加新规则")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            if (showRegexDialog) {
                AlertDialog(
                    onDismissRequest = { showRegexDialog = false },
                    title = { Text(if (editingRegexRule == null) "添加正则脚本" else "编辑正则脚本", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = regexNameInput,
                                onValueChange = { regexNameInput = it },
                                label = { Text("规则名称") },
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = regexPatternInput,
                                onValueChange = { regexPatternInput = it },
                                label = { Text("匹配模式 (Regex)") },
                                placeholder = { Text("如：\\*(.*?)\\*") },
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = regexReplacementInput,
                                onValueChange = { regexReplacementInput = it },
                                label = { Text("替换内容 (可留空删除)") },
                                placeholder = { Text("如：<i>$1</i>") },
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (regexNameInput.isNotBlank() && regexPatternInput.isNotBlank()) {
                                    val newRule = cat.tarven.data.model.RegexRule(
                                        id = editingRegexRule?.id ?: java.util.UUID.randomUUID().toString(),
                                        name = regexNameInput,
                                        pattern = regexPatternInput,
                                        replacement = regexReplacementInput,
                                        isEnabled = editingRegexRule?.isEnabled ?: true
                                    )
                                    if (editingRegexRule == null) {
                                        settingsViewModel.addRegexRule(newRule)
                                    } else {
                                        settingsViewModel.updateRegexRule(newRule)
                                    }
                                    showRegexDialog = false
                                }
                            }
                        ) {
                            Text("保存", color = TavernPurpleLight)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRegexDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.surfaceElevated
                )
            }
}