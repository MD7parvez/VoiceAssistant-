package com.parvez.fieldai

object LocalLlm {
    init { System.loadLibrary("fieldai_llama") }
    external fun nativeLoad(path: String): Boolean
    external fun nativeAsk(prompt: String): String
    external fun nativeUnload()
}
