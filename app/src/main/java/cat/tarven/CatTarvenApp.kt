package cat.tarven

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.tarven.data.repository.CharacterRepository
import cat.tarven.ui.navigation.Screen
import cat.tarven.ui.screens.CharacterEditScreen
import cat.tarven.ui.screens.CharacterListScreen
import cat.tarven.ui.screens.ChatScreen
import cat.tarven.ui.screens.SettingsScreen
import cat.tarven.viewmodel.CharacterViewModel
import cat.tarven.viewmodel.ChatViewModel
import cat.tarven.viewmodel.SettingsViewModel

@Composable
fun CatTarvenApp() {
    val navController = rememberNavController()
    val characterViewModel: CharacterViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val labViewModel: cat.tarven.viewmodel.LabViewModel = viewModel()
    val context = LocalContext.current
    val characterRepo = remember { CharacterRepository(context) }

    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route
    ) {
        // 角色列表
        composable(Screen.CharacterList.route) {
            CharacterListScreen(
                characterViewModel = characterViewModel,
                onCharacterClick = { character ->
                    characterViewModel.selectCharacter(character)
                    chatViewModel.initChat(character)
                    navController.navigate(Screen.Chat.createRoute(character.id))
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
            val character = if (characterId == "new") null else characterRepo.getCharacter(characterId)

            CharacterEditScreen(
                character = character,
                characterViewModel = characterViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 聊天
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("characterId") { type = NavType.StringType }
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
