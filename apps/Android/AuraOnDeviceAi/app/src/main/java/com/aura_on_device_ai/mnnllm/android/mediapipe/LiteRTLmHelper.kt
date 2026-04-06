/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aura_on_device_ai.mnnllm.android.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch

private const val TAG = "LiteRTLmHelper"

/**
 * Helper for LiteRT-LM (New Google Edge AI SDK).
 * Adapted from official Google snippet for better multi-modal and fast inference support.
 */
class LiteRTLmHelper(
    private val context: Context,
    private val modelPath: String,
    private val maxTokens: Int = 2048,
    private val temperature: Float = 0.7f,
    private val topK: Int = 40,
    private val topP: Float = 0.9f
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isGenerating = false

    init {
        setupEngine()
    }

    @OptIn(ExperimentalApi::class)
    private fun setupEngine() {
        var lastError: Exception? = null
        
        // Try GPU first (for high performance)
        try {
            Log.d(TAG, "Attempting to initialize LiteRT-LM Engine on GPU: $modelPath")
            val gpuBackend = Backend.GPU()
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = gpuBackend,
                visionBackend = gpuBackend,
                maxNumTokens = maxTokens,
                cacheDir = context.cacheDir.absolutePath
            )
            val engineInstance = Engine(engineConfig)
            engineInstance.initialize()
            
            this.engine = engineInstance
            Log.d(TAG, "LiteRT-LM Engine initialized successfully on GPU")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize on GPU, trying CPU fallback: ${e.message}")
            lastError = e
        }

        // Fallback to CPU if GPU failed or was skipped
        if (this.engine == null) {
            try {
                Log.d(TAG, "Initializing LiteRT-LM Engine on CPU: $modelPath")
                val cpuBackend = Backend.CPU()
                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = cpuBackend,
                    visionBackend = cpuBackend,
                    maxNumTokens = maxTokens,
                    cacheDir = context.cacheDir.absolutePath
                )
                val engineInstance = Engine(engineConfig)
                engineInstance.initialize()
                
                this.engine = engineInstance
                Log.d(TAG, "LiteRT-LM Engine initialized successfully on CPU")
            } catch (e: Exception) {
                Log.e(TAG, "Final attempt to initialize LiteRT-LM Engine on CPU failed", e)
                throw lastError ?: e
            }
        }

        // Initialize conversation
        try {
            conversation = engine?.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble()
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            throw e
        }
    }

    /**
     * Generate response with support for multi-modality (Images)
     */
    fun generateBlocking(
        prompt: String,
        bitmap: Bitmap? = null,
        onToken: (delta: String, isEnd: Boolean) -> Boolean
    ) {
        val conv = conversation ?: run {
            onToken("Error: LiteRT-LM conversation NOT initialized", true)
            return
        }

        val latch = CountDownLatch(1)
        var stopRequested = false
        isGenerating = true

        val contents = mutableListOf<Content>()
        
        // Add image content if provided
        bitmap?.let {
            contents.add(Content.ImageBytes(it.toPngByteArray()))
        }
        
        // Add text content
        if (prompt.trim().isNotEmpty()) {
            contents.add(Content.Text(prompt))
        }

        try {
            conv.sendMessageAsync(
                Contents.of(contents),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        if (!stopRequested) {
                            val text = message.toString()
                            if (text.isNotEmpty()) {
                                if (onToken(text, false)) {
                                    stopRequested = true
                                    conv.cancelProcess()
                                }
                            }
                        }
                    }

                    override fun onDone() {
                        onToken("", true)
                        latch.countDown()
                    }

                    override fun onError(throwable: Throwable) {
                        if (throwable is CancellationException) {
                            Log.i(TAG, "Inference cancelled")
                            onToken("", true)
                        } else {
                            Log.e(TAG, "Inference error", throwable)
                            onToken("Error: ${throwable.message}", true)
                        }
                        latch.countDown()
                    }
                }
            )
            
            latch.await()
        } catch (e: Exception) {
            Log.e(TAG, "Generation stage failed: ${e.message}", e)
            onToken("Error: ${e.message}", true)
            latch.countDown()
        } finally {
            isGenerating = false
        }
    }

    /**
     * Add text to history for pre-seeding conversations
     */
    fun addQuery(text: String) {
        // In the new API, we typically send messages in turns.
        // For pre-seeding, we can send a system instruction or a combined first message.
        // LiteRT-LM manages history automatically within the Conversation object.
        // If we need to seed history from external storage:
        Log.w(TAG, "addQuery called - in LiteRT-LM history is managed via the Conversation object automatically.")
    }

    fun close() {
        if (isGenerating) {
            conversation?.cancelProcess()
            Thread.sleep(100) // Brief wait for cleanup
        }
        try {
            conversation?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LiteRT-LM components", e)
        }
        conversation = null
        engine = null
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
