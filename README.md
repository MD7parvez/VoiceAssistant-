# VoiceAssistant — Version 0.2

## Real AI integration

Version 0.2 connects the voice/text assistant to the Gemini API through a lightweight REST client.

- Voice: SpeechRecognizer → Gemini → on-screen response → optional TTS
- Text: typed message → Gemini → on-screen response → optional TTS

**Text output is always the primary fallback.** If TTS is unavailable, the AI answer remains visible.

## Configure Gemini

Open the app and tap **AI SETTINGS**, then enter a Gemini API key. The key is stored in the app's private preferences and is never written to the repository.

Treat API keys as secrets. For production, do not hard-code a provider credential into a mobile APK; use a server-side backend/proxy.

The prototype uses the supported \`generateContent\` REST endpoint to keep the Android app lightweight.

## Build

Run:

\`\`\`bash
./gradlew test
./gradlew assembleDebug
\`\`\`

Expected APK:

\`app/build/outputs/apk/debug/app-debug.apk\`

The GitHub Actions workflow builds a debug APK on pushes to \`main\` and manual workflow runs.

## Implemented

- Real Gemini text generation.
- Bounded in-memory conversation history.
- Voice input through Android SpeechRecognizer.
- Typed-message fallback.
- Text response displayed before TTS is attempted.
- TTS with graceful fallback.
- Runtime microphone and camera permissions.
- Dynamic sensor discovery.
- Camera2 Xplore preview foundation.
- Local fallback engine when Gemini is not configured or available.
- Network/API error handling without crashing the app.

## Current limitations

- Gemini requires internet access.
- A Gemini API key must be configured on the test device.
- The prototype stores the key locally on-device; this is not production-grade secret management.
- Xplore currently provides camera preview only; camera-frame AI vision is not connected yet.
- The app has no always-on microphone or background listening.
- Physical Vivo Y01 testing is still required.

## Security note

Never commit an API key to GitHub. If a key is exposed, revoke/replace it. For a production release, move Gemini requests behind a server-side backend so the mobile APK does not contain the provider credential.
