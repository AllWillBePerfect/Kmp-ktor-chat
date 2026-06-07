package org.example.project.v2.client.clientstate

import org.example.project.v2.core.models.User

sealed class UserState {

    object NotSet : UserState() { override fun toString(): String = "NotSet" }
    data class UserSet(val user: User) : UserState()

    data class AnonymousUserSet(val anonymousUser: User) : UserState()

    internal fun userOrError(): User = when (this) {
        is UserSet -> user
        is AnonymousUserSet -> anonymousUser
        else -> error("This state doesn't contain user!")
    }
}