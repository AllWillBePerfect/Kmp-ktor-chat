package org.example.project.v2.client.utils.observable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.v2.client.ChatEventListener
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ConnectingEvent
import org.example.project.v2.client.events.ConnectionErrorEvent
import org.example.project.v2.client.events.DisconnectedEvent
import org.example.project.v2.client.socket.ChatSocket
import org.example.project.v2.client.socket.SocketListener

internal class ChatEventsObservable(
    private val scope: CoroutineScope,
    private val chatSocket: ChatSocket,
) {
    private val mutex = Mutex()
    private val subscriptions = mutableSetOf<EventSubscription>()
    private val eventsMapper = EventsMapper(this)

    private fun onNext(event: ChatEvent) {
        notifySubscriptions(event)
    }

    fun subscribe(
        filter: (ChatEvent) -> Boolean = { true },
        listener: ChatEventListener<ChatEvent>,
    ): Disposable {
        return addSubscription(SubscriptionImpl(filter, listener))
    }

    fun subscribeSuspend(
        filter: (ChatEvent) -> Boolean = { true },
        listener: ChatEventSuspendListener<ChatEvent>,
    ): Disposable {
        return addSubscription(SuspendSubscription(scope, filter, listener))
    }

    fun subscribeSingle(
        filter: (ChatEvent) -> Boolean = { true },
        listener: ChatEventListener<ChatEvent>,
    ): Disposable {
        return addSubscription(
            SubscriptionImpl(filter, listener).apply {
                afterEventDelivered = this::dispose
            },
        )
    }

    private fun notifySubscriptions(event: ChatEvent) {
        scope.launch {
            mutex.withLock {
                val iterator = subscriptions.iterator()
                while (iterator.hasNext()) {
                    val subscription = iterator.next()
                    if (subscription.isDisposed) {
                        iterator.remove()
                    } else {
                        subscription.onNext(event)
                    }
                }
                if (subscriptions.isEmpty()) {
                    chatSocket.unsubscribe(eventsMapper)
                }
            }
        }
    }

    private fun addSubscription(subscription: EventSubscription): Disposable {
        scope.launch {
            mutex.withLock {
                if (subscriptions.isEmpty()) {
                    chatSocket.subscribe(eventsMapper)
                }
                subscriptions.add(subscription)
            }
        }
        return subscription
    }

    internal fun interface ChatEventSuspendListener<EventT : ChatEvent> {
        suspend fun onEvent(event: EventT)
    }

    private class EventsMapper(
        private val observable: ChatEventsObservable,
    ) : SocketListener() {
        override fun onConnecting() {
            observable.onNext(
                ConnectingEvent(
                    type = "connection.connecting",
                    createdAt = null,
                    rawCreatedAt = null,
                ),
            )
        }

        override fun onConnected(event: ConnectedEvent) {
            observable.onNext(event)
        }

        override fun onDisconnected(cause: String?) {
            observable.onNext(
                DisconnectedEvent(
                    type = "connection.disconnected",
                    createdAt = null,
                    rawCreatedAt = null,
                    disconnectCause = cause,
                ),
            )
        }

        override fun onEvent(event: ChatEvent) {
            observable.onNext(event)
        }

        override fun onError(error: Throwable) {
            observable.onNext(
                ConnectionErrorEvent(
                    type = "connection.error",
                    createdAt = null,
                    rawCreatedAt = null,
                    connectionId = null,
                    errorMessage = error.message.orEmpty(),
                ),
            )
        }
    }
}
