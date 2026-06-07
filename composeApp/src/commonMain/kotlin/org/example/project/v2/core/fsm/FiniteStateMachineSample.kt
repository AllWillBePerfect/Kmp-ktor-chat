package org.example.project.v2.core.fsm

import kotlinx.coroutines.flow.StateFlow

private class FiniteStateMachineSample {

    class UserStateService {

        suspend fun onUserUpdated(user: String) {
            fsm.sendEvent(DemoUserStateEvent.UserUpdated(user))
        }


        suspend fun onSetUser(user: String, isAnonymous: Boolean) {
            if (isAnonymous) {
                fsm.sendEvent(DemoUserStateEvent.ConnectAnonymous(user))
            } else {
                fsm.sendEvent(DemoUserStateEvent.ConnectUser(user))
            }
        }


        suspend fun onLogout() {
            fsm.sendEvent(DemoUserStateEvent.UnsetUser)
        }

        suspend fun onSocketUnrecoverableError() {
            fsm.sendEvent(DemoUserStateEvent.UnsetUser)

        }

        val state: DemoUserState
            get() = fsm.state

        val stateFlow: StateFlow<DemoUserState>
            get() = fsm.stateFlow

        private val fsm = FiniteStateMachine<DemoUserState, DemoUserStateEvent> {
            defaultHandler { state, event -> state }
            initialState(DemoUserState.NotSet)
            state<DemoUserState.NotSet> {
                onEvent<DemoUserStateEvent.ConnectUser> { event -> DemoUserState.UserSet(event.user) }
                onEvent<DemoUserStateEvent.ConnectAnonymous> { event -> DemoUserState.AnonymousUserSet(event.user) }
                onEvent<DemoUserStateEvent.UnsetUser> { state }
            }
            state<DemoUserState.UserSet> {
                onEvent<DemoUserStateEvent.UserUpdated> { event -> DemoUserState.UserSet(event.user) }
                onEvent<DemoUserStateEvent.UnsetUser> { DemoUserState.NotSet }
            }
            state<DemoUserState.AnonymousUserSet> {
                onEvent<DemoUserStateEvent.UserUpdated> { event -> DemoUserState.AnonymousUserSet(event.user) }
                onEvent<DemoUserStateEvent.UnsetUser> { DemoUserState.NotSet }
            }
        }
    }




}

private sealed class DemoUserState {
    object NotSet : DemoUserState() {
        override fun toString(): String = "NotSet"
    }

    data class UserSet(val user: String) : DemoUserState()
    data class AnonymousUserSet(val anonymousUser: String) : DemoUserState()

    fun userOrError(): String = when (this) {
        is UserSet -> user
        is AnonymousUserSet -> anonymousUser
        else -> error("This state doesn't contain user!")
    }
}

private sealed class DemoUserStateEvent {
    data class ConnectUser(val user: String) : DemoUserStateEvent()
    data class UserUpdated(val user: String) : DemoUserStateEvent()
    data class ConnectAnonymous(val user: String) : DemoUserStateEvent()
    object UnsetUser : DemoUserStateEvent() {
        override fun toString(): String = "UnsetUser"
    }
}