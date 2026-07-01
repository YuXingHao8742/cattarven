package cat.tarven.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.draw.blur
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import cat.tarven.data.model.Attachment
import cat.tarven.data.model.WorldInfo
import cat.tarven.ui.components.ChatInput
import cat.tarven.ui.components.ChatWebView
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.ChatViewModel
import cat.tarven.viewmodel.SettingsViewModel
import cat.tarven.viewmodel.CharacterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    characterViewModel: CharacterViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onEditCharacter: (String) -> Unit
) {
    val character = chatViewModel.currentCharacter
    val messages = chatViewModel.messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 文件选择器
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val isImage = mimeType.startsWith("image/")

            // 限制最多 3 张图片
            if (isImage) {
                val currentImageCount = chatViewModel.pendingAttachments.count { it.type == "image" }
                if (currentImageCount >= 3) return@rememberLauncherForActivityResult
            }

            // 获取文件名
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) it.getString(idx) else null
                } else null
            } ?: "file_${System.currentTimeMillis()}"

            // 复制文件到应用内部缓存
            val cacheDir = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
            val ext = if (isImage) mimeType.substringAfter("/", "png") else "txt"
            val cachedFile = java.io.File(cacheDir, "att_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cachedFile.outputStream().use { output -> input.copyTo(output) }
            }

            val attachment = Attachment(
                type = if (isImage) "image" else "text",
                fileName = fileName,
                filePath = cachedFile.absolutePath,
                mimeType = mimeType
            )
            chatViewModel.pendingAttachments.add(attachment)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("读取文件失败: ${e.message}")
            }
        }
    }

    var showWorldInfo by remember { mutableStateOf(false) }
    var worldInfo by remember(character) { mutableStateOf(character?.worldInfo ?: WorldInfo()) }

    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editingMessageContent by remember { mutableStateOf("") }

    // "回到底部"按钮状态 — 由 WebView 的 JS 回调控制
    var showScrollToBottom by remember { mutableStateOf(false) }

    // 每次进入页面时刷新角色数据，确保获取到最新的世界书等修改
    LaunchedEffect(Unit) {
        chatViewModel.reloadCharacter()
    }

    // 错误提示
    LaunchedEffect(chatViewModel.errorMessage) {
        chatViewModel.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                chatViewModel.clearError()
            }
        }
    }

    // 背景处理：优先角色卡背景，其次全局背景
    val charBg = character?.chatBackgroundUri
    val charBgBlur = character?.chatBackgroundBlurRadius ?: 10f
    val globalBg = settingsViewModel.backgroundImagePath
    val globalBgBlur = settingsViewModel.backgroundBlurRadius
    val effectiveBg = if (!charBg.isNullOrBlank()) charBg else globalBg.takeIf { it.isNotBlank() }
    val effectiveBgBlur = if (!charBg.isNullOrBlank()) charBgBlur else globalBgBlur

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片层
        if (effectiveBg != null) {
            val bgFile = java.io.File(effectiveBg)
            if (bgFile.exists()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(bgFile),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(effectiveBgBlur.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (settingsViewModel.isDarkMode)
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
                            else
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f)
                        )
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // 顶部栏 — 与原版完全相同
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(TavernPurple, TavernGold)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (character?.avatarUri != null) {
                            coil.compose.AsyncImage(
                                model = "file://${character.avatarUri}",
                                contentDescription = "Avatar",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (character?.name?.firstOrNull() ?: '?').uppercase(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = character?.name ?: "聊天",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (chatViewModel.isGenerating) {
                            Text(
                                text = "正在输入...",
                                style = MaterialTheme.typography.labelSmall,
                                color = TavernGold
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // 世界书入口
                IconButton(onClick = { showWorldInfo = true }) {
                    Text("📖", fontSize = 20.sp)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {
                    character?.id?.let { onEditCharacter(it) }
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑角色", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // ===== 核心改造：单一 WebView 替代 LazyColumn =====
        Box(modifier = Modifier.weight(1f)) {
            ChatWebView(
                messages = messages.toList(), // 创建快照
                isDarkMode = settingsViewModel.isDarkMode,
                chatFontSize = settingsViewModel.chatFontSize,
                enableHtmlRendering = settingsViewModel.enableHtmlRendering,
                regexRules = settingsViewModel.regexRules + (character?.regexRules ?: emptyList()),
                characterName = character?.name,
                characterAvatarUri = character?.avatarUri,
                userAvatarUri = settingsViewModel.userAvatarPath.takeIf { it.isNotBlank() },
                isGenerating = chatViewModel.isGenerating,
                onDeleteMessage = { chatViewModel.deleteMessage(it) },
                onRequestEditDialog = { id, content ->
                    editingMessageId = id
                    editingMessageContent = content
                },
                onRegenerateMessage = { chatViewModel.regenerateMessage(it) },
                onReincarnate = { content -> chatViewModel.reincarnate(content) },
                onSwitchSwipe = { id, idx -> chatViewModel.switchSwipe(id, idx) },
                onCopyText = { clipboardManager.setText(AnnotatedString(it)) },
                onFormSubmit = { json -> chatViewModel.sendMessage(json) },
                onScrollStateChanged = { showScrollToBottom = it },
                editVersion = chatViewModel.editVersion,
                modifier = Modifier.fillMaxSize()
            )

            // Snackbar 错误提示
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.surfaceElevated,
                    contentColor = ErrorRed,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 到达底部按钮（FAB）— 浮在 WebView 之上
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            // 不再需要操作 LazyListState
                            // WebView 内部的 JS 会处理滚动
                            showScrollToBottom = false
                        },
                        containerColor = TavernPurple,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to Bottom")
                    }
                }
            }
        }

        // 输入框
        ChatInput(
            text = chatViewModel.chatInputText,
            onTextChange = { chatViewModel.chatInputText = it },
            onSend = { text ->
                val attachmentsCopy = chatViewModel.pendingAttachments.toList()
                chatViewModel.sendMessage(text, attachments = attachmentsCopy)
                chatViewModel.pendingAttachments.clear()
            },
            onSendProp = { prop -> chatViewModel.sendMessage(prop.content, propName = prop.name) },
            onStop = { chatViewModel.stopGenerating() },
            onNewConversation = { chatViewModel.newConversation() },
            onPickFile = { filePicker.launch("*/*") },
            isGenerating = chatViewModel.isGenerating,
            props = settingsViewModel.propItems,
            attachments = chatViewModel.pendingAttachments.toList(),
            onRemoveAttachment = { index -> chatViewModel.pendingAttachments.removeAt(index) }
        )
    }
    } // end Box

    // 世界书弹窗 — 保持不变
    if (showWorldInfo) {
        WorldInfoDialog(
            worldInfo = worldInfo,
            characterViewModel = characterViewModel,
            onDismiss = { showWorldInfo = false },
            onUpdate = { updated ->
                worldInfo = updated
                chatViewModel.saveWorldInfo(updated)
            }
        )
    }

    // 编辑消息弹窗 — 保持不变
    if (editingMessageId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingMessageId = null },
            title = { Text("编辑消息") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = editingMessageContent,
                    onValueChange = { editingMessageContent = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 300.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.surfaceElevated,
                        unfocusedContainerColor = MaterialTheme.surfaceElevated,
                        focusedBorderColor = TavernPurple,
                        unfocusedBorderColor = TavernPurpleLight.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        chatViewModel.editMessage(editingMessageId!!, editingMessageContent)
                        editingMessageId = null
                    }
                ) {
                    Text("保存", color = TavernPurpleLight)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editingMessageId = null }) {
                    Text("取消", color = MaterialTheme.textMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}
