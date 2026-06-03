package org.example.project.v2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.example.project.data.PreferencesDataSource
import org.example.project.ui.screens.conversation.ConversationRoute
import org.example.project.ui.screens.settings.SettingsRoute
import org.example.project.v2.client.ChatClient
import org.example.project.v2.core.models.User
import org.example.project.v2.ui.screens.login.LoginRoute
import org.example.project.v2.ui.screens.chat.ChatRoute
import org.example.project.v2.ui.screens.chats.ChatsRoute
import org.koin.compose.koinInject

@Composable
fun V2App(
    navController: NavHostController = rememberNavController(),
    preferencesDataSource: PreferencesDataSource = koinInject(),
    chatClient: ChatClient = koinInject(),
) {
    val userData by preferencesDataSource.userDataFlow.collectAsStateWithLifecycle(null)
    val isAuthenticated = !userData?.token.isNullOrBlank() && !userData?.userId.isNullOrBlank()

    LaunchedEffect(userData?.token, userData?.userId, userData?.userName) {
        val token = userData?.token
        val userId = userData?.userId
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            chatClient.connectUser(
                user = User(
                    id = userId,
                    name = userData?.userName.orEmpty(),
                ),
                token = token,
            )
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(V2AppRoutes.Chats) {
                popUpTo(V2AppRoutes.Login) { inclusive = true }
            }
        } else {
            navController.navigate(V2AppRoutes.Login) {
                popUpTo(0)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) V2AppRoutes.Chats else V2AppRoutes.Login,
    ) {
        composable<V2AppRoutes.Login> {
            LoginRoute()
        }

        composable<V2AppRoutes.Chats> {
            ChatsRoute(
                navigateToChat = { channelCid ->
                    navController.navigate(V2AppRoutes.Chat(channelCid = channelCid))
                },
                navigateToSettings = {
                    navController.navigate(V2AppRoutes.Settings)
                }
            )
        }

        composable<V2AppRoutes.Chat> { backStackEntry ->
//            backStackEntry.toRoute<V2AppRoutes.Chat>()
//            ChatRoute(
//                onBackPressed = { navController.popBackStack() },
//            )
            ConversationRoute(
                onBackPressed = {navController.popBackStack()}
            )
        }

        composable<V2AppRoutes.Settings> {
            SettingsRoute(
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
