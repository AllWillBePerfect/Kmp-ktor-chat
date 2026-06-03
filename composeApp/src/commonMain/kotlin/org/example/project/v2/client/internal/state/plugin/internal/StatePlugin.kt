package org.example.project.v2.client.internal.state.plugin.internal

import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.internal.state.event.handler.internal.EventHandler
import org.example.project.v2.client.internal.state.plugin.logic.internal.LogicRegistry
import org.example.project.v2.client.plugin.listeners.ChannelMarkReadListener
import org.example.project.v2.client.setup.state.ClientState
import org.example.project.v2.client.setup.state.internal.MutableClientState

internal class StatePlugin(
    val eventHandler: EventHandler,
    val chatEventHandler: ChatEventHandler,
    val logicRegistry: LogicRegistry,
    val channelMarkReadListener: ChannelMarkReadListener,
    val mutableClientState: MutableClientState,
    val clientState: ClientState,
    val stateRegistry: StateRegistry,
)
