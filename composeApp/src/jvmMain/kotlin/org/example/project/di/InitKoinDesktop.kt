package org.example.project.di

import org.example.project.platform.AppLogger
import org.example.project.platform.AppLoggerJvm
import org.example.project.platform.DatastoreProvider
import org.example.project.platform.DatastoreProviderJvm
import org.example.project.platform.RoomDatabaseBuilderProvider
import org.example.project.platform.RoomDatabaseBuilderProviderJvm
import org.example.project.v2.client.network.JvmNetworkStateProvider
import org.example.project.v2.client.network.NetworkStateProvider as V2NetworkStateProvider
import org.example.project.v2.platform.AppLogger as V2AppLogger
import org.example.project.v2.platform.AppLoggerJvm as V2AppLoggerJvm
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun initKoinDesktop() = initKoin(platformModules = listOf(
    jvmModule
))

val jvmModule = module {
    singleOf(::DatastoreProviderJvm) { bind<DatastoreProvider>() }
    singleOf(::RoomDatabaseBuilderProviderJvm) { bind<RoomDatabaseBuilderProvider>() }

    singleOf(::AppLoggerJvm) { bind<AppLogger>() }
    singleOf(::V2AppLoggerJvm) { bind<V2AppLogger>() }
    single<V2NetworkStateProvider> { JvmNetworkStateProvider() }
}
