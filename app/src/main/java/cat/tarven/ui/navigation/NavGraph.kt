package cat.tarven.ui.navigation

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    data object CharacterList : Screen("character_list")
    data object CharacterEdit : Screen("character_edit/{characterId}") {
        fun createRoute(characterId: String = "new") = "character_edit/$characterId"
    }
    data object Chat : Screen("chat/{characterId}") {
        fun createRoute(characterId: String) = "chat/$characterId"
    }
    data object Settings : Screen("settings")
    data object Lab : Screen("lab")
}
