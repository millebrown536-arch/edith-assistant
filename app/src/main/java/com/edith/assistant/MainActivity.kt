package com.edith.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.edith.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeechHelper
    private lateinit var memory: MemoryManager
    private lateinit var commandProcessor: CommandProcessor
    private var recognizer: SpeechRecognizer? = null
    private val followUpHandler = Handler(Looper.getMainLooper())
    private lateinit var audioManager: AudioManager

    private fun muteBeep(mute: Boolean) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        } catch (_: Exception) { }
    }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.RECORD_AUDIO] == true) {
            startWakeService()
        } else {
            Toast.makeText(this, "Mic permission is needed for Edith to hear you.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memory = MemoryManager(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        commandProcessor = CommandProcessor(memory, "Edith")
        tts = TextToSpeechHelper(this) {
            val name = if (tts.isMale) "Vyro" else "Edith"
            commandProcessor = CommandProcessor(memory, name)
        }

        binding.orbButton.setOnClickListener { startActiveListening() }

        binding.btnEdithVoice.setOnClickListener {
            tts.setPersona(male = false)
            commandProcessor = CommandProcessor(memory, "Edith")
            binding.statusText.text = "Persona: Edith"
        }
        binding.btnVyroVoice.setOnClickListener {
            tts.setPersona(male = true)
            commandProcessor = CommandProcessor(memory, "Vyro")
            binding.statusText.text = "Persona: Vyro"
        }

        requestPermissionsAndStart()
        handleLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent) {
        val directCommand = intent.getStringExtra(EXTRA_COMMAND)
        val autoListen = intent.getBooleanExtra(EXTRA_AUTO_LISTEN, false)
        when {
            !directCommand.isNullOrBlank() -> binding.root.post { handleHeard(directCommand) }
            autoListen -> binding.root.post { startActiveListening() }
        }
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startWakeService()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startWakeService() {
        val svcIntent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, svcIntent)
    }

    private fun startActiveListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device.", Toast.LENGTH_LONG).show()
            return
        }
        binding.statusText.text = getString(R.string.listening)
        binding.responseText.text = ""

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    muteBeep(false)
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull().orEmpty()
                    if (heard.isNotBlank()) handleHeard(heard)
                    else binding.statusText.text = getString(R.string.wake_prompt)
                }
                override fun onError(error: Int) {
                    muteBeep(false)
                    binding.statusText.text = getString(R.string.wake_prompt)
                }
                override fun onReadyForSpeech(params: Bundle?) { muteBeep(true) }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { muteBeep(false) }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 15000)
                putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2500)
                putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2500)
            }
            try {
                muteBeep(true)
                startListening(intent)
            } catch (e: Exception) {
                muteBeep(false)
            }
        }
    }

    private fun handleHeard(heard: String) {
        binding.responseText.text = heard
        val action = commandProcessor.process(heard)
        when (action) {
            is EdithAction.Speak -> {
                tts.speak(action.text) { scheduleFollowUpListen() }
                binding.statusText.text = getString(R.string.wake_prompt)
            }
            is EdithAction.SpeakAndOpenCamera -> tts.speak(action.text) {
                try {
                    startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                } catch (e: Exception) {
                    Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
                }
                scheduleFollowUpListen()
            }
            is EdithAction.SpeakAndOpenGallery -> tts.speak(action.text) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                } catch (e: Exception) {
                    Toast.makeText(this, "No gallery app found.", Toast.LENGTH_SHORT).show()
                }
                scheduleFollowUpListen()
            }
            is EdithAction.SpeakAndOpenBrowser -> tts.speak(action.text) {
                try {
                    val uri = if (action.query != null)
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(action.query))
                    else Uri.parse("https://www.google.com")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(this, "No browser app found.", Toast.LENGTH_SHORT).show()
                }
                scheduleFollowUpListen()
            }
        }
    }

    private fun scheduleFollowUpListen() {
        binding.statusText.text = "Anything else? (I'm listening)"
        followUpHandler.postDelayed({
            if (!isFinishing) startActiveListening()
        }, 300)
    }

    override fun onDestroy() {
        muteBeep(false)
        followUpHandler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AUTO_LISTEN = "auto_listen"
        const val EXTRA_COMMAND = "direct_command"
    }
}
