package dev.canem.bluecheckers.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.canem.bluecheckers.ui.game.GameScreen
import dev.canem.bluecheckers.ui.home.HomeScreen
import dev.canem.bluecheckers.ui.rules.RulesScreen
import dev.canem.bluecheckers.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val RULES = "rules"
    const val SETTINGS = "settings"
    const val GAME_PREFIX = "game"
    const val ARG_LEVEL = "level"
    const val GAME = "$GAME_PREFIX/{$ARG_LEVEL}"

    /** Sentinel level value that flips GameViewModel into 2-player same-device mode. */
    const val LEVEL_TWO_PLAYER = -1

    fun game(level: Int) = "$GAME_PREFIX/$level"
    fun game2p() = game(LEVEL_TWO_PLAYER)
}

@Composable
fun BlueNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { level -> navController.navigate(Routes.game(level)) },
                onStart2Player = { navController.navigate(Routes.game2p()) },
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument(Routes.ARG_LEVEL) { type = NavType.IntType }),
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getInt(Routes.ARG_LEVEL) ?: 0
            GameScreen(level = level, onExit = { navController.popBackStack() })
        }
        composable(Routes.RULES) {
            RulesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
