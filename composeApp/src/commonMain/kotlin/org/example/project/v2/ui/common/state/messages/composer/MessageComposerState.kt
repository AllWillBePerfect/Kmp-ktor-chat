package org.example.project.v2.ui.common.state.messages.composer

data class MessageComposerState(
    val inputValue: String = "",
    val errorMessage: String? = null,
) {
    val canSendMessage: Boolean = inputValue.isNotBlank()
}
