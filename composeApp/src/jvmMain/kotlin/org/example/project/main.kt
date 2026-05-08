package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.di.initKoinDesktop
import org.example.project.ui.App

fun main() {
    initKoinDesktop()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kmp-ktor-chat",
        ) {
            App()
        }
    }
}