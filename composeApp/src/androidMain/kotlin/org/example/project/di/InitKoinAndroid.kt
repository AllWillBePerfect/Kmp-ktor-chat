package org.example.project.di

import android.app.Application
import android.content.Context
import org.example.project.platform.AppLogger
import org.example.project.platform.AppLoggerAndroid
import org.example.project.platform.DatastoreProvider
import org.example.project.platform.DatastoreProviderAndroid
import org.example.project.platform.RoomDatabaseBuilderProvider
import org.example.project.platform.RoomDatabaseBuilderProviderAndroid
import org.example.project.v2.client.network.AndroidNetworkStateProvider
import org.example.project.v2.client.network.NetworkStateProvider as V2NetworkStateProvider
import org.example.project.v2.platform.AppLogger as V2AppLogger
import org.example.project.v2.platform.AppLoggerAndroid as V2AppLoggerAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun Application.initKoinAndroid() = initKoin(platformModules = listOf(
    androidModule
), platformAction = {
    androidContext(this@initKoinAndroid)
})

val androidModule = module {
    singleOf(::DatastoreProviderAndroid) { bind<DatastoreProvider>() }
    singleOf(::RoomDatabaseBuilderProviderAndroid) { bind<RoomDatabaseBuilderProvider>() }

    singleOf(::AppLoggerAndroid) { bind<AppLogger>() }
    singleOf(::V2AppLoggerAndroid) { bind<V2AppLogger>() }
    single<V2NetworkStateProvider> {
        AndroidNetworkStateProvider(
            scope = get(),
            connectivityManager = androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager,
        )
    }
}
