package org.example.project

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.di.initKoinDesktop
import org.example.project.ui.App
import java.awt.Dimension

fun main() {
    initKoinDesktop()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kmp-ktor-chat",
            state = rememberWindowState(
                size = DpSize(
                    width = 400.dp,
                    height = 800.dp
                )
            ),
        ) {
            window.minimumSize = Dimension(400, 400)

            App()
        }
    }

}

