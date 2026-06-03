package org.example.project.v2.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import org.example.project.data.PreferencesDataSource
import org.example.project.data.models.UserPrefModel
import org.example.project.v2.data.models.LoginRequest
import org.example.project.v2.data.models.LoginResponse
import kotlinx.serialization.json.jsonPrimitive

interface V2AuthRepository {
    suspend fun login(login: String, password: String): Result<Unit>
    suspend fun logout()

    class Impl(
        private val httpClient: HttpClient,
        private val preferencesDataSource: PreferencesDataSource,
    ) : V2AuthRepository {
        override suspend fun login(login: String, password: String): Result<Unit> {
            return runCatching {
                val settings = preferencesDataSource.connectionSettingsFlow.first()
                val url = "http://${settings.host}:${settings.port}/v2/login"
                val response = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(login = login, password = password))
                }.body<LoginResponse>()

                preferencesDataSource.saveUserData(
                    UserPrefModel(
                        token = response.token,
                        userId = response.user["id"]?.jsonPrimitive?.content,
                        userName = response.user["name"]?.jsonPrimitive?.content,
                    ),
                )
            }
        }

        override suspend fun logout() {
            preferencesDataSource.clearAuthData()
        }
    }
}
