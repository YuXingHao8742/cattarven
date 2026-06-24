package cat.tarven.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.tarven.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabScreen(
    labViewModel: cat.tarven.viewmodel.LabViewModel,
    onBack: () -> Unit
) {
    val logFiles by labViewModel.logs.collectAsState()
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String>("") }

    // 每次进入页面刷新
    LaunchedEffect(Unit) {
        labViewModel.loadLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("🧪 实验室 - JSON 日志", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = {
                    labViewModel.clearLogs()
                    selectedFileContent = null
                }) {
                    Icon(Icons.Default.Delete, "清空", tint = ErrorRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        if (logFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("暂无 JSON 日志", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(logFiles) { log ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                    val title = "请求: ${log.characterName}"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                selectedFileName = title
                                selectedFileContent = log.requestJson
                            },
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("时间: $dateStr", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Text("大小: ${log.requestJson.length / 1024} KB", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (selectedFileContent != null) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = LocalContext.current
        
        AlertDialog(
            onDismissRequest = { selectedFileContent = null },
            title = { Text(selectedFileName, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = selectedFileContent!!,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFileContent = null }) {
                    Text("关闭", color = TavernPurpleLight)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedFileContent!!))
                    android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("一键复制", color = TavernPurpleLight)
                }
            },
            containerColor = DarkSurfaceElevated,
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
}
