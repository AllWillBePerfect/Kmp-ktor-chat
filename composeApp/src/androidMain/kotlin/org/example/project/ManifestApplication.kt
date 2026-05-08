package org.example.project

import android.app.Application
import org.example.project.di.initKoinAndroid

class ManifestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoinAndroid()
    }
}