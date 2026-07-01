package cat.tarven.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.InsertDriveFile
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.tarven.data.model.Attachment
import cat.tarven.ui.theme.*

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import cat.tarven.data.repository.PropItem
import androidx.compose.material.icons.filled.Backpack

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onSendProp: (PropItem) -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
    onPickFile: () -> Unit,
    isGenerating: Boolean,
    props: List<PropItem>,
    attachments: List<Attachment>,
    onRemoveAttachment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPropsMenu by remember { mutableStateOf(false) }

    val canSend = text.isNotBlank() || attachments.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 附件预览区
        if (attachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(attachments) { index, attachment ->
                    AttachmentPreview(
                        attachment = attachment,
                        onRemove = { onRemoveAttachment(index) }
                    )
                }
            }
        }

        // 输入行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 新建对话按钮
            IconButton(
                onClick = onNewConversation,
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp).size(36.dp)
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "新建对话", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // 附件按钮（替代原来的重新生成按钮）
            IconButton(
                onClick = onPickFile,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp).size(36.dp)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "添加附件", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 输入框
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 160.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.inputBackground)
                    .border(1.dp, MaterialTheme.inputBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(TavernPurple),
                maxLines = 6,
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "输入消息...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.textMuted
                        )
                    }
                    innerTextField()
                }
            )

            // 道具按钮
            Box {
                IconButton(
                    onClick = { showPropsMenu = true },
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backpack,
                        contentDescription = "快捷道具",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showPropsMenu,
                    onDismissRequest = { showPropsMenu = false },
                    modifier = Modifier.background(MaterialTheme.surfaceElevated)
                ) {
                    if (props.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("暂无道具，请在设置中添加", color = MaterialTheme.textMuted) },
                            onClick = { showPropsMenu = false }
                        )
                    } else {
                        props.forEach { prop ->
                            DropdownMenuItem(
                                text = { Text(prop.name, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    onSendProp(prop)
                                    showPropsMenu = false
                                }
                            )
                        }
                    }
                }
            }

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
                        } else if (canSend) {
                            onSend(text)
                            onTextChange("")
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGenerating) ErrorRed
                            else if (canSend) TavernPurple
                            else TavernPurpleDark.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Default.Stop
                                      else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isGenerating) "停止" else "发送",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单个附件的预览卡片
 */
@Composable
private fun AttachmentPreview(
    attachment: Attachment,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        if (attachment.type == "image") {
            // 图片预览
            Box(modifier = Modifier.size(80.dp)) {
                coil.compose.AsyncImage(
                    model = java.io.File(attachment.filePath),
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                // 删除按钮
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "移除",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // 文本文件预览
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = TavernPurpleLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onRemove() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
