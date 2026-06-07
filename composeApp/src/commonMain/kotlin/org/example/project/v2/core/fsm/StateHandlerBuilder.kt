package org.example.project.v2.core.fsm

import kotlin.reflect.KClass

class StateHandlerBuilder<STATE : Any, EVENT : Any, S : STATE> {
    val eventHandlers: MutableMap<KClass<out EVENT>, StateFunction<STATE, EVENT>> = mutableMapOf()
    val onEnterListeners: MutableList<(STATE, EVENT) -> Unit> = mutableListOf()

    inline fun <reified E : EVENT> onEvent(noinline func: S.(E) -> STATE) {
        @Suppress("UNCHECKED_CAST")
        eventHandlers[E::class] = func as StateFunction<STATE, EVENT>
    }

    inline fun onEnter(crossinline listener: S.(EVENT) -> Unit) {
        onEnterListeners.add { state, cause ->
            @Suppress("UNCHECKED_CAST")
            listener(state as S, cause)
        }
    }

    fun get(): Map<KClass<out EVENT>, StateFunction<STATE, EVENT>> = eventHandlers
    fun getEnterListeners(): MutableList<(STATE, EVENT) -> Unit> = onEnterListeners
}