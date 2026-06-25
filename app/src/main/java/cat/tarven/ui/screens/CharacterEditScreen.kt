package cat.tarven.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.tarven.data.model.Character
import cat.tarven.data.model.RegexRule
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.CharacterViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    character: Character?,
    characterViewModel: CharacterViewModel,
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

    var showRegexDialog by remember { mutableStateOf(false) }
    var editingRegexRule by remember { mutableStateOf<RegexRule?>(null) }
    var regexNameInput by remember { mutableStateOf("") }
    var regexPatternInput by remember { mutableStateOf("") }
    var regexReplacementInput by remember { mutableStateOf("") }

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
        }

        // 保存按钮
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val updatedCharacter = (character ?: Character()).copy(
                        name = name,
                        description = description,
                        firstMessage = firstMessage,
                        systemPrompt = systemPrompt,
                        creatorNotes = creatorNotes,
                        avatarUri = avatarUri,
                        regexRules = regexRules,
                        updatedAt = System.currentTimeMillis()
                    )
                    characterViewModel.saveCharacter(updatedCharacter)
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
