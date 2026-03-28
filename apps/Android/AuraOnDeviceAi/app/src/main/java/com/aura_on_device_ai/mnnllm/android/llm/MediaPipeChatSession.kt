package com.aura_on_device_ai.mnnllm.android.llm

import android.content.Context
import android.util.Log
import com.aura_on_device_ai.mnnllm.android.chat.model.ChatDataItem
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeLlmHelper

class MediaPipeChatSession(
    private val context: Context,
    private val modelId: String,
    override val sessionId: String,
    private val modelPath: String,
    private val initialHistory: List<ChatDataItem>? = null
) : ChatSession {
    private val TAG = "MediaPipeChatSession"
    private var llmHelper: MediaPipeLlmHelper? = null
    private var history = initialHistory?.toMutableList() ?: mutableListOf()
    private var keepHistory = true

    override val debugInfo: String
        get() = "Engine: LiteRT-LM (Google)\nModel: $modelId"

    override var supportOmni: Boolean = false

    override fun load() {
        Log.d(TAG, "Loading LiteRT-LM model from $modelPath")
        llmHelper = MediaPipeLlmHelper(context, modelPath)
        
        // Seed the session with existing history if any
        history.forEach { item ->
            val text = item.text ?: return@forEach
            if (item.type == com.aura_on_device_ai.mnnllm.android.chat.chatlist.ChatViewHolders.USER) {
                llmHelper?.addQuery(text)
            } else if (item.type == com.aura_on_device_ai.mnnllm.android.chat.chatlist.ChatViewHolders.ASSISTANT) {
                llmHelper?.addResponse(text)
            }
        }
    }

    override fun generate(
        prompt: String,
        params: Map<String, Any>,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        val helper = llmHelper ?: run {
            return hashMapOf("error" to true, "message" to "LiteRT-LM model NOT loaded")
        }

        // Add user prompt to history
        if (keepHistory) {
            history.add(ChatDataItem(null, com.aura_on_device_ai.mnnllm.android.chat.chatlist.ChatViewHolders.USER, prompt))
        }

        val result = HashMap<String, Any>()
        val responseBuilder = StringBuilder()

        try {
            val imageUri = params["image"] as? android.net.Uri
            val bitmap: android.graphics.Bitmap? = if (imageUri != null) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode image URI", e)
                    null
                }
            } else null

            helper.generateBlocking(prompt, bitmap) { delta, isEnd ->
                if (isEnd) {
                    val response = responseBuilder.toString()
                    if (keepHistory && response.isNotEmpty()) {
                        history.add(ChatDataItem(null, com.aura_on_device_ai.mnnllm.android.chat.chatlist.ChatViewHolders.ASSISTANT, response))
                    }
                    progressListener.onProgress(null)
                    result["response"] = response
                } else if (delta.isNotEmpty()) {
                    responseBuilder.append(delta)
                    val stop = progressListener.onProgress(delta)
                    if (stop) return@generateBlocking true
                }
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            progressListener.onProgress(null)
            result["error"] = true
            result["message"] = e.message ?: "Unknown error"
        }

        return result
    }

    override fun reset(): String {
        history.clear()
        return sessionId
    }

    override fun release() {
        llmHelper?.close()
        llmHelper = null
    }

    override fun setKeepHistory(keepHistory: Boolean) {
        this.keepHistory = keepHistory
    }

    override fun setEnableAudioOutput(enable: Boolean) {
        // Not supported for LiteRT-LM
    }

    override fun getHistory(): List<ChatDataItem>? = history

    override fun setHistory(history: List<ChatDataItem>?) {
        this.history = history?.toMutableList() ?: mutableListOf()
    }

    override fun updateThinking(thinking: Boolean) {
        // LiteRT-LM models don't support explicit thinking mode
    }
}

