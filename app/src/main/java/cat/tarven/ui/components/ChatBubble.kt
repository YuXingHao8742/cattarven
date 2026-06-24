package cat.tarven.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.toArgb
import android.widget.TextView
import android.text.method.LinkMovementMethod
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.MessageRole
import cat.tarven.ui.theme.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    characterAvatarUri: String? = null,
    enableHtmlRendering: Boolean = false,
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onSwitchGreeting: ((Int) -> Unit)? = null,
    isLastAssistant: Boolean = false,
    itemHeights: androidx.compose.runtime.snapshots.SnapshotStateMap<String, androidx.compose.ui.unit.Dp>? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val isSystem = message.role == MessageRole.SYSTEM
    val clipboardManager = LocalClipboardManager.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // AI 头像
        if (!isUser && !isSystem) {
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
                if (characterAvatarUri != null) {
                    coil.compose.AsyncImage(
                        model = "file://$characterAvatarUri",
                        contentDescription = "Avatar",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (message.name?.firstOrNull() ?: 'A').uppercase(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = screenWidth * 0.78f)
        ) {
            // 角色名称
            if (!isUser && !isSystem && message.name != null) {
                Text(
                    text = message.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TavernPurpleLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            val density = androidx.compose.ui.platform.LocalDensity.current
            val cachedHeight = itemHeights?.get(message.id)

            // 消息气泡
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(
                        when {
                            isUser -> UserBubble
                            isSystem -> SystemBubble
                            else -> AssistantBubble
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isUser -> UserBubbleBorder
                            isSystem -> SystemBubbleBorder
                            else -> AssistantBubbleBorder
                        },
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .then(
                        if (cachedHeight != null) Modifier.heightIn(min = cachedHeight) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .onSizeChanged { size ->
                        if (size.height > 0) {
                            val heightDp = with(density) { size.height.toDp() }
                            // 如果高度变化超过 2dp，再更新缓存，避免频繁重组
                            val oldHeight = itemHeights?.get(message.id)?.value ?: 0f
                            if (kotlin.math.abs(oldHeight - heightDp.value) > 2f) {
                                itemHeights?.put(message.id, heightDp)
                            }
                        }
                    }
            ) {
                if (isSystem) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    HtmlText(
                        text = message.content,
                        isStreaming = message.isStreaming,
                        color = TextPrimary.toArgb()
                    )
                }

                // 多版本切换器（Swipes 或 旧版开场白）
                val totalVersions = if (message.swipes.isNotEmpty()) message.swipes.size else message.alternateGreetings.size
                val currentVersionIdx = if (message.swipes.isNotEmpty()) message.currentSwipeIndex else message.currentGreetingIndex

                if (totalVersions > 1 && onSwitchGreeting != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                val newIdx = if (currentVersionIdx > 0) currentVersionIdx - 1 else totalVersions - 1
                                onSwitchGreeting(newIdx) 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一条", tint = TextMuted)
                        }
                        
                        Text(
                            text = "${currentVersionIdx + 1} / $totalVersions",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { 
                                val newIdx = if (currentVersionIdx < totalVersions - 1) currentVersionIdx + 1 else 0
                                onSwitchGreeting(newIdx) 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一条", tint = TextMuted)
                        }
                    }
                }

            }

            // 时间戳与操作栏
            if (!isSystem) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    // 操作图标组
                    IconButton(onClick = { onEdit(message.content) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(message.content)) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                    if (isLastAssistant && !isUser) {
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "重新生成", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 用户头像
        if (isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TavernPurpleDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (message.name?.firstOrNull() ?: 'U').uppercase(),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 已移除 SimpleMarkdownText 及其依赖的变量

@Composable
fun HtmlText(
    text: String,
    isStreaming: Boolean = false,
    color: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // 使用 AndroidView 嵌入 WebView
    androidx.compose.ui.viewinterop.AndroidView<android.webkit.WebView>(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                
                // 禁用长按选词的放大镜，使用自定义样式
                isLongClickable = false
                
                // 彻底禁用 WebView 内部滚动，交由 Compose 的 LazyColumn 处理
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isFocusable = false
                isFocusableInTouchMode = false

                // 核心修复：强制释放滑动控制权给外层的 LazyColumn
                setOnTouchListener { view, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN || 
                        event.action == android.view.MotionEvent.ACTION_MOVE) {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }

                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // 注入 Javascript 接口
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun openLink(url: String) {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, "Android")

                // 拦截页面内的链接点击及处理加载完成事件
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val url = request?.url?.toString()
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 页面加载完成后，提取保存的最新文本并渲染
                        val tagData = view?.tag as? Pair<*, *>
                        val b64 = tagData?.first as? String ?: return
                        val streaming = tagData?.second as? Boolean ?: false
                        
                        view.evaluateJavascript("javascript:if(typeof setContent === 'function') { setContent('$b64', $streaming); }") {
                            view.requestLayout()
                        }
                    }
                }

                // 加载本地模板
                loadUrl("file:///android_asset/chat/chat_template.html")
            }
        },
        update = { webView ->
            // 将文本进行 Base64 编码，防止引号/换行等破坏 JS 语法
            val base64Text = android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.NO_WRAP)
            
            // 将最新数据保存在 tag 中，供 onPageFinished 使用（防止首次加载太慢导致文本丢失）
            webView.tag = Pair(base64Text, isStreaming)
            
            // 尝试直接执行 JS（如果模板已经加载完毕）
            webView.evaluateJavascript("javascript:if(typeof setContent === 'function') { setContent('$base64Text', $isStreaming); }") {
                webView.requestLayout()
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
