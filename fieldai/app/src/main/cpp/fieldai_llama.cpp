#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>
#include "llama.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FieldAI", __VA_ARGS__)
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static llama_sampler* g_sampler = nullptr;
static std::mutex g_mutex;

static std::string jstring_to_string(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return out;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_parvez_fieldai_LocalLlm_nativeLoad(JNIEnv* env, jobject, jstring pathValue) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const std::string path = jstring_to_string(env, pathValue);
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    llama_backend_init();
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path.c_str(), mp);
    if (!g_model) { LOGE("Could not load model: %s", path.c_str()); return JNI_FALSE; }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 1024;
    cp.n_batch = 128;
    cp.n_threads = 2;
    cp.n_threads_batch = 2;
    g_ctx = llama_init_from_model(g_model, cp);
    if (!g_ctx) { llama_model_free(g_model); g_model = nullptr; return JNI_FALSE; }
    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(20));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.90f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_parvez_fieldai_LocalLlm_nativeAsk(JNIEnv* env, jobject, jstring promptValue) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_ctx || !g_sampler) return env->NewStringUTF("Load a compatible GGUF model first.");
    const std::string prompt = jstring_to_string(env, promptValue);
    std::vector<llama_token> tokens(2048);
    const int n = llama_tokenize(llama_model_get_vocab(g_model), prompt.c_str(), (int32_t)prompt.size(), tokens.data(), (int32_t)tokens.size(), true, true);
    if (n <= 0 || n >= (int)tokens.size()) return env->NewStringUTF("Prompt is too long for this model/context.");
    tokens.resize(n);
    llama_memory_clear(llama_get_memory(g_ctx), true);
    llama_sampler_reset(g_sampler);
    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)tokens.size());
    if (llama_decode(g_ctx, batch) != 0) return env->NewStringUTF("Model could not process the prompt. Try a smaller GGUF model.");
    std::string answer;
    answer.reserve(1200);
    for (int i = 0; i < 180; ++i) {
        llama_token token = llama_sampler_sample(g_sampler, g_ctx, -1);
        if (llama_vocab_is_eog(llama_model_get_vocab(g_model), token)) break;
        char piece[256];
        int pieceLen = llama_token_to_piece(llama_model_get_vocab(g_model), token, piece, sizeof(piece), 0, true);
        if (pieceLen > 0) answer.append(piece, pieceLen);
        if (answer.size() > 5000) break;
        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(g_ctx, batch) != 0) break;
    }
    return env->NewStringUTF(answer.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_parvez_fieldai_LocalLlm_nativeUnload(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}
