# VoiceAssistant — Version 0.1

A lightweight Android foundation for a future voice-first assistant, optimized for low-end devices such as the Vivo Y01 running Android 12.

## Current version

Implemented:
- Kotlin Android application with a small platform-API-first architecture.
- Dark voice-assistant UI with IDLE, LISTENING, PROCESSING, SPEAKING, XPLORE, and ERROR states.
- Runtime microphone permission and Android SpeechRecognizer input.
- Android TextToSpeech output.
- Replaceable `AssistantEngine` abstraction with deterministic `LocalAssistantEngine` responses for greetings, name, time, sensors, and unknown input.
- In-memory assistant context.
- Dynamic SensorManager discovery and lifecycle-safe listener registration.
- Camera permission, Camera2 live preview, Xplore Mode entry/exit, and camera cleanup.
- `VisionProcessor`/`NullVisionProcessor` placeholder that explicitly does not claim vision AI.
- Unit tests for local engine behavior.

## Not implemented yet

No cloud LLM, API provider, camera frame upload, computer vision, continuous listening, wake word, background service, external hardware, persistence, or unrestricted device control is included.

## Build

Open the repository in Android Studio with an Android SDK installed, then run:

```bash
./gradlew test
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`. Physical-device testing is required for microphone, speech-service, TTS, sensor availability, and Camera2 behavior.

## Permissions

Only `RECORD_AUDIO` and `CAMERA` are declared. Both are requested at runtime only when the user activates the associated feature. Denial leaves other features usable.

## Architecture

`MainActivity` coordinates the UI; audio, sensor, camera, and assistant engine responsibilities are separated into small components. No secrets or API keys are stored in source code.

## Target

Vivo Y01 / Android 12 and comparable low-end Android devices. The app avoids continuous microphone/camera use, polling, and unnecessary background services.
