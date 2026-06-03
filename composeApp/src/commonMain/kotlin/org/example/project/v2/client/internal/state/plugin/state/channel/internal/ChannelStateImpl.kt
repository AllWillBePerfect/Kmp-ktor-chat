package org.example.project.v2.client.internal.state.plugin.state.channel.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User

internal class ChannelStateImpl(
    override val channelType: String,
    override val channelId: String,
) : ChannelState {

    override val cid: String = "$channelType:$channelId"

    private val _channelData = MutableStateFlow<Channel?>(null)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _typing = MutableStateFlow<List<User>>(emptyList())
    private val _reads = MutableStateFlow<Map<String, String?>>(emptyMap())
    private val _unreadCount = MutableStateFlow(0)
    private val _members = MutableStateFlow<List<User>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _messageCount = MutableStateFlow<Int?>(null)

    override val channelData: StateFlow<Channel?> = _channelData.asStateFlow()
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    override val typing: StateFlow<List<User>> = _typing.asStateFlow()
    override val reads: StateFlow<Map<String, String?>> = _reads.asStateFlow()
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    override val members: StateFlow<List<User>> = _members.asStateFlow()
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    override val messageCount: StateFlow<Int?> = _messageCount.asStateFlow()

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }

    fun setChannel(channel: Channel?) {
        _channelData.value = channel
        _messageCount.value = channel?.messageCount ?: channel?.messages?.size
        _messages.value = channel?.messages.orEmpty()
        _unreadCount.value = channel?.unreadCount ?: 0
        _members.value = channel?.members.orEmpty()
        _reads.value = channel?.reads.orEmpty().associate { it.user.id to it.lastReadMessageId }
        if (channel == null) {
            _typing.value = emptyList()
            _reads.value = emptyMap()
        }
    }

    fun setMessages(messages: List<Message>) {
        _messages.value = messages
        _messageCount.value = messages.size
        val currentChannel = _channelData.value
        if (currentChannel != null) {
            _channelData.value = currentChannel.copy(
                messages = messages,
                messageCount = messages.size,
                lastMessageAt = messages.lastOrNull()?.createdAt,
            )
        }
    }

    fun setMembers(members: List<User>) {
        _members.value = members
        val currentChannel = _channelData.value
        if (currentChannel != null) {
            _channelData.value = currentChannel.copy(
                members = members,
                memberCount = members.size,
            )
        }
    }

    fun upsertMember(user: User) {
        val updated = (_members.value.associateBy { it.id } + (user.id to user)).values.toList()
        setMembers(updated)
    }

    fun removeMember(userId: String) {
        if (userId.isBlank()) return
        setMembers(_members.value.filterNot { it.id == userId })
    }

    fun startTyping(user: User) {
        if (user.id.isBlank()) return
        if (_typing.value.any { it.id == user.id }) return
        _typing.value = _typing.value + user
    }

    fun stopTyping(userId: String) {
        if (userId.isBlank()) return
        _typing.value = _typing.value.filterNot { it.id == userId }
    }

    fun setRead(userId: String, lastReadMessageId: String?) {
        if (userId.isBlank()) return
        _reads.value = _reads.value + (userId to lastReadMessageId)
        val currentChannel = _channelData.value ?: return
        _channelData.value = currentChannel.copy(
            reads = currentChannel.reads
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
                .filterNotNull(),
        )
    }

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
        val currentChannel = _channelData.value ?: return
        _channelData.value = currentChannel.copy(unreadCount = count.coerceAtLeast(0))
    }

    fun markRead(currentUserId: String): Boolean {
        if (currentUserId.isBlank()) return false
        val lastMessage = _messages.value.lastOrNull() ?: return true
        val currentReadMessageId = _reads.value[currentUserId]
        return if (lastMessage.id != currentReadMessageId) {
            _reads.value = _reads.value + (currentUserId to lastMessage.id)
            _unreadCount.value = 0
            true
        } else {
            false
        }
    }

    override fun toChannel(): Channel? = _channelData.value

    override fun getMessageById(id: String): Message? = _messages.value.firstOrNull { it.id == id }
}
