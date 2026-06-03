package org.example.project.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.example.project.data.PreferencesDataSource
import org.example.project.data.repository.AuthRepository
import org.example.project.data.repository.LocalMessageRepository
import org.example.project.data.room.AppDatabase
import org.example.project.data.room.MessageDao
import org.example.project.data.room.getRoomDatabase
import org.example.project.ktor.WsClient
import org.example.project.ktor.WsClientContract
import org.example.project.platform.DatastoreProvider
import org.example.project.platform.RoomDatabaseBuilderProvider
import org.example.project.v2.client.ChatClient as V2ChatClient
import org.example.project.v2.client.network.NetworkStateProvider as V2NetworkStateProvider
import org.example.project.v2.data.repository.V2AuthRepository
import org.example.project.v2.platform.AppLogger as V2AppLogger
import org.example.project.v2.platform.V2Logs
import org.example.project.v2.ui.screens.chat.ChatViewModel as V2ChatViewModel
import org.example.project.v2.ui.screens.chat.MessageComposerViewModel as V2MessageComposerViewModel
import org.example.project.v2.ui.screens.chat.MessageListViewModel as V2MessageListViewModel
import org.example.project.v2.ui.screens.chats.ChatsViewModel as V2ChatsViewModel
import org.example.project.v2.ui.screens.login.LoginViewModel as V2LoginViewModel
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
}.also { koinApp ->
    V2Logs.install(koinApp.koin.get<V2AppLogger>())
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
    viewModelOf(::ChatsViewModel)
    viewModelOf(::ConversationViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::V2ChatsViewModel)
    viewModelOf(::V2ChatViewModel)
    viewModelOf(::V2MessageListViewModel)
    viewModelOf(::V2MessageComposerViewModel)
    viewModelOf(::V2LoginViewModel)
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
    singleOf(V2AuthRepository::Impl) { bind<V2AuthRepository>() }

    singleOf(AppDispatchers::Impl) { bind<AppDispatchers>() }


    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<RoomDatabase.Builder<AppDatabase>> {
        get<RoomDatabaseBuilderProvider>().provide()
    }

    single<AppDatabase> {
        getRoomDatabase(get())
    }

    single<MessageDao> {
        get<AppDatabase>().messageDao()
    }

    singleOf(LocalMessageRepository::Impl) { bind<LocalMessageRepository>() }
    single<V2ChatClient> {
        val settings = runBlocking { get<PreferencesDataSource>().connectionSettingsFlow.first() }
        V2ChatClient.Builder(
            apiKey = "v2-dev-key",
            wssUrl = "ws://${settings.host}:${settings.port}/connect",
        )
            .baseUrl("http://${settings.host}:${settings.port}")
            .httpClient(get())
            .networkStateProvider(get<V2NetworkStateProvider>())
            .clientScope(get())
            .build()
    }

}
