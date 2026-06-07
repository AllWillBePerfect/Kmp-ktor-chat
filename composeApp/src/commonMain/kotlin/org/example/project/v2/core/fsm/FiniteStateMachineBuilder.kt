package org.example.project.v2.core.fsm

import kotlin.reflect.KClass

class FiniteStateMachineBuilder<STATE : Any, EVENT : Any> {
    private lateinit var _initialState: STATE
    public val stateFunctions: MutableMap<KClass<out STATE>, Map<KClass<out EVENT>, StateFunction<STATE, EVENT>>> =
        mutableMapOf()
    private var _defaultHandler: StateFunction<STATE, EVENT> = { s, _ -> s }

    fun initialState(state: STATE) {
        _initialState = state
    }

    fun defaultHandler(defaultHandler: StateFunction<STATE, EVENT>) {
        _defaultHandler = defaultHandler
    }

    inline fun <reified S: STATE> state(stateHandlerBuilder: StateHandlerBuilder<STATE, EVENT, S>.() -> Unit) {
        stateFunctions[S::class] = StateHandlerBuilder<STATE,EVENT,S>().apply(stateHandlerBuilder).get()
    }

    fun build(): FiniteStateMachine<STATE, EVENT> {
        check(this::_initialState.isInitialized) { "Initial state must be set!" }
        return FiniteStateMachine(_initialState, stateFunctions, _defaultHandler)
    }
}