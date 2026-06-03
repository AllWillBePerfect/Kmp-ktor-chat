package org.example.project.v2.client.api.models

interface ChannelRequest<T : ChannelRequest<T>> {
    var state: Boolean
    var watch: Boolean
    var presence: Boolean
}
