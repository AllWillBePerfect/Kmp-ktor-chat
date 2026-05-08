package org.example.project.di

import org.example.project.ui.screens.chat.ChatViewModel
import org.example.project.ui.screens.chats.ChatsViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
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
        viewModelModule
    )

    platformAction(this)
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
    viewModelOf(::ChatsViewModel)
}