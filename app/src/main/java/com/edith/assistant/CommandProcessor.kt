package com.edith.assistant

sealed class EdithAction {
    data class Speak(val text: String) : EdithAction()
    data class SpeakAndOpenCamera(val text: String) : EdithAction()
    data class SpeakAndOpenGallery(val text: String) : EdithAction()
    data class SpeakAndOpenBrowser(val text: String, val query: String? = null) : EdithAction()
}

/**
 * Turns raw recognized speech into an action.
 * Covers the "Apps" feature list from the notes: camera, gallery, browser,
 * plus the memory feature ("remember X is Y" / "what is X").
 */
class CommandProcessor(private val memory: MemoryManager, private val personaName: String) {

    fun process(rawInput: String): EdithAction {
        val input = rawInput.trim().lowercase()

        return when {
            input.isEmpty() ->
                EdithAction.Speak("I didn't catch that.")

            input.contains("open camera") || input == "camera" ->
                EdithAction.SpeakAndOpenCamera("Opening camera.")

            input.contains("open gallery") || input.contains("show my photos") || input == "gallery" ->
                EdithAction.SpeakAndOpenGallery("Opening your gallery.")

            input.contains("open browser") || input.contains("search for") ->
                EdithAction.SpeakAndOpenBrowser(
                    "Searching now.",
                    input.substringAfter("search for", "").trim().ifEmpty { null }
                )

            input.startsWith("remember ") && input.contains(" is ") -> {
                val body = input.removePrefix("remember ").trim()
                val key = body.substringBefore(" is ").trim()
                val value = body.substringAfter(" is ").trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    memory.remember(key, value)
                    EdithAction.Speak("Got it. I'll remember that $key is $value.")
                } else {
                    EdithAction.Speak("Tell me what to remember, like: remember my flight is at 6.")
                }
            }

            input.startsWith("what is ") || input.startsWith("what's ") -> {
                val key = input.removePrefix("what is ").removePrefix("what's ").trim()
                val value = memory.recall(key)
                if (value != null) EdithAction.Speak("$key is $value.")
                else EdithAction.Speak("I don't have anything saved about $key yet.")
            }

            input.contains("forget") -> {
                val key = input.substringAfter("forget").trim()
                memory.forget(key)
                EdithAction.Speak("Okay, I've forgotten that.")
            }

            input.contains("who are you") || input.contains("your name") ->
                EdithAction.Speak("I'm $personaName, your assistant.")

            else ->
                EdithAction.Speak("I heard: $rawInput. I'm still learning that command.")
        }
    }
}
