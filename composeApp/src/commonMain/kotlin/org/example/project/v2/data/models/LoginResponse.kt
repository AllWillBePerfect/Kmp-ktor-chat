package org.example.project.v2.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LoginResponse(
    val user: JsonObject,
    val token: String,
)
