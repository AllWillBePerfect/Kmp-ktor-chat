package org.example.project.di

import android.app.Application
import org.example.project.platform.DatastoreProvider
import org.example.project.platform.DatastoreProviderAndroid
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
}