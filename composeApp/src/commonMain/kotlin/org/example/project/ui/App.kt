package org.example.project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.example.project.ui.AppRoutes
import org.example.project.ui.screens.chats.ChatsRoute
import org.example.project.ui.screens.conversation.ConversationRoute
import org.example.project.ui.screens.login.LoginRoute
import org.example.project.ui.screens.settings.SettingsRoute
import org.example.project.v2.ui.V2App
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.example.project.data.PreferencesDataSource

@Composable
@Preview()
fun App() {
    AppTheme {
        V2App()
    }
}

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        isDarkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val backgroundColor = colorScheme.surface

    CompositionLocalProvider(
        LocalBackgroundColor provides backgroundColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = {
                AppThemeBackground(
                    content = content
                )
            }
        )
    }
}

@Composable
fun AppThemeBackground(
    content: @Composable () -> Unit
) {
    val color = LocalBackgroundColor.current

    Surface(
        color = if (color == Color.Unspecified) Color.Transparent else color
    ) {
        content()
    }
}

val LocalBackgroundColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun AppNavigation(
    navHostController: NavHostController = rememberNavController(),
    pref: PreferencesDataSource = koinInject(),
    mainViewModel: MainViewModel = koinViewModel()
) {
    val userData by pref.userDataFlow.collectAsState(null)

    // Управление глобальным соединением
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        mainViewModel.onStart()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        mainViewModel.onStop()
    }

    LaunchedEffect(userData) {
        if (userData != null && !userData!!.isAuthenticated) {
            navHostController.navigate(AppRoutes.Login) {
                popUpTo(0)
            }
        }
    }

    NavHost(
        navController = navHostController,
        startDestination = AppRoutes.Chats
    ) {
        composable<AppRoutes.Login> {
            LoginRoute(
                onLoginSuccess = {
                    navHostController.navigate(AppRoutes.Chats) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
                onSettingsClick = {
                    navHostController.navigate(AppRoutes.Settings)
                }
            )
        }

        composable<AppRoutes.Chats> {
            ChatsRoute(
                navigateToChatScreen = { chatId ->
                    navHostController.navigate(AppRoutes.Chat(chatId = chatId))
                },
                navigateToSettingsScreen = {
                    navHostController.navigate(AppRoutes.Settings)
                }
            )
        }

        composable<AppRoutes.Chat> { backStackEntry ->
           /* val chat: AppRoutes.Chat = backStackEntry.toRoute()
            ConversationRoute(
                onBackPressed = {
                    navHostController.popBackStack()
                }
            )*/
        }

        composable<AppRoutes.Settings> {
            SettingsRoute(
                onBackPressed = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}
