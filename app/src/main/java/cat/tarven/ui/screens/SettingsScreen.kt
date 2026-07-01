package cat.tarven.ui.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    var showRegexDialog by remember { mutableStateOf(false) }
    var editingRegexRule by remember { mutableStateOf<cat.tarven.data.model.RegexRule?>(null) }
    var regexNameInput by remember { mutableStateOf("") }
    var regexPatternInput by remember { mutableStateOf("") }
    var regexReplacementInput by remember { mutableStateOf("") }

    var showPropDialog by remember { mutableStateOf(false) }
    var editingProp by remember { mutableStateOf<cat.tarven.data.repository.PropItem?>(null) }
    var propNameInput by remember { mutableStateOf("") }
    var propContentInput by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TavernPurple,
        unfocusedBorderColor = BorderColor,
        focusedLabelColor = TavernPurpleLight,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = TavernPurple,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.inputBackground,
        unfocusedContainerColor = MaterialTheme.inputBackground
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "⚙️ 设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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

            // === 用户设置 ===
            SectionHeader("用户设置")

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

            // === 快捷道具管理 ===
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("保存 API 预设", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("预设名称") },
                    placeholder = { Text("例如：本地-Qwen", color = MaterialTheme.textMuted) },
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
                            showSavePresetDialog = false
                            presetNameInput = ""
                        }
                    }
                ) {
                    Text("保存", color = TavernPurpleLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("取消", color = MaterialTheme.textMuted)
                }
            },
            containerColor = MaterialTheme.surfaceElevated,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showRegexDialog) {
        AlertDialog(
            onDismissRequest = { showRegexDialog = false },
            title = { Text(if (editingRegexRule == null) "添加正则规则" else "编辑正则规则", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = regexNameInput,
                        onValueChange = { regexNameInput = it },
                        label = { Text("规则名称") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = regexPatternInput,
                        onValueChange = { regexPatternInput = it },
                        label = { Text("正则表达式 (Pattern)") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = regexReplacementInput,
                        onValueChange = { regexReplacementInput = it },
                        label = { Text("替换为 (Replacement)") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (regexPatternInput.isNotBlank()) {
                            if (editingRegexRule == null) {
                                settingsViewModel.addRegexRule(
                                    cat.tarven.data.model.RegexRule(
                                        name = regexNameInput,
                                        pattern = regexPatternInput,
                                        replacement = regexReplacementInput
                                    )
                                )
                            } else {
                                settingsViewModel.updateRegexRule(
                                    editingRegexRule!!.copy(
                                        name = regexNameInput,
                                        pattern = regexPatternInput,
                                        replacement = regexReplacementInput
                                    )
                                )
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
                    Text("取消", color = MaterialTheme.textMuted)
                }
            },
            containerColor = MaterialTheme.surfaceElevated,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showPropDialog) {
        AlertDialog(
            onDismissRequest = { showPropDialog = false },
            title = { Text(if (editingProp == null) "添加新道具" else "编辑道具", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = propNameInput,
                        onValueChange = { propNameInput = it },
                        label = { Text("道具名称") },
                        placeholder = { Text("例如：总结", color = MaterialTheme.textMuted) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = propContentInput,
                        onValueChange = { propContentInput = it },
                        label = { Text("道具发送内容") },
                        placeholder = { Text("例如：请总结之前的剧情...", color = MaterialTheme.textMuted) },
                        minLines = 3,
                        maxLines = 8,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (propNameInput.isNotBlank() && propContentInput.isNotBlank()) {
                            if (editingProp == null) {
                                settingsViewModel.savePropItem(
                                    cat.tarven.data.repository.PropItem(
                                        name = propNameInput,
                                        content = propContentInput
                                    )
                                )
                            } else {
                                settingsViewModel.savePropItem(
                                    editingProp!!.copy(
                                        name = propNameInput,
                                        content = propContentInput
                                    )
                                )
                            }
                            showPropDialog = false
                        }
                    }
                ) {
                    Text("保存", color = TavernPurpleLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPropDialog = false }) {
                    Text("取消", color = MaterialTheme.textMuted)
                }
            },
            containerColor = MaterialTheme.surfaceElevated,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TavernPurpleLight,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelLarge,
                color = TavernGold,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = TavernPurple,
                activeTrackColor = TavernPurple,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
