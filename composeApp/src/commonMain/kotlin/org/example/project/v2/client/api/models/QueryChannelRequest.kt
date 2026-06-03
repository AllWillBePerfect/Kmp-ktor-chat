package org.example.project.v2.client.api.models

@Suppress("TooManyFunctions")
open class QueryChannelRequest : ChannelRequest<QueryChannelRequest> {

    override var state: Boolean = false
    override var watch: Boolean = false
    override var presence: Boolean = false

    var shouldRefresh: Boolean = false
    var isWatchChannel: Boolean = false
    var isNotificationUpdate: Boolean = false

    val messages: MutableMap<String, Any> = mutableMapOf()
    val watchers: MutableMap<String, Any> = mutableMapOf()
    val members: MutableMap<String, Any> = mutableMapOf()
    val data: MutableMap<String, Any> = mutableMapOf()

    open fun withData(data: Map<String, Any>): QueryChannelRequest {
        this.data.putAll(data)
        return this
    }

    open fun withMembers(limit: Int, offset: Int): QueryChannelRequest {
        state = true
        members[KEY_LIMIT] = limit
        members[KEY_OFFSET] = offset
        return this
    }

    open fun withWatchers(limit: Int, offset: Int): QueryChannelRequest {
        state = true
        watchers[KEY_LIMIT] = limit
        watchers[KEY_OFFSET] = offset
        return this
    }

    open fun withMessages(limit: Int): QueryChannelRequest {
        state = true
        messages[KEY_LIMIT] = limit
        return this
    }

    override fun toString(): String {
        return "QueryChannelRequest(" +
            "state=$state, " +
            "watch=$watch, " +
            "presence=$presence, " +
            "shouldRefresh=$shouldRefresh, " +
            "isWatchChannel=$isWatchChannel, " +
            "isNotificationUpdate=$isNotificationUpdate, " +
            "messages=$messages, " +
            "watchers=$watchers, " +
            "members=$members, " +
            "data=$data)"
    }

    internal companion object {
        private const val KEY_LIMIT = "limit"
        private const val KEY_OFFSET = "offset"
    }
}
