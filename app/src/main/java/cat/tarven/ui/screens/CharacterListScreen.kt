package cat.tarven.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.tarven.data.model.Character
import cat.tarven.ui.components.CharacterCard
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.CharacterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    characterViewModel: CharacterViewModel,
    onConversationClick: (cat.tarven.data.model.ConversationWithCharacter) -> Unit,
    onCreateCharacter: () -> Unit,
    onSettings: () -> Unit,
    onLabClick: () -> Unit
) {
    val conversations = characterViewModel.filteredConversations
    var showSearch by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Character?>(null) }
    var showFab by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 文件选择器
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { characterViewModel.importCharacterFromUri(it) }
    }

    // 错误提示
    LaunchedEffect(characterViewModel.errorMessage) {
        characterViewModel.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                characterViewModel.clearError()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = characterViewModel.searchQuery,
                            onValueChange = { characterViewModel.updateSearchQuery(it) },
                            placeholder = { Text("搜索角色...", color = MaterialTheme.textMuted) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = TavernPurple,
                                focusedIndicatorColor = TavernPurple,
                                unfocusedIndicatorColor = DividerColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🏰",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "CatTavern",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLabClick) {
                        Text("🧪", fontSize = 20.sp)
                    }
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) characterViewModel.updateSearchQuery("")
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 角色列表
            if (conversations.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(text = "🎭", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "还没有角色",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击下方按钮创建新角色\n或导入角色卡文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 48.dp),
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = conversations,
                        key = { it.conversation.id } // 使用 conversation.id 作为 key
                    ) { convWithChar ->
                        cat.tarven.ui.components.ConversationCard(
                            conversation = convWithChar.conversation,
                            character = convWithChar.character,
                            onClick = { onConversationClick(convWithChar) },
                            onLongClick = { showDeleteDialog = convWithChar.character }, // 暂时保留长按删除整个角色
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // 删除确认对话框
        showDeleteDialog?.let { charToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除角色", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("确定要删除角色「${charToDelete.name}」及其所有聊天记录吗？此操作不可恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    TextButton(onClick = {
                        characterViewModel.deleteCharacter(charToDelete.id)
                        showDeleteDialog = null
                    }) {
                        Text("删除", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("取消", color = MaterialTheme.textMuted)
                    }
                }
            )
        }

        // FAB 按钮组
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = showFab) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            filePicker.launch("*/*")
                            showFab = false
                        },
                        containerColor = TavernGoldDark,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "导入")
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            onCreateCharacter()
                            showFab = false
                        },
                        containerColor = TavernPurpleDark,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "创建")
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFab = !showFab },
                containerColor = TavernPurple,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    if (showFab) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "添加角色",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.surfaceElevated,
                contentColor = ErrorRed,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
