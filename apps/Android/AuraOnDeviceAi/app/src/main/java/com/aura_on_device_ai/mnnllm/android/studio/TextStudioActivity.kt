package com.aura_on_device_ai.mnnllm.android.studio

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aura_on_device_ai.mnnllm.android.R
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeLanguageDetectorHelper
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeTextClassifierHelper
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeTextEmbedderHelper

class TextStudioActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private lateinit var etInputTwo: EditText
    private lateinit var cardInputTwo: android.view.View
    private lateinit var tvOutput: TextView
    private lateinit var btnAnalyze: Button
    
    private var langDetector: MediaPipeLanguageDetectorHelper? = null
    private var textClassifier: MediaPipeTextClassifierHelper? = null
    private var textEmbedder: MediaPipeTextEmbedderHelper? = null
    
    private var taskType: String = ""
    private var modelPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_studio)

        etInput = findViewById(R.id.et_input)
        etInputTwo = findViewById(R.id.et_input_two)
        cardInputTwo = findViewById(R.id.card_input_two)
        tvOutput = findViewById(R.id.tv_output)
        btnAnalyze = findViewById(R.id.btn_analyze)

        val taskName = intent.getStringExtra("taskName") ?: "Text Task"
        taskType = intent.getStringExtra("modelId") ?: ""
        modelPath = intent.getStringExtra("modelPath") ?: ""

        if (taskType == "Embedder") {
            cardInputTwo.visibility = android.view.View.VISIBLE
            btnAnalyze.text = "COMPARE TEXTS"
        }

        findViewById<TextView>(R.id.tv_studio_title).text = taskName

        initModel()

        btnAnalyze.setOnClickListener {
            val input = etInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            } else {
                runInference(input)
            }
        }
    }

    private fun initModel() {
        if (modelPath.isEmpty()) return
        
        try {
            when (taskType) {
                "Lang-Detector" -> langDetector = MediaPipeLanguageDetectorHelper(this, modelPath)
                "Text-Classifier" -> textClassifier = MediaPipeTextClassifierHelper(this, modelPath)
                "Embedder" -> textEmbedder = MediaPipeTextEmbedderHelper(this, modelPath)
            }
        } catch (e: Exception) {
            Log.e("TextStudio", "Model init failed", e)
            tvOutput.text = "Error initializing model: ${e.message}"
        }
    }

    private fun runInference(text: String) {
        tvOutput.text = "Analyzing..."
        
        Thread {
            try {
                when (taskType) {
                    "Lang-Detector" -> {
                        langDetector?.detect(text) { result, time ->
                            runOnUiThread {
                                val top = result?.languagesAndScores()?.firstOrNull()
                                val output = if (top != null) {
                                    "Inference Time: ${time}ms\n\nDetected Language: ${top.languageCode()} (Score: %.2f)".format(top.probability())
                                } else "No language detected."
                                tvOutput.text = output
                            }
                        }
                    }
                    "Text-Classifier" -> {
                        textClassifier?.classify(text) { classifications, time ->
                            runOnUiThread {
                                if (classifications != null && classifications.isNotEmpty()) {
                                    val resultText = classifications.flatMap { it.categories() }.joinToString("\n") { 
                                        val label = if (modelPath.contains("wordvec", true)) {
                                            if (it.categoryName() == "1") "Positive" else "Negative"
                                        } else it.categoryName()
                                        "$label: %.2f".format(it.score())
                                    }
                                    tvOutput.text = "Inference Time: ${time}ms\n\n$resultText"
                                } else {
                                    tvOutput.text = "No classification found."
                                }
                            }
                        }
                    }
                    "Embedder" -> {
                        val firstText = etInput.text.toString().trim()
                        val secondText = etInputTwo.text.toString().trim()
                        
                        val embedder = textEmbedder
                        if (embedder == null) {
                            runOnUiThread { tvOutput.text = "Error: Text Embedder is not initialized." }
                            return@Thread
                        }

                        // Perform Embedding 1
                        val start1 = SystemClock.uptimeMillis()
                        val emb1 = embedder.embed(firstText)
                        val time1 = SystemClock.uptimeMillis() - start1
                        val vec1 = emb1?.embeddingResult()?.embeddings()?.firstOrNull()
                        
                        if (secondText.isNotEmpty()) {
                            // Perform Embedding 2
                            val start2 = SystemClock.uptimeMillis()
                            val emb2 = embedder.embed(secondText)
                            val time2 = SystemClock.uptimeMillis() - start2
                            val vec2 = emb2?.embeddingResult()?.embeddings()?.firstOrNull()
                            
                            runOnUiThread {
                                if (vec1 != null && vec2 != null) {
                                    val similarity = com.google.mediapipe.tasks.text.textembedder.TextEmbedder.cosineSimilarity(vec1, vec2)
                                    val v1Str = vec1.floatEmbedding().take(6).joinToString(", ") { "%.3f".format(it) }
                                    val v2Str = vec2.floatEmbedding().take(6).joinToString(", ") { "%.3f".format(it) }
                                    
                                    val output = """
                                        INPUT 1:
                                        Inference: ${time1}ms
                                        Vector: [$v1Str...]
                                        
                                        INPUT 2:
                                        Inference: ${time2}ms
                                        Vector: [$v2Str...]
                                        
                                        ---------------------------
                                        AI COSINE SIMILARITY: %.4f
                                    """.trimIndent().format(similarity)
                                    
                                    tvOutput.text = output
                                } else {
                                    tvOutput.text = "Error: Failed to generate one or more embeddings.\nCheck if the model file is corrupted."
                                }
                            }
                        } else {
                            // Single embedding mode
                            runOnUiThread {
                                if (vec1 != null) {
                                    val v1Str = vec1.floatEmbedding().take(10).joinToString(", ") { "%.4f".format(it) }
                                    tvOutput.text = "Inference: ${time1}ms\nSize: ${vec1.floatEmbedding().size}\nVector: [$v1Str...]\n\nTip: Fill box 2 to compare."
                                } else {
                                    tvOutput.text = "Embedding failed. The model might not support this input."
                                }
                            }
                        }
                    }
                    else -> runOnUiThread { tvOutput.text = "Unknown Task type." }
                }
            } catch (e: Exception) {
                runOnUiThread { tvOutput.text = "Error: ${e.message}" }
            }
        }.start()
    }

    private fun clearHelpers() {
        langDetector?.close()
        langDetector = null
        textClassifier?.close()
        textClassifier = null
        textEmbedder?.close()
        textEmbedder = null
    }

    override fun onStop() {
        super.onStop()
        clearHelpers()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearHelpers()
    }
}

