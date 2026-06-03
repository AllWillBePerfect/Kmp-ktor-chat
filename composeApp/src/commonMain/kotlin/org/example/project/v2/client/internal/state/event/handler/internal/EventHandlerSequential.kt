package org.example.project.v2.client.internal.state.event.handler.internal

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.v2.client.api.event.EventHandlingResult
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.internal.state.plugin.logic.internal.LogicRegistry
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsLogic
import org.example.project.v2.platform.taggedLogger

/**
 * Processes chat events sequentially.
 *
 * This mirrors the main invariant from the SDK:
 * event application order must be deterministic.
 */
internal class EventHandlerSequential(
    private val events: Flow<ChatEvent>,
    private val queryChannelsLogic: QueryChannelsLogic,
    private val logicRegistry: LogicRegistry,
    scope: CoroutineScope,
) : EventHandler {
    private val logger by taggedLogger(TAG)

    private val supervisorJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        logger.e(throwable) {
            "Unhandled coroutine exception while applying chat events; context=$context"
        }
    }
    private val scope = CoroutineScope(
        scope.coroutineContext + supervisorJob + exceptionHandler + Dispatchers.Default,
    )

    private val mutex = Mutex()
    private var listeningJob: Job? = null

    override fun startListening() {
        if (listeningJob != null) return

        logger.i { "startListening" }
        listeningJob = scope.launch {
            events.collect { event ->
                applyEvent(event)
            }
        }
    }

    override fun stopListening() {
        logger.i { "stopListening" }
        listeningJob?.cancel()
        listeningJob = null
        supervisorJob.cancelChildren()
    }

    override suspend fun handleEvents(vararg events: ChatEvent) {
        events.forEach { event ->
            applyEvent(event)
        }
    }

    private suspend fun applyEvent(event: ChatEvent) {
        mutex.withLock {
            logger.d { "Applying event sequentially; type=${event.type} seq=${event.seq}" }
            handleChatEvents(listOf(event))
        }
    }

    private suspend fun handleChatEvents(events: List<ChatEvent>) {
        queryChannelsLogic.parseChatEventResults(events).forEach { result ->
            when (result) {
                is EventHandlingResult.Add -> queryChannelsLogic.addChannel(result.channel)
                is EventHandlingResult.WatchAndAdd -> queryChannelsLogic.watchAndAddChannel(result.cid)
                is EventHandlingResult.Remove -> queryChannelsLogic.removeChannel(result.cid)
                EventHandlingResult.Skip -> Unit
            }
            logger.v { "Event handled; result=$result" }
        }
        events.forEach { event -> logicRegistry.handleEvent(event) }
        events.forEach { event -> queryChannelsLogic.applyQueryStateForEvent(event) }
    }

    private companion object {
        const val TAG = "Chat:V2EventHandlerSequential"
    }
}
