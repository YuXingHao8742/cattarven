package cat.tarven.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.tarven.data.model.WorldInfo
import cat.tarven.data.model.WorldInfoEntry
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.CharacterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldInfoDialog(
    worldInfo: WorldInfo,
    characterViewModel: CharacterViewModel? = null,
    onDismiss: () -> Unit,
    onUpdate: (WorldInfo) -> Unit
) {
    var editingEntry by remember { mutableStateOf<WorldInfoEntry?>(null) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TavernPurple,
        unfocusedBorderColor = BorderColor,
        focusedLabelColor = TavernPurpleLight,
        unfocusedLabelColor = TextSecondary,
        cursorColor = TavernPurple,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = InputBackground,
        unfocusedContainerColor = InputBackground
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = DarkSurfaceElevated
        ) {
            if (editingEntry != null) {
                // === 编辑/新增词条 ===
                WorldInfoEntryEditor(
                    entry = editingEntry!!,
                    worldInfo = worldInfo,
                    textFieldColors = textFieldColors,
                    onSave = { newEntry, updatedInfo ->
                        val newEntries = updatedInfo.entries.toMutableList()
                        val index = newEntries.indexOfFirst { it.id == newEntry.id }
                        if (index >= 0) newEntries[index] = newEntry else newEntries.add(newEntry)
                        onUpdate(WorldInfo(newEntries))
                        editingEntry = null
                    },
                    onCancel = { editingEntry = null }
                )
            } else {
                // === 列表视图 ===
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📖 世界书", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "关闭", tint = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 导入世界书按钮
                    if (characterViewModel != null) {
                        val worldInfoPicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let {
                                val result = characterViewModel.importWorldInfoFromUri(it)
                                if (result != null) {
                                    onUpdate(WorldInfo(entries = worldInfo.entries + result.entries))
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { worldInfoPicker.launch("application/json") },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TavernPurpleLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TavernPurpleLight)
                        ) {
                            Text("📥 导入世界书文件 (.json)", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(worldInfo.entries) { entry ->
                            WorldInfoListItem(
                                entry = entry,
                                onEdit = { editingEntry = entry },
                                onDelete = { onUpdate(WorldInfo(worldInfo.entries.filter { it.id != entry.id })) }
                            )
                        }
                    }
                    Button(
                        onClick = { editingEntry = WorldInfoEntry() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TavernPurple, contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                        Text("添加词条")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldInfoListItem(
    entry: WorldInfoEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tagLabel = when (entry.tag) {
        "main_setting" -> "📌 主要设定"
        "writing_rules" -> "✍️ 写作规则"
        else -> null
    }
    val roleLabel = when (entry.role) {
        "user" -> "👤"
        "assistant" -> "🤖"
        else -> "⚙️"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(roleLabel, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    if (entry.comment.isNotBlank()) {
                        Text(entry.comment, style = MaterialTheme.typography.labelLarge, color = TavernGold, maxLines = 1)
                    } else if (entry.keys.isNotEmpty()) {
                        Text(entry.keys.joinToString(", "), style = MaterialTheme.typography.labelLarge, color = TavernGold, maxLines = 1)
                    } else {
                        Text("(无关键字)", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    }
                }
                if (tagLabel != null) {
                    Text(tagLabel, style = MaterialTheme.typography.labelSmall, color = TavernPurpleLight)
                }
                Text(
                    entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "编辑", tint = TextPrimary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = ErrorRed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldInfoEntryEditor(
    entry: WorldInfoEntry,
    worldInfo: WorldInfo,
    textFieldColors: TextFieldColors,
    onSave: (WorldInfoEntry, WorldInfo) -> Unit,
    onCancel: () -> Unit
) {
    var keysText by remember { mutableStateOf(entry.keys.joinToString(",")) }
    var commentText by remember { mutableStateOf(entry.comment) }
    var contentText by remember { mutableStateOf(entry.content) }
    var roleState by remember { mutableStateOf(entry.role) }
    var tagState by remember { mutableStateOf(entry.tag) }
    var relativePositionState by remember { mutableStateOf(entry.relativePosition) }
    var insertionOrderText by remember { mutableStateOf(entry.insertionOrder.toString()) }
    var constantState by remember { mutableStateOf(entry.constant) }
    var disableState by remember { mutableStateOf(entry.disable) }

    // 下拉状态
    var tagExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    var posExpanded by remember { mutableStateOf(false) }

    val tags = listOf(
        "normal" to "普通条目",
        "main_setting" to "📌 主要设定",
        "writing_rules" to "✍️ 写作规则"
    )
    val roles = listOf(
        "system" to "⚙️ 系统 (System)",
        "user" to "👤 玩家 (User)",
        "assistant" to "🤖 AI (Assistant)"
    )
    val positions = listOf(
        "before_main" to "主要设定前",
        "after_main" to "主要设定后"
    )

    // 检查标签冲突
    val tagConflict = when (tagState) {
        "main_setting" -> worldInfo.entries.any { it.tag == "main_setting" && it.id != entry.id }
        "writing_rules" -> worldInfo.entries.any { it.tag == "writing_rules" && it.id != entry.id }
        else -> false
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("编辑世界书词条", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // 备注名
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            label = { Text("备注名（方便识别）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 关键字
        OutlinedTextField(
            value = keysText,
            onValueChange = { keysText = it },
            label = { Text("关键字 (用逗号分隔，常驻条目可留空)") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 内容
        OutlinedTextField(
            value = contentText,
            onValueChange = { contentText = it },
            label = { Text("内容") },
            minLines = 4,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // === 标签选择 ===
        Text("标签类型", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = tagExpanded, onExpandedChange = { tagExpanded = it }) {
            OutlinedTextField(
                value = tags.firstOrNull { it.first == tagState }?.second ?: "普通条目",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = textFieldColors
            )
            ExposedDropdownMenu(
                expanded = tagExpanded,
                onDismissRequest = { tagExpanded = false },
                modifier = Modifier.background(DarkSurfaceElevated)
            ) {
                tags.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = TextPrimary) },
                        onClick = { tagState = value; tagExpanded = false }
                    )
                }
            }
        }
        if (tagConflict) {
            Text(
                "⚠️ 已有其他条目使用此标签，保存后将替换",
                style = MaterialTheme.typography.labelSmall,
                color = TavernGold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === 相对位置（仅普通条目显示） ===
        if (tagState == "normal") {
            Text("相对位置", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(expanded = posExpanded, onExpandedChange = { posExpanded = it }) {
                OutlinedTextField(
                    value = positions.firstOrNull { it.first == relativePositionState }?.second ?: "主要设定后",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = posExpanded,
                    onDismissRequest = { posExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    positions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = TextPrimary) },
                            onClick = { relativePositionState = value; posExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // === 消息身份 (Role) ===
        Text("消息身份", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
            OutlinedTextField(
                value = roles.firstOrNull { it.first == roleState }?.second ?: "⚙️ 系统 (System)",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = textFieldColors
            )
            ExposedDropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false },
                modifier = Modifier.background(DarkSurfaceElevated)
            ) {
                roles.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = TextPrimary) },
                        onClick = { roleState = value; roleExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === 插入顺序 + 开关 ===
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = insertionOrderText,
                onValueChange = { insertionOrderText = it.filter { c -> c.isDigit() } },
                label = { Text("排序序号") },
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("常驻", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Switch(
                    checked = constantState,
                    onCheckedChange = { constantState = it }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("启用", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Switch(
                    checked = !disableState,
                    onCheckedChange = { disableState = !it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 操作按钮 ===
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("取消", color = TextSecondary) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    // 如果设置了唯一标签，先清除同标签的其他条目
                    var updatedWorldInfo = worldInfo
                    if (tagState == "main_setting" || tagState == "writing_rules") {
                        updatedWorldInfo = WorldInfo(
                            worldInfo.entries.map {
                                if (it.id != entry.id && it.tag == tagState) it.copy(tag = "normal") else it
                            }
                        )
                    }

                    val newEntry = entry.copy(
                        keys = keysText.split(",", "，", "、").map { it.trim() }.filter { it.isNotEmpty() },
                        comment = commentText,
                        content = contentText,
                        role = roleState,
                        tag = tagState,
                        relativePosition = relativePositionState,
                        insertionOrder = insertionOrderText.toIntOrNull() ?: 0,
                        constant = constantState,
                        disable = disableState
                    )
                    onSave(newEntry, updatedWorldInfo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TavernPurple, contentColor = TextPrimary)
            ) { Text("保存") }
        }
    }
}
