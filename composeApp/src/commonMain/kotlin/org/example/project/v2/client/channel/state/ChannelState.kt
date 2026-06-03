package org.example.project.v2.client.channel.state

import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User

/**
 * Minimal per-channel reactive state aligned with the SDK entry point.
 */
interface ChannelState {
    val channelType: String
    val channelId: String
    val cid: String

    val channelData: StateFlow<Channel?>
    val messages: StateFlow<List<Message>>
    val typing: StateFlow<List<User>>
    val reads: StateFlow<Map<String, String?>>
    val unreadCount: StateFlow<Int>
    val members: StateFlow<List<User>>
    val loading: StateFlow<Boolean>
    val messageCount: StateFlow<Int?>

    fun toChannel(): Channel?

    fun getMessageById(id: String): Message?
}
