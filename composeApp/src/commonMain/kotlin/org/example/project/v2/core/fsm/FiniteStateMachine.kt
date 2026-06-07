package org.example.project.v2.core.fsm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

class FiniteStateMachine<STATE : Any, EVENT : Any>(
    initialState: STATE,
    private val stateFunctions: Map<KClass<out STATE>, Map<KClass<out EVENT>, StateFunction<STATE, EVENT>>>,
    private val defaultEventHandler: (STATE, EVENT) -> STATE
) {
    private val _mutex = Mutex()
    private val _state: MutableStateFlow<STATE> = MutableStateFlow(initialState)

    val stateFlow: StateFlow<STATE> = _state
    val state: STATE
        get() = _state.value

    suspend fun sendEvent(event: EVENT) {
        _mutex.withLock {
            val currentState = _state.value
            val handler = stateFunctions[currentState::class]?.get(event::class) ?: defaultEventHandler
            _state.value = handler(currentState, event)
        }

    }

    fun stay(): STATE = state

    companion object {
        operator fun <STATE: Any, EVENT: Any> invoke(builder: FiniteStateMachineBuilder<STATE, EVENT>.() -> Unit): FiniteStateMachine<STATE, EVENT> {
            return FiniteStateMachineBuilder<STATE, EVENT>().apply(builder).build()
        }

    }


}

internal typealias StateFunction<S, E> = (S, E) -> S


