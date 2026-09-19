# VoiceAssistant — Version 0.1

## Build status

The repository contains a lightweight Kotlin Android application. Build verification must be run in an Android/Gradle environment with Android SDK platform 34 installed:

```bash
./gradlew test
./gradlew assembleDebug
```

The expected debug APK path is `app/build/outputs/apk/debug/app-debug.apk`. This repository environment does not provide a local Gradle execution tool, so command results and APK generation still require verification in Android Studio or a configured CI runner.

## Implemented

- Dark voice-assistant UI with explicit IDLE, LISTENING, PROCESSING, SPEAKING, XPLORE, and ERROR states.
- Runtime `RECORD_AUDIO` and `CAMERA` permissions.
- Android `SpeechRecognizer` input with availability, timeout, network, busy, permission, and empty-result handling.
- Android TextToSpeech with initialization, language, empty-input, repeat, and shutdown handling.
- Replaceable `AssistantEngine` and deterministic offline `LocalAssistantEngine`.
- Dynamic sensor discovery with opt-in listener registration and cleanup.
- Camera2 preview foundation and Xplore Mode entry/exit.
- Explicit `NullVisionProcessor`; real AI vision is not implemented.
- JVM unit tests for the local engine and state transitions.

## Physical testing required

Vivo Y01 / Android 12 testing is still required for microphone permission behavior, the installed speech service, TTS language availability, camera hardware and rotation, sensor availability, UI responsiveness, memory use, and battery impact.

## Known limitations

There is no cloud LLM, API provider, persistent conversation storage, wake word, always-on microphone/camera, frame upload, computer vision, background service, or external hardware support. The camera preview remains hosted by the initial activity implementation and should be tested on-device for configuration changes.

## Next step

After build and physical-device verification, add the AI/LLM integration behind `AssistantEngine`; do not couple a provider to the Android UI.
