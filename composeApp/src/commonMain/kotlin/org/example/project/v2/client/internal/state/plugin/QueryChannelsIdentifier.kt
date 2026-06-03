package org.example.project.v2.client.internal.state.plugin

import org.example.project.v2.core.models.FilterObject

internal sealed interface QueryChannelsIdentifier {
    data class Standard(
        val filter: FilterObject,
    ) : QueryChannelsIdentifier
}
