package cat.tarven.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.tarven.data.model.Character
import cat.tarven.ui.theme.*
import cat.tarven.viewmodel.CharacterViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    character: Character?,
    characterViewModel: CharacterViewModel,
    onBack: () -> Unit
) {
    val isNew = character == null || character.name.isBlank()

    var name by remember { mutableStateOf(character?.name ?: "") }
    var description by remember { mutableStateOf(character?.description ?: "") }
    var firstMessage by remember { mutableStateOf(character?.firstMessage ?: "") }
    var systemPrompt by remember { mutableStateOf(character?.systemPrompt ?: "") }
    var creatorNotes by remember { mutableStateOf(character?.creatorNotes ?: "") }
    var avatarUri by remember { mutableStateOf(character?.avatarUri) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = characterViewModel.saveAvatarImage(it)
            if (savedPath != null) {
                avatarUri = savedPath
            }
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isNew) "创建角色" else "编辑角色",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 头像上传
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = "file://$avatarUri",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = "上传头像", tint = TextSecondary)
                            Text("点击上传", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 角色名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("角色名称 *") },
                placeholder = { Text("例：猫猫酒馆老板", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 角色描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("角色描述") },
                placeholder = { Text("描述角色的外貌、身份、背景故事等", color = TextMuted) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 开场白
            OutlinedTextField(
                value = firstMessage,
                onValueChange = { firstMessage = it },
                label = { Text("开场白") },
                placeholder = { Text("角色的第一条消息，支持 {{user}} 和 {{char}}", color = TextMuted) },
                minLines = 3,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 角色系统提示词
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("角色系统提示词（可选）") },
                placeholder = { Text("角色专属的系统提示词，追加到全局提示词之后", color = TextMuted) },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 创作者备注
            OutlinedTextField(
                value = creatorNotes,
                onValueChange = { creatorNotes = it },
                label = { Text("创作者备注（可选）") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 保存按钮
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val updatedCharacter = (character ?: Character()).copy(
                        name = name,
                        description = description,
                        firstMessage = firstMessage,
                        systemPrompt = systemPrompt,
                        creatorNotes = creatorNotes,
                        avatarUri = avatarUri,
                        updatedAt = System.currentTimeMillis()
                    )
                    characterViewModel.saveCharacter(updatedCharacter)
                    onBack()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TavernPurple,
                contentColor = TextPrimary,
                disabledContainerColor = TavernPurpleDark.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = if (isNew) "创建角色" else "保存修改",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
