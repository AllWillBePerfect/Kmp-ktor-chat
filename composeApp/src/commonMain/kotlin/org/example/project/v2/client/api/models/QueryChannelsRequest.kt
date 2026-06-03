package org.example.project.v2.client.api.models

import org.example.project.v2.core.models.FilterObject
import org.example.project.v2.core.models.NeutralFilterObject

open class QueryChannelsRequest @JvmOverloads constructor(
    val filter: FilterObject = NeutralFilterObject,
    var offset: Int = 0,
    var limit: Int,
    var messageLimit: Int? = null,
    var memberLimit: Int? = null,
) : ChannelRequest<QueryChannelsRequest> {

    override var state: Boolean = true
    override var watch: Boolean = true
    override var presence: Boolean = false

    open fun withMessages(limit: Int): QueryChannelsRequest {
        messageLimit = limit
        return this
    }

    open fun withMembers(limit: Int): QueryChannelsRequest {
        memberLimit = limit
        return this
    }

    open fun withOffset(offset: Int): QueryChannelsRequest {
        this.offset = offset
        return this
    }

    open fun withLimit(limit: Int): QueryChannelsRequest {
        this.limit = limit
        return this
    }

    val isFirstPage: Boolean
        get() = offset == 0
}
