# Field AI — local text AI + camera overlay (ARM32)

This project contains a local language-model inference path based on `llama.cpp`, not canned replies. The app imports a `.gguf` model, loads it with native ARM32 code, accepts typed questions, runs inference locally, and displays generated answers as text over a live CameraX preview. It does not call a cloud AI API.

## Build the APK
Open **Actions → Build Field AI local-LLM APK → Run workflow** in this repository. When the workflow succeeds, download the `field-ai-local-ai-arm32-debug` artifact and install the APK on the Vivo Y15s.

The workflow downloads `llama.cpp` during the build. Model weights are not bundled in the APK.

## Model requirements
- Import a GGUF text-generation/instruct model supported by the current `llama.cpp` version.
- Start with a very small quantized model (around 0.5B parameters, Q4 quantization). Larger models can cause out-of-memory errors or be extremely slow on the Vivo Y15s.
- Importing copies the model into the app's private storage. Ensure enough free storage first.
- If loading fails, the model may be unsupported, incomplete, or too large for available memory.

## Honest feature status
**Implemented in source:** live camera preview, GGUF file import, native `llama.cpp` model loading, local text inference, and on-screen answer overlay. Once a model is imported, text inference is designed to work offline.

**Not implemented yet:** Whisper/offline voice input, camera-image understanding (the LLM does not analyse camera pixels), object detection, and true spatially anchored AR labels. The current overlay is a text console over the camera preview. There are no voice replies.

## Compatibility caveat
The project targets `armeabi-v7a` (32-bit ARM). Model support and memory use depend on the architecture and model. The GitHub Actions build must complete successfully before an APK exists; it has not yet been tested on the Vivo Y15s.
