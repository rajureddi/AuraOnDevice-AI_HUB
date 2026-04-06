package com.aura_on_device_ai.mnnllm.android.llm

import android.content.Context
import android.util.Log
import com.aura_on_device_ai.mnnllm.android.chat.model.ChatDataItem
import com.aura_on_device_ai.mnnllm.android.mediapipe.LiteRTLmHelper
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeLlmHelper
import com.aura_on_device_ai.mnnllm.android.model.ModelTypeUtils

class MediaPipeChatSession(
    private val context: Context,
    private val modelId: String,
    override val sessionId: String,
    private val modelPath: String,
    private val initialHistory: List<ChatDataItem>? = null
) : ChatSession {
    private val TAG = "MediaPipeChatSession"
    private var modernHelper: LiteRTLmHelper? = null
    private var legacyHelper: MediaPipeLlmHelper? = null
    private var history = initialHistory?.toMutableList() ?: mutableListOf()
    private var keepHistory = true

    override val debugInfo: String
        get() = "Engine: LiteRT-LM (Google)\nModel: $modelId"

    override var supportOmni: Boolean = false

    override fun load() {
        if (ModelTypeUtils.isLiteRTLm(modelPath)) {
            try {
                Log.d(TAG, "Loading modern LiteRT-LM engine for $modelPath")
                modernHelper = LiteRTLmHelper(context, modelPath)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load via modern engine, falling back to legacy for $modelPath", e)
            }
        }
        
        Log.d(TAG, "Loading legacy MediaPipe Task engine for $modelPath")
        legacyHelper = MediaPipeLlmHelper(context, modelPath)
    }

    override fun generate(
        prompt: String,
        params: Map<String, Any>,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        // Add user prompt to history (maintaining UI state)
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

            // Format only the NEW turn. Since we use useSession = true, past history is already in KV-cache!
            val isGemma = modelId.contains("gemma", ignoreCase = true)
            val isLlama = modelId.contains("llama", ignoreCase = true)
            val isQwen = modelId.contains("qwen", ignoreCase = true)
            val newTurnPrompt: String
            
            if (isGemma) {
                newTurnPrompt = "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"
            } else if (isLlama) {
                newTurnPrompt = "<|start_header_id|>user<|end_header_id|>\n\n$prompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            } else if (isQwen) {
                newTurnPrompt = "<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
            } else {
                newTurnPrompt = prompt // Fallback for unknown models
            }

            Log.d(TAG, "Inference prompt: $newTurnPrompt")

            // Execute via the active engine
            if (modernHelper != null) {
                modernHelper!!.generateBlocking(newTurnPrompt, bitmap) { delta, isEnd ->
                    handleToken(delta, isEnd, responseBuilder, progressListener)
                }
            } else if (legacyHelper != null) {
                legacyHelper!!.generateBlocking(newTurnPrompt, bitmap) { delta, isEnd ->
                    handleToken(delta, isEnd, responseBuilder, progressListener)
                }
            } else {
                Log.e(TAG, "No inference engine loaded")
                progressListener.onProgress(null)
                result["error"] = true
                result["message"] = "No inference engine loaded"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            progressListener.onProgress(null)
            result["error"] = true
            result["message"] = e.message ?: "Unknown error"
        }

        return result
    }

    private fun handleToken(
        delta: String,
        isEnd: Boolean,
        responseBuilder: StringBuilder,
        progressListener: GenerateProgressListener
    ): Boolean {
        if (isEnd) {
            val response = responseBuilder.toString()
            if (keepHistory && response.isNotEmpty() && (history.isEmpty() || history.last().text != response)) {
                history.add(ChatDataItem(null, com.aura_on_device_ai.mnnllm.android.chat.chatlist.ChatViewHolders.ASSISTANT, response))
            }
            progressListener.onProgress(null)
            return false
        } else if (delta.isNotEmpty()) {
            val sanitizedDelta = delta.replace("<start_of_turn>", "")
                .replace("<end_of_turn>", "").replace("<|eot_id|>", "")
                .replace("<|im_end|>", "").replace("<im_end>", "").replace("<eos>", "")
                .replace("<|start_header_id|>", "").replace("<|end_header_id|>", "")
            
            if (sanitizedDelta.isNotEmpty()) {
                responseBuilder.append(sanitizedDelta)
                return progressListener.onProgress(sanitizedDelta)
            }
        }
        return false
    }

    override fun reset(): String {
        history.clear()
        return sessionId
    }

    override fun release() {
        modernHelper?.close()
        modernHelper = null
        legacyHelper?.close()
        legacyHelper = null
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

