package org.example.project.data.models

data class ConnectionPrefModel(
    val host: String,
    val port: Int
) {
    companion object {
        fun default() = ConnectionPrefModel(
            host = "192.168.0.192",
            port = 8080
        )
    }
}
