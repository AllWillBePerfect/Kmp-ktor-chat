package org.example.project.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import org.example.project.data.PreferencesDataSource
import org.example.project.data.models.UserPrefModel
import org.example.project.ui.screens.chat.ClientCommand
import org.example.project.ui.screens.chat.ServerEvent

interface AuthRepository {
    suspend fun login(login: String, pass: String): Result<Unit>
    suspend fun logout()

    class Impl(
        private val httpClient: HttpClient,
        private val preferences: PreferencesDataSource
    ) : AuthRepository {
        override suspend fun login(login: String, pass: String): Result<Unit> {
            return try {
                val settings = preferences.connectionSettingsFlow.first()
                val url = "http://${settings.host}:${settings.port}/login"

                val response: ServerEvent = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(ClientCommand.LoginCommand(login = login, password = pass))
                }.body()

                when (response) {
                    is ServerEvent.AuthSuccessEvent -> {
                        preferences.saveUserData(
                            UserPrefModel(
                                token = response.token,
                                userId = response.userId,
                                userName = response.userName
                            )
                        )
                        Result.success(Unit)
                    }

                    is ServerEvent.ErrorEvent -> {
                        Result.failure(Exception(response.message))
                    }

                    else -> {
                        Result.failure(Exception("Unknown error"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun logout() {
            preferences.clearAuthData()
        }
    }
}
