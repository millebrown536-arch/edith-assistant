 package com.edith.assistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class EdithAction {
    data class Speak(val text: String) : EdithAction()
    data class SpeakAndOpenCamera(val text: String) : EdithAction()
    data class SpeakAndOpenGallery(val text: String) : EdithAction()
    data class SpeakAndOpenBrowser(val text: String, val query: String? = null) : EdithAction()
}

class CommandProcessor(private val memory: MemoryManager, private val personaName: String) {

    private fun any(input: String, vararg words: String) = words.any { input.contains(it) }

    fun process(rawInput: String): EdithAction {
        val input = rawInput.trim().lowercase()

        return when {
            input.isEmpty() ->
                EdithAction.Speak("I didn't catch that, please try again.")

            any(input, "camera", "kaimara", "photo khींch", "photo khinch", "picture le") ->
                EdithAction.SpeakAndOpenCamera("Opening camera.")

            any(input, "gallery", "photos", "tasveer", "tasveerein", "pictures dikha", "gallary") ->
                EdithAction.SpeakAndOpenGallery("Opening your gallery.")

            any(input, "browser", "internet", "search for", "search", "google", "chrome khol", "browser khol") -> {
                val query = when {
                    input.contains("search for") -> input.substringAfter("search for").trim().ifEmpty { null }
                    input.contains("search") -> input.substringAfter("search").trim().ifEmpty { null }
                    else -> null
                }
                EdithAction.SpeakAndOpenBrowser(
                    if (query != null) "Searching for $query." else "Opening browser.",
                    query
                )
            }

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
            (any(input, "yaad rakho", "yaad rakhna") && input.contains(" hai")) -> {
                val body = input.substringAfter("yaad rakho").ifEmpty { input.substringAfter("yaad rakhna") }.trim()
                val key = body.substringBefore(" hai").trim()
                val value = body.substringAfter(" hai").trim()
                if (key.isNotEmpty()) {
                    memory.remember(key, value.ifEmpty { "haan" })
                    EdithAction.Speak("Theek hai, mujhe yaad rahega ki $key hai $value.")
                } else {
                    EdithAction.Speak("Kya yaad rakhna hai, bolo jaise: yaad rakho meri flight 6 baje hai.")
                }
            }

            input.startsWith("what is ") || input.startsWith("what's ") -> {
                val key = input.removePrefix("what is ").removePrefix("what's ").trim()
                val value = memory.recall(key)
                if (value != null) EdithAction.Speak("$key is $value.")
                else EdithAction.Speak("I don't have anything saved about $key yet.")
            }
            any(input, "kya hai", "kya tha") -> {
                val key = input.substringBefore("kya").trim()
                val value = memory.recall(key)
                if (value != null) EdithAction.Speak("$key $value hai.")
                else EdithAction.Speak("Mujhe $key ke baare me kuch pata nahi hai.")
            }

            input.contains("forget") -> {
                val key = input.substringAfter("forget").trim()
                memory.forget(key)
                EdithAction.Speak("Okay, I've forgotten that.")
            }
            input.contains("bhool jao") -> {
                val key = input.substringBefore("bhool jao").trim()
                memory.forget(key)
                EdithAction.Speak("Theek hai, main bhool gayi.")
            }

            any(input, "who are you", "your name", "tum kaun ho", "tera naam kya hai", "aap kaun ho") ->
                EdithAction.Speak("I'm $personaName, your assistant.")

            any(input, "hello", "hi ", "hey edith", "hey vyro", "namaste", "namaskar") ->
                EdithAction.Speak("Hello! How can I help you?")
            any(input, "how are you", "kaise ho", "kaisi ho", "kaise hain aap") ->
                EdithAction.Speak("I'm doing great, thanks for asking! What can I do for you?")
            any(input, "thank you", "thanks", "shukriya", "dhanyawad") ->
                EdithAction.Speak("You're welcome!")

            any(input, "what time", "current time", "samay kya", "time kya", "kitne baje") -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                EdithAction.Speak("It's $time right now.")
            }
            any(input, "what date", "today's date", "aaj ki tareekh", "aaj kya date") -> {
                val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                EdithAction.Speak("Today is $date.")
            }

            any(input, "help", "what can you do", "commands", "madad", "kya kya kar sakti ho") ->
                EdithAction.Speak(
                    "You can say: open camera, open gallery, open browser, search for something, " +
                    "remember something is something, what is something, what time is it, or what's today's date."
                )

            else ->
                EdithAction.Speak("I heard: $rawInput. Say help to hear what I can do.")
        }
    }
}               
