package cat.tarven.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import cat.tarven.ui.theme.*

@Composable
fun ChatInput(
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
    onRegenerate: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 新建对话与重试按钮
        IconButton(
            onClick = onNewConversation,
            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp).size(36.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.AddComment, contentDescription = "新建对话", tint = TextSecondary)
        }
        IconButton(
            onClick = onRegenerate,
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp).size(36.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "重新生成", tint = TextSecondary)
        }

        // 输入框
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp, max = 160.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(InputBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary
            ),
            cursorBrush = SolidColor(TavernPurple),
            maxLines = 6,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "输入消息...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
                innerTextField()
            }
        )

        // 发送/停止按钮
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(
                onClick = {
                    if (isGenerating) {
                        onStop()
                    } else if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGenerating) ErrorRed
                        else if (text.isNotBlank()) TavernPurple
                        else TavernPurpleDark.copy(alpha = 0.5f)
                    )
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Default.Stop
                                  else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isGenerating) "停止" else "发送",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
