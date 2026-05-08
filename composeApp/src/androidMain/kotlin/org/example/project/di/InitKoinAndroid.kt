package org.example.project.di

import android.app.Application
import org.koin.android.ext.koin.androidContext

fun Application.initKoinAndroid() = initKoin(platformModules = listOf(

), platformAction = {
    androidContext(this@initKoinAndroid)
})