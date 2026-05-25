package org.example.project.di

import org.example.project.platform.DatastoreProvider
import org.example.project.platform.DatastoreProviderJvm
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun initKoinDesktop() = initKoin(platformModules = listOf(
    jvmModule
))

val jvmModule = module {
    singleOf(::DatastoreProviderJvm) { bind<DatastoreProvider>() }
}