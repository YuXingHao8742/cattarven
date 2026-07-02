package cat.tarven

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.tarven.data.repository.CharacterRepository
import cat.tarven.ui.navigation.Screen
import cat.tarven.ui.screens.CharacterEditScreen
import cat.tarven.ui.screens.CharacterListScreen
import cat.tarven.ui.screens.ChatScreen
import cat.tarven.ui.screens.SettingsScreen
import cat.tarven.ui.theme.CattarvenTheme
import cat.tarven.viewmodel.CharacterViewModel
import cat.tarven.viewmodel.ChatViewModel
import cat.tarven.viewmodel.SettingsViewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun CatTarvenApp() {
    val navController = rememberNavController()
    val characterViewModel: CharacterViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val labViewModel: cat.tarven.viewmodel.LabViewModel = hiltViewModel()
    val context = LocalContext.current
    val characterRepo = remember { CharacterRepository(context) }

    CattarvenTheme(
        darkTheme = settingsViewModel.isDarkMode,
        appFontSize = settingsViewModel.appFontSize
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            // 背景图片层 — 仅在非对话和非角色编辑页面显示全局背景
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val isInChatOrEdit = currentRoute?.startsWith("chat/") == true ||
                currentRoute?.startsWith("character_edit/") == true
            if (!isInChatOrEdit && settingsViewModel.backgroundImagePath.isNotBlank()) {
                val bgFile = File(settingsViewModel.backgroundImagePath)
                if (bgFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(bgFile),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(settingsViewModel.backgroundBlurRadius.dp),
                        contentScale = ContentScale.Crop
                    )
                    // 半透明遮罩层，确保文字可读
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (settingsViewModel.isDarkMode)
                                    Color.Black.copy(alpha = 0.55f)
                                else
                                    Color.White.copy(alpha = 0.55f)
                            )
                    )
                }
            }

            // 主导航内容
            NavHost(
                navController = navController,
                startDestination = Screen.CharacterList.route
            ) {
                // 角色/对话列表
                composable(Screen.CharacterList.route) {
                    CharacterListScreen(
                        characterViewModel = characterViewModel,
                        onConversationClick = { convWithChar ->
                            characterViewModel.selectConversation(convWithChar)
                            chatViewModel.initChat(convWithChar)
                            navController.navigate(Screen.Chat.createRoute(convWithChar.conversation.id))
                        },
                        onCreateCharacter = {
                            characterViewModel.startEditCharacter(null)
                            navController.navigate(Screen.CharacterEdit.createRoute("new"))
                        },
                        onSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onLabClick = {
                            navController.navigate(Screen.Lab.route)
                        }
                    )
                }

                // 角色编辑
                composable(
                    route = Screen.CharacterEdit.route,
                    arguments = listOf(
                        navArgument("characterId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val characterId = backStackEntry.arguments?.getString("characterId") ?: "new"
                    val character = if (characterId == "new") null else characterViewModel.conversations.find { it.character.id == characterId }?.character

                    CharacterEditScreen(
                        character = character,
                        characterViewModel = characterViewModel,
                        settingsViewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // 聊天
                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType }
                    )
                ) {
                    ChatScreen(
                        chatViewModel = chatViewModel,
                        settingsViewModel = settingsViewModel,
                        characterViewModel = characterViewModel,
                        onBack = { navController.popBackStack() },
                        onSettings = { navController.navigate(Screen.Settings.route) },
                        onEditCharacter = { charId -> navController.navigate(Screen.CharacterEdit.createRoute(charId)) }
                    )
                }

                // 设置
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // 实验室
                composable(Screen.Lab.route) {
                    cat.tarven.ui.screens.LabScreen(
                        labViewModel = labViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
