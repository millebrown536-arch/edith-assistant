package com.edith.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edith.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeechHelper
    private lateinit var memory: MemoryManager
    private lateinit var commandProcessor: CommandProcessor
    private var recognizer: SpeechRecognizer? = null

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
        tts = TextToSpeechHelper(this) {
            val name = if (tts.isMale) "Vyro" else "Edith"
            commandProcessor = CommandProcessor(memory, name)
        }
        commandProcessor = CommandProcessor(memory, "Edith")

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

        // If launched by the wake-word service, jump straight into active listening
        if (intent.getBooleanExtra(EXTRA_AUTO_LISTEN, false)) {
            binding.root.post { startActiveListening() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_LISTEN, false)) {
            startActiveListening()
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

    /** Opens the on-screen orb + listens for one command, like the wake-up screen from the sketch. */
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
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull().orEmpty()
                    handleHeard(heard)
                }
                override fun onError(error: Int) {
                    binding.statusText.text = getString(R.string.wake_prompt)
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            }
            startListening(intent)
        }
    }

    private fun handleHeard(heard: String) {
        binding.responseText.text = heard
        val action = commandProcessor.process(heard)
        when (action) {
            is EdithAction.Speak -> {
                tts.speak(action.text)
                binding.statusText.text = getString(R.string.wake_prompt)
            }
            is EdithAction.SpeakAndOpenCamera -> tts.speak(action.text) {
                startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            }
            is EdithAction.SpeakAndOpenGallery -> tts.speak(action.text) {
                startActivity(Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
            }
            is EdithAction.SpeakAndOpenBrowser -> tts.speak(action.text) {
                val uri = if (action.query != null)
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(action.query))
                else Uri.parse("https://www.google.com")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AUTO_LISTEN = "auto_listen"
    }
}
