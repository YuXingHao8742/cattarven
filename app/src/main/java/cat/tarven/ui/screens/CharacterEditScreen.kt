package cat.tarven.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.tarven.data.model.Character
import cat.tarven.data.model.RegexRule
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.CharacterViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    character: Character?,
    characterViewModel: CharacterViewModel,
    settingsViewModel: cat.tarven.viewmodel.SettingsViewModel,
    onBack: () -> Unit
) {
    val isNew = character == null || character.name.isBlank()

    var name by remember { mutableStateOf(character?.name ?: "") }
    var description by remember { mutableStateOf(character?.description ?: "") }
    var firstMessage by remember { mutableStateOf(character?.firstMessage ?: "") }
    var systemPrompt by remember { mutableStateOf(character?.systemPrompt ?: "") }
    var creatorNotes by remember { mutableStateOf(character?.creatorNotes ?: "") }
    var avatarUri by remember { mutableStateOf(character?.avatarUri) }
    var regexRules by remember { mutableStateOf(character?.regexRules ?: emptyList()) }
    var chatBackgroundUri by remember { mutableStateOf(character?.chatBackgroundUri) }
    var chatBackgroundBlurRadius by remember { mutableStateOf(character?.chatBackgroundBlurRadius ?: 10f) }

    var showRegexDialog by remember { mutableStateOf(false) }
    var editingRegexRule by remember { mutableStateOf<RegexRule?>(null) }
    var regexNameInput by remember { mutableStateOf("") }
    var regexPatternInput by remember { mutableStateOf("") }
    var regexReplacementInput by remember { mutableStateOf("") }

    val currentCharacter = (character ?: Character()).copy(
        name = name,
        description = description,
        firstMessage = firstMessage,
        systemPrompt = systemPrompt,
        creatorNotes = creatorNotes,
        avatarUri = avatarUri,
        regexRules = regexRules,
        chatBackgroundUri = chatBackgroundUri,
        chatBackgroundBlurRadius = chatBackgroundBlurRadius
    )

    // 导出状态
    var showExportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 文件保存器 — JSON
    val jsonSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            characterViewModel.exportCharacterAsJson(currentCharacter, it)
        }
    }

    // 文件保存器 — PNG
    val pngSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        uri?.let {
            characterViewModel.exportCharacterAsPng(currentCharacter, it)
        }
    }

    // 监听导出结果
    LaunchedEffect(characterViewModel.exportSuccessMessage) {
        characterViewModel.exportSuccessMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                characterViewModel.clearExportSuccess()
            }
        }
    }

    LaunchedEffect(characterViewModel.errorMessage) {
        characterViewModel.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                characterViewModel.clearError()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = characterViewModel.saveAvatarImage(it)
            if (savedPath != null) {
                avatarUri = savedPath
            }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景渲染逻辑（与 ChatScreen 一致）
        val bgUri = chatBackgroundUri
        val globalBgPath = settingsViewModel.backgroundImagePath
        
        if (!bgUri.isNullOrBlank() || globalBgPath.isNotBlank()) {
            val bgSource = if (!bgUri.isNullOrBlank()) {
                java.io.File(bgUri)
            } else {
                java.io.File(globalBgPath)
            }
            
            if (bgSource.exists()) {
                val blurRadius = if (!bgUri.isNullOrBlank()) chatBackgroundBlurRadius else settingsViewModel.backgroundBlurRadius
                Image(
                    painter = coil.compose.rememberAsyncImagePainter(bgSource),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (settingsViewModel.isDarkMode)
                                Color.Black.copy(alpha = 0.55f)
                            else
                                Color.White.copy(alpha = 0.55f)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                
        ) {
            TopAppBar(
            title = {
                Text(
                    text = if (isNew) "创建角色" else "编辑角色",
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
            actions = {
                if (!isNew) {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "导出角色卡",
                            tint = TavernGold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 头像上传
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = "file://$avatarUri",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = "上传头像", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("点击上传", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 角色名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("角色名称 *") },
                placeholder = { Text("例：猫猫酒馆老板", color = MaterialTheme.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 角色描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("角色描述") },
                placeholder = { Text("描述角色的外貌、身份、背景故事等", color = MaterialTheme.textMuted) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 开场白
            OutlinedTextField(
                value = firstMessage,
                onValueChange = { firstMessage = it },
                label = { Text("开场白") },
                placeholder = { Text("角色的第一条消息，支持 {{user}} 和 {{char}}", color = MaterialTheme.textMuted) },
                minLines = 3,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 角色系统提示词
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("角色系统提示词（可选）") },
                placeholder = { Text("角色专属的系统提示词，追加到全局提示词之后", color = MaterialTheme.textMuted) },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 创作者备注
            OutlinedTextField(
                value = creatorNotes,
                onValueChange = { creatorNotes = it },
                label = { Text("创作者备注（可选）") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            // === 角色专属正则扩展 ===
            Text(
                text = "角色专属正则脚本",
                style = MaterialTheme.typography.titleMedium,
                color = TavernPurpleLight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "导入角色卡时会自动识别内嵌的正则规则。您也可以手动添加。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            regexRules.forEach { rule ->
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
                        Text(text = "Find: ${rule.pattern.take(60)}${if (rule.pattern.length > 60) "..." else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.textMuted)
                    }
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { enabled ->
                            regexRules = regexRules.map { if (it.id == rule.id) it.copy(isEnabled = enabled) else it }
                        },
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
                    IconButton(onClick = {
                        regexRules = regexRules.filter { it.id != rule.id }
                    }) {
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
                border = androidx.compose.foundation.BorderStroke(1.dp, TavernPurpleDark)
            ) {
                Text("+ 添加新规则")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            // === 对话背景设置 ===
            Text(
                text = "对话背景",
                style = MaterialTheme.typography.titleMedium,
                color = TavernPurpleLight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "为该角色的对话界面设置专属背景图片。未设置时将使用全局背景。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val chatBgPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    val savedPath = characterViewModel.saveChatBackgroundImage(it)
                    if (savedPath != null) {
                        chatBackgroundUri = savedPath
                    }
                }
            }

            if (chatBackgroundUri != null) {
                // 背景预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { chatBgPickerLauncher.launch(arrayOf("image/*")) }
                ) {
                    AsyncImage(
                        model = "file://${chatBackgroundUri}",
                        contentDescription = "对话背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 模糊度调节
                Text(
                    text = "🔮 模糊度: ${chatBackgroundBlurRadius.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.Slider(
                    value = chatBackgroundBlurRadius,
                    onValueChange = { chatBackgroundBlurRadius = it },
                    valueRange = 0f..25f,
                    steps = 24,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = TavernGold,
                        activeTrackColor = TavernPurple,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                // 清除按钮
                OutlinedButton(
                    onClick = {
                        // 删除旧文件
                        chatBackgroundUri?.let { path ->
                            try { java.io.File(path).delete() } catch (_: Exception) {}
                        }
                        chatBackgroundUri = null
                        chatBackgroundBlurRadius = 10f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("清除对话背景")
                }
            } else {
                OutlinedButton(
                    onClick = { chatBgPickerLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TavernPurpleLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TavernPurpleDark)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("选择对话背景图片")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 保存按钮
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    characterViewModel.saveCharacter(currentCharacter.copy(updatedAt = System.currentTimeMillis()))
                    onBack()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TavernPurple,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = TavernPurpleDark.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = if (isNew) "创建角色" else "保存修改",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        } // 结束 Column

        // Snackbar 提示
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.surfaceElevated,
                contentColor = TavernGold,
                shape = RoundedCornerShape(12.dp)
            )
        }
    } // 结束 Box

    // 导出格式选择对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    "导出角色卡",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "选择导出格式。导出的角色卡将包含您当前的修改（世界书条目、开场白、正则规则等）。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        showExportDialog = false
                        val fileName = "${name.ifBlank { "character" }}.json"
                        jsonSaver.launch(fileName)
                    }) {
                        Text("📄  导出为 JSON", color = TavernPurpleLight)
                    }
                    TextButton(onClick = {
                        showExportDialog = false
                        val fileName = "${name.ifBlank { "character" }}.png"
                        pngSaver.launch(fileName)
                    }) {
                        Text("🖼️  导出为 PNG", color = TavernGold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
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
                                regexRules = regexRules + RegexRule(
                                    name = regexNameInput,
                                    pattern = regexPatternInput,
                                    replacement = regexReplacementInput
                                )
                            } else {
                                regexRules = regexRules.map {
                                    if (it.id == editingRegexRule!!.id) it.copy(
                                        name = regexNameInput,
                                        pattern = regexPatternInput,
                                        replacement = regexReplacementInput
                                    ) else it
                                }
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
}
