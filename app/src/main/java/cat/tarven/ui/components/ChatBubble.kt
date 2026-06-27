package cat.tarven.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.MessageRole
import cat.tarven.ui.theme.*

/**
 * @deprecated 已废弃。聊天气泡渲染已迁移至 WebView (ChatWebView)。
 * 保留此函数仅用于向后兼容，新代码请勿使用。
 */
@Deprecated(
    message = "Chat bubbles are now rendered inside ChatWebView. Use ChatWebView instead.",
    level = DeprecationLevel.WARNING
)
@Composable
fun ChatBubble(
    message: ChatMessage,
    characterAvatarUri: String? = null,
    enableHtmlRendering: Boolean = false,
    regexRules: List<cat.tarven.data.model.RegexRule> = emptyList(),
    chatFontSize: Float = 15f,
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onReincarnate: (String) -> Unit = {},
    onSwitchGreeting: ((Int) -> Unit)? = null,
    isLastAssistant: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
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
                Text(
                    text = (message.name?.firstOrNull() ?: 'A').uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = screenWidth * 0.78f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUser) MaterialTheme.bubbleUser
                        else MaterialTheme.bubbleAssistant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.displayContent,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = chatFontSize.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
