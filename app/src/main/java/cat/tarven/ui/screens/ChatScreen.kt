package cat.tarven.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.tarven.data.model.MessageRole
import cat.tarven.ui.components.ChatBubble
import cat.tarven.ui.components.ChatInput
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.ChatViewModel
import cat.tarven.viewmodel.SettingsViewModel
import cat.tarven.viewmodel.CharacterViewModel
import cat.tarven.data.model.WorldInfo
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
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showWorldInfo by remember { mutableStateOf(false) }
    var worldInfo by remember(character) { mutableStateOf(character?.worldInfo ?: WorldInfo()) }

    // 每次进入页面时刷新角色数据，确保获取到最新的世界书等修改
    androidx.compose.runtime.LaunchedEffect(Unit) {
        chatViewModel.reloadCharacter()
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 顶部栏
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
                                color = TextPrimary,
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
                            color = TextPrimary,
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
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                // 世界书入口
                IconButton(onClick = { showWorldInfo = true }) {
                    Text("📖", fontSize = 20.sp)
                }
                IconButton(onClick = onSettings) {
                    Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "设置", tint = TextPrimary)
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "菜单", tint = TextPrimary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("新建对话") },
                        onClick = {
                            chatViewModel.newConversation()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑角色") },
                        onClick = {
                            character?.id?.let { onEditCharacter(it) }
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("重新生成") },
                        onClick = {
                            chatViewModel.regenerateLastResponse()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface,
                titleContentColor = TextPrimary
            )
        )

        // 消息列表
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "开始与 ${character?.name ?: "角色"} 对话",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        val isLastAssistant = messages.lastOrNull { it.role == MessageRole.ASSISTANT } == message

                        ChatBubble(
                            message = message,
                            characterAvatarUri = character?.avatarUri,
                            enableHtmlRendering = settingsViewModel.enableHtmlRendering,
                            isLastAssistant = isLastAssistant,
                            onDelete = { chatViewModel.deleteMessage(message.id) },
                            onEdit = { /* TODO: 弹出编辑对话框 */ },
                            onRegenerate = { chatViewModel.regenerateLastResponse() },
                            onSwitchGreeting = { newIdx -> chatViewModel.switchGreeting(message.id, newIdx) }
                        )
                    }
                }
            }

            // Snackbar 错误提示
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurfaceElevated,
                    contentColor = ErrorRed,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 输入框
        ChatInput(
            onSend = { text -> chatViewModel.sendMessage(text) },
            onStop = { chatViewModel.stopGenerating() },
            onNewConversation = { chatViewModel.newConversation() },
            onRegenerate = { chatViewModel.regenerateLastResponse() },
            isGenerating = chatViewModel.isGenerating
        )
    }

    // 世界书弹窗
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
}
