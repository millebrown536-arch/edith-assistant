# Edith — Voice Assistant (Android Prototype)

Based on your notebook design: wake-word activated assistant, calm/smooth
voice (two personas — **Edith** female / **Vyro** male), on-screen orb
display, persistent memory, and quick-launch for camera / gallery / browser.

## What's actually built (working code, not stubs)
- **Wake-word listening service** (`WakeWordService.kt`) — runs in the
  background, listens for "Edith" or "Vyro", opens the app when it hears it.
- **Main screen** (`MainActivity.kt` + `activity_main.xml`) — the cyan orb
  UI from your sketch. Tap it (or say the wake word) to give a command.
- **Two voices** (`TextToSpeechHelper.kt`) — toggle buttons switch between
  Edith (female) and Vyro (male), both tuned calm/smooth (lower speech
  rate, not the fast robotic default).
- **Memory** (`MemoryManager.kt`) — say "remember [something] is [value]",
  later ask "what is [something]" and it recalls it. Saved permanently on
  the phone.
- **Commands** (`CommandProcessor.kt`) — "open camera", "open gallery",
  "open browser" / "search for ___" — matches your "Apps" feature list.

## Important honest note on the wake word
Android doesn't give apps a free always-on, battery-cheap wake word out of
the box. This prototype uses the built-in `SpeechRecognizer` in a
listen → pause → re-listen loop, which **works** but drains battery faster
and needs a live internet connection (it uses Google's speech service).
For a real always-on low-power wake word (what a finished glasses product
would need), swap `WakeWordService.kt` to use an offline SDK like
**Picovoice Porcupine** (free tier available) — everything else in this
project (the screen, the voice, the memory, the commands) stays exactly
the same, you'd just change how the wake word is detected.

## How to build it on your phone
1. Install **Android Studio** (free) on a PC/laptop: https://developer.android.com/studio
2. Unzip this project, then in Android Studio: **File → Open** → select the
   `EdithAssistant` folder.
3. Let it sync (first time it downloads Gradle — needs internet once).
4. Plug in your Android phone via USB, enable **Developer Options → USB
   debugging** on the phone (Settings → About Phone → tap "Build number"
   7 times → Developer Options appears).
5. Click the green **Run ▶** button in Android Studio, pick your phone.
6. App installs. First launch it'll ask for microphone + notification
   permission — allow both.

No PC available? You can also zip the `app/` folder and use an online
Android build service, or ask any local dev to build it — the code itself
is complete and ready to compile.

## Next steps (once this basic version works)
- Real wake-word engine (Porcupine) for proper always-on + battery life
- Custom trained voice for Edith/Vyro instead of default Android TTS
- Connect this same app logic to actual AR glasses hardware later
