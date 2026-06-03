package org.example.project.v2.client.api.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger

class StateRegistry(
    private val scope: CoroutineScope,
    private val currentUserId: () -> String? = { null },
) {
    private val logger by taggedLogger("Chat:StateRegistry")
    private val queryChannels = ConcurrentHashMap<QueryChannelsIdentifier, QueryChannelsMutableState>()
    private val channelStates = ConcurrentHashMap<Pair<String, String>, ChannelStateImpl>()

    private val _channels = MutableStateFlow<Map<String, Channel>?>(emptyMap())
    val channels: StateFlow<Map<String, Channel>?> = _channels.asStateFlow()
    private val _messagesByCid = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesByCid: StateFlow<Map<String, List<Message>>> = _messagesByCid.asStateFlow()

    fun getChannel(cid: String): Channel? = _channels.value?.get(cid)

    fun getActiveChannel(cid: String): Channel? {
        val separatorIndex = cid.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= cid.lastIndex) return null
        val key = cid.substring(0, separatorIndex) to cid.substring(separatorIndex + 1)
        return channelStates[key]?.toChannel()
    }

    fun isActiveChannel(cid: String): Boolean {
        val separatorIndex = cid.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= cid.lastIndex) return false
        val key = cid.substring(0, separatorIndex) to cid.substring(separatorIndex + 1)
        return channelStates.containsKey(key)
    }

    fun getMessages(cid: String): List<Message> = _messagesByCid.value[cid].orEmpty()

    fun channel(channelType: String, channelId: String): ChannelState {
        return channelStates.getOrPut(channelType to channelId) {
            ChannelStateImpl(channelType = channelType, channelId = channelId)
        }
    }

    fun channel(cid: String): ChannelState {
        val separatorIndex = cid.indexOf(':')
        require(separatorIndex > 0 && separatorIndex < cid.lastIndex) {
            "Expected cid in format 'type:id', but was '$cid'"
        }
        return channel(
            channelType = cid.substring(0, separatorIndex),
            channelId = cid.substring(separatorIndex + 1),
        )
    }

    fun queryChannels(filter: FilterObject): QueryChannelsState {
        val identifier = QueryChannelsIdentifier.Standard(filter)
        return queryChannels.getOrPut(identifier) {
            QueryChannelsMutableState(
                identifier = identifier,
                filter = filter,
                scope = scope,
                latestChannels = channels,
            )
        }
    }

    fun getChannels(cids: Collection<String>): Map<String, Channel> {
        val channelsMap = _channels.value.orEmpty()
        if (cids.isEmpty() || channelsMap.isEmpty()) return emptyMap()
        return cids.mapNotNull { cid ->
            channelsMap[cid]?.let { cid to it }
        }.toMap()
    }

    fun upsertChannel(channel: Channel) {
        val current = _channels.value.orEmpty()
        val mergedChannel = mergeIncomingChannel(
            existing = current[channel.cid],
            incoming = channel.copy(messages = getMessages(channel.cid)),
        )
        val updated = current + (channel.cid to mergedChannel)
        _channels.value = updated
        (channel(channel.cid) as ChannelStateImpl).setChannel(updated[channel.cid])
    }

    fun setMessages(cid: String, messages: List<Message>) {
        logger.d { "[setMessages] cid=$cid size=${messages.size}" }
        val updatedMessages = _messagesByCid.value + (cid to messages)
        _messagesByCid.value = updatedMessages
        (channel(cid) as ChannelStateImpl).setMessages(messages)
        val channel = _channels.value.orEmpty()[cid] ?: return
        _channels.value = _channels.value.orEmpty() + (
            cid to channel.copy(
                messages = messages,
                messageCount = messages.size,
                lastMessageAt = messages.lastOrNull()?.createdAt,
            )
        )
    }

    fun upsertMessage(cid: String, message: Message) {
        val current = getMessages(cid)
        val existingIndex = current.indexOfFirst { it.id == message.id && it.id.isNotBlank() }
        val updated = when {
            existingIndex >= 0 -> current.toMutableList().also { it[existingIndex] = message }
            message.clientMessageId != null -> {
                val byClientId = current.indexOfFirst { it.clientMessageId == message.clientMessageId }
                if (byClientId >= 0) {
                    current.toMutableList().also { it[byClientId] = message }
                } else {
                    current + message
                }
            }
            else -> current + message
        }
        setMessages(cid, updated.sortedBy { it.createdAt.orEmpty() })
    }

    fun setMembers(cid: String, members: List<User>) {
        (channel(cid) as ChannelStateImpl).setMembers(members)
        val currentChannel = _channels.value.orEmpty()[cid] ?: return
        _channels.value = _channels.value.orEmpty() + (
            cid to currentChannel.copy(
                members = members,
                memberCount = members.size,
            )
        )
    }

    fun upsertMember(cid: String, user: User) {
        val state = channel(cid) as ChannelStateImpl
        state.upsertMember(user)
        val currentChannel = _channels.value.orEmpty()[cid] ?: return
        _channels.value = _channels.value.orEmpty() + (
            cid to currentChannel.copy(
                members = state.members.value,
                memberCount = state.members.value.size,
            )
        )
    }

    fun removeMember(cid: String, userId: String) {
        val state = channel(cid) as ChannelStateImpl
        state.removeMember(userId)
        val currentChannel = _channels.value.orEmpty()[cid] ?: return
        _channels.value = _channels.value.orEmpty() + (
            cid to currentChannel.copy(
                members = state.members.value,
                memberCount = state.members.value.size,
            )
        )
    }

    fun startTyping(cid: String, user: User) {
        (channel(cid) as ChannelStateImpl).startTyping(user)
    }

    fun stopTyping(cid: String, userId: String) {
        (channel(cid) as ChannelStateImpl).stopTyping(userId)
    }

    fun setRead(cid: String, userId: String, lastReadMessageId: String?) {
        (channel(cid) as ChannelStateImpl).setRead(userId, lastReadMessageId)
        val currentChannel = _channels.value.orEmpty()[cid]
        if (currentChannel != null) {
            val updatedReads = currentChannel.reads
                .filterNot { it.user.id == userId }
                .plus(
                    currentChannel.members.firstOrNull { it.id == userId }?.let { user ->
                        org.example.project.v2.core.models.ChannelUserRead(
                            user = user,
                            lastReadMessageId = lastReadMessageId,
                        )
                    } ?: currentChannel.reads.firstOrNull { it.user.id == userId }?.copy(
                        lastReadMessageId = lastReadMessageId,
                    )
                )
                .filterNotNull()
            _channels.value = _channels.value.orEmpty() + (
                cid to currentChannel.copy(reads = updatedReads)
            )
        }
        if (userId == currentUserId()) {
            setUnreadCount(cid, 0)
        }
    }

    fun setUnreadCount(cid: String, count: Int) {
        (channel(cid) as ChannelStateImpl).setUnreadCount(count)
        val currentChannel = _channels.value.orEmpty()[cid] ?: return
        _channels.value = _channels.value.orEmpty() + (
            cid to currentChannel.copy(unreadCount = count.coerceAtLeast(0))
        )
    }

    fun deleteMessage(cid: String, messageId: String) {
        if (messageId.isBlank()) return
        val current = getMessages(cid)
        val updated = current.filterNot { it.id == messageId }
        if (updated.size == current.size) return
        logger.d { "[deleteMessage] cid=$cid messageId=$messageId" }
        setMessages(cid, updated)
    }

    fun removeChannel(cid: String) {
        val current = _channels.value.orEmpty()
        if (!current.containsKey(cid)) return
        logger.d { "[removeChannel] cid=$cid" }
        _channels.value = current - cid
        _messagesByCid.value = _messagesByCid.value - cid
        (channel(cid) as ChannelStateImpl).setChannel(null)
        (channel(cid) as ChannelStateImpl).setMessages(emptyList())
    }

    fun clear() {
        logger.d { "[clear] no args" }
        _channels.value = emptyMap()
        _messagesByCid.value = emptyMap()
        queryChannels.clear()
        channelStates.values.forEach { state ->
            state.setChannel(null)
            state.setMessages(emptyList())
            state.setMembers(emptyList())
            state.setUnreadCount(0)
            state.setLoading(false)
        }
        channelStates.clear()
    }

    private fun mergeIncomingChannel(existing: Channel?, incoming: Channel): Channel {
        val currentUserId = currentUserId().orEmpty()
        if (existing == null || currentUserId.isBlank()) return incoming

        val existingRead = existing.reads.firstOrNull { it.user.id == currentUserId }
        val incomingRead = incoming.reads.firstOrNull { it.user.id == currentUserId }
        val shouldPreserveLocalRead =
            existing.unreadCount == 0 &&
                incoming.unreadCount > 0 &&
                existingRead?.lastReadMessageId?.isNotBlank() == true &&
                existing.lastMessageAt == incoming.lastMessageAt &&
                existingRead.lastReadMessageId != incomingRead?.lastReadMessageId

        if (!shouldPreserveLocalRead) return incoming

        val mergedReads = incoming.reads
            .filterNot { it.user.id == currentUserId }
            .plus(existingRead)

        logger.d {
            "[mergeIncomingChannel] preserveLocalRead cid=${incoming.cid} " +
                "incomingUnread=${incoming.unreadCount} localUnread=${existing.unreadCount}"
        }

        return incoming.copy(
            unreadCount = 0,
            reads = mergedReads,
        )
    }
}
