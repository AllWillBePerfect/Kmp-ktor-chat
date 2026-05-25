package org.example.project.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefModel(
    val token: String? = null,
    val userId: String? = null,
    val userName: String? = null
) {
    val isAuthenticated: Boolean = !token.isNullOrBlank()
}
