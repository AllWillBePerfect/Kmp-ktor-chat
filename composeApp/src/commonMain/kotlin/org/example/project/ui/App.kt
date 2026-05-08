package org.example.project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import org.example.project.ui.screens.chat.ChatRoute
import org.example.project.ui.AppRoutes
import org.example.project.ui.screens.chats.ChatsRoute

@Composable
@Preview()
fun App() {
    AppTheme {
        /*var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }*/
        AppNavigation()
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
    navHostController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navHostController,
        startDestination = AppRoutes.Chats
    ) {
        composable<AppRoutes.Chats> {
            ChatsRoute(
                navigateToChatScreen = {
                    navHostController.navigate(AppRoutes.Chat)
                }
            )
        }
        composable<AppRoutes.Chat> {
            ChatRoute()
        }
    }
}
