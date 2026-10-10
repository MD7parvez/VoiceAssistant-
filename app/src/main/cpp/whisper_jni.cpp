#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_md7parvez_voiceassistant_WhisperNative_transcribe(
        JNIEnv *env, jclass,
        jstring model_path_j,
        jfloatArray audio_j,
        jstring language_j) {
    if (model_path_j == nullptr || audio_j == nullptr || language_j == nullptr) {
        return env->NewStringUTF("ERROR: Missing model, audio, or language.");
    }

    const char *model_path_c = env->GetStringUTFChars(model_path_j, nullptr);
    const char *language_c = env->GetStringUTFChars(language_j, nullptr);
    std::string model_path(model_path_c ? model_path_c : "");
    std::string language(language_c ? language_c : "auto");
    if (model_path_c) env->ReleaseStringUTFChars(model_path_j, model_path_c);
    if (language_c) env->ReleaseStringUTFChars(language_j, language_c);

    const jsize count = env->GetArrayLength(audio_j);
    if (count < 1600) return env->NewStringUTF("ERROR: Recording too short. Hold SPEAK for longer.");

    std::vector<jfloat> samples(static_cast<size_t>(count));
    env->GetFloatArrayRegion(audio_j, 0, count, samples.data());

    whisper_context_params context_params = whisper_context_default_params();
    context_params.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(model_path.c_str(), context_params);
    if (ctx == nullptr) return env->NewStringUTF("ERROR: Could not load this Whisper model file. Use a whisper.cpp ggml model.");

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.translate = false;
    params.no_context = true;
    params.n_threads = 2;
    params.language = language == "auto" ? "auto" : language.c_str();

    std::string result;
    const int status = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));
    if (status == 0) {
        const int segments = whisper_full_n_segments(ctx);
        for (int i = 0; i < segments; ++i) {
            const char *text = whisper_full_get_segment_text(ctx, i);
            if (text != nullptr) result += text;
        }
    } else {
        result = "ERROR: Whisper transcription failed.";
    }

    whisper_free(ctx);
    if (result.empty()) result = "ERROR: No speech detected. Try again closer to the microphone.";
    return env->NewStringUTF(result.c_str());
}
