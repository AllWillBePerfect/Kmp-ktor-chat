package org.example.project.v2.client.utils.observable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.v2.client.ChatEventListener
import org.example.project.v2.client.events.ChatEvent

interface Disposable {
    val isDisposed: Boolean
    fun dispose()
}

internal interface EventSubscription : Disposable {
    fun onNext(event: ChatEvent)
}

internal open class SubscriptionImpl(
    private val filter: (ChatEvent) -> Boolean,
    listener: ChatEventListener<ChatEvent>,
) : EventSubscription {
    @Volatile
    private var listener: ChatEventListener<ChatEvent>? = listener

    @Volatile
    override var isDisposed: Boolean = false

    var afterEventDelivered: () -> Unit = {}

    override fun dispose() {
        isDisposed = true
        listener = null
    }

    final override fun onNext(event: ChatEvent) {
        if (isDisposed) return

        if (filter(event)) {
            try {
                listener?.onEvent(event)
            } finally {
                afterEventDelivered()
            }
        }
    }
}

internal class SuspendSubscription(
    private val scope: CoroutineScope,
    private val filter: (ChatEvent) -> Boolean,
    listener: ChatEventsObservable.ChatEventSuspendListener<ChatEvent>,
) : EventSubscription {
    @Volatile
    private var listener: ChatEventsObservable.ChatEventSuspendListener<ChatEvent>? = listener

    @Volatile
    override var isDisposed: Boolean = false

    override fun dispose() {
        isDisposed = true
        listener = null
    }

    override fun onNext(event: ChatEvent) {
        if (isDisposed) return

        scope.launch {
            if (filter(event)) {
                listener?.onEvent(event)
            }
        }
    }
}
