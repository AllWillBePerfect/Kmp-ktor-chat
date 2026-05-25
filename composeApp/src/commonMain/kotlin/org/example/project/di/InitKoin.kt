package org.example.project.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.example.project.data.PreferencesDataSource
import org.example.project.data.repository.AuthRepository
import org.example.project.ktor.WsClient
import org.example.project.ktor.WsClientContract
import org.example.project.platform.DatastoreProvider
import org.example.project.ui.MainViewModel
import org.example.project.ui.screens.chat.ChatViewModel
import org.example.project.ui.screens.chats.ChatsViewModel
import org.example.project.ui.screens.conversation.ConversationViewModel
import org.example.project.ui.screens.login.LoginViewModel
import org.example.project.ui.screens.settings.SettingsViewModel
import org.example.project.utils.AppDispatchers
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
    platformModules: List<Module> = emptyList(),
    platformAction: KoinApplication.() -> Unit = {}
) = startKoin {
    appDeclaration()

    modules(
        viewModelModule,
        module,
        *platformModules.toTypedArray()
    )

    platformAction(this)
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
    viewModelOf(::ChatsViewModel)
    viewModelOf(::ConversationViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainViewModel)
}

val module = module {
    factory {
        HttpClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }
    singleOf(::WsClient) { bind<WsClientContract>() }

    factory {
        get<DatastoreProvider>().provide { "preferences.preferences_pb" }
    }
    singleOf(::PreferencesDataSource)
    singleOf(AuthRepository::Impl) { bind<AuthRepository>() }

    singleOf(AppDispatchers::Impl) { bind<AppDispatchers>() }


    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
