package org.example.project.data.models

data class AppSettings(
    val host: String,
    val port: Int,
    val themeMode: ThemeMode
) {
    companion object {
        fun default(): AppSettings {
            val connection = ConnectionPrefModel.default()
            return AppSettings(
                host = connection.host,
                port = connection.port,
                themeMode = ThemeMode.SYSTEM
            )
        }
    }
}
