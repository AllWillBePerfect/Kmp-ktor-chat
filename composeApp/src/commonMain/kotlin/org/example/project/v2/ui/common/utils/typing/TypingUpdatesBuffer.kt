package org.example.project.v2.ui.common.utils.typing

interface TypingUpdatesBuffer {
    fun onKeystroke(inputText: String)

    fun clear()
}
