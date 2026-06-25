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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableIntStateOf
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
    regexRules: List<cat.tarven.data.model.RegexRule> = emptyList(),
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onSwitchGreeting: ((Int) -> Unit)? = null,
    isLastAssistant: Boolean = false,
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
                .widthIn(max = screenWidth * 0.78f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val processedContent = remember(message.displayContent, regexRules) {
                    var text = message.displayContent
                    regexRules.filter { it.isEnabled }.forEach { rule ->
                        try {
                            val regex = Regex(rule.pattern, RegexOption.DOT_MATCHES_ALL)
                            text = text.replace(regex, rule.replacement)
                        } catch (e: Exception) {
                            // Ignore invalid regex
                        }
                    }
                    text
                }

                if (isSystem) {
                    Text(
                        text = processedContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontStyle = FontStyle.Italic
                    )
                } else if (enableHtmlRendering && !isSystem) {
                    HtmlText(
                        html = processedContent,
                        textColor = TextPrimary
                    )
                } else {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = processedContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
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
                    IconButton(onClick = { onEdit(message.displayContent) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(message.displayContent)) }, modifier = Modifier.size(24.dp)) {
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
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun HtmlText(html: String, textColor: androidx.compose.ui.graphics.Color) {
    val htmlColor = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                setBackgroundColor(0)
                settings.javaScriptEnabled = false
            }
        },
        update = { webView ->
            val styledHtml = """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            color: $htmlColor;
                            background-color: transparent;
                            font-family: sans-serif;
                            line-height: 1.5;
                            margin: 0;
                            padding: 0;
                            word-wrap: break-word;
                            font-size: 15px;
                        }
                        * { max-width: 100%; }
                        pre { background: rgba(0,0,0,0.1); padding: 8px; border-radius: 4px; overflow-x: auto; }
                        code { font-family: monospace; }
                        details { margin-bottom: 8px; }
                        summary { cursor: pointer; font-weight: bold; }
                    </style>
                </head>
                <body>$html</body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    )
}
