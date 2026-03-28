package com.aura_on_device_ai.mnnllm.android.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aura_on_device_ai.mnnllm.android.R
import com.aura_on_device_ai.mnnllm.android.mediapipe.MediaPipeAudioClassifierHelper
import kotlin.concurrent.thread

class AudioStudioActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnAction: Button
    private lateinit var pbLevel: ProgressBar
    
    private var audioClassifier: MediaPipeAudioClassifierHelper? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var modelPath: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startRecording()
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_studio)

        tvOutput = findViewById(R.id.tv_audio_output)
        tvStatus = findViewById(R.id.tv_listening_status)
        btnAction = findViewById(R.id.btn_detect_audio)
        pbLevel = findViewById(R.id.pb_audio_level)

        modelPath = intent.getStringExtra("modelPath") ?: ""
        val taskName = intent.getStringExtra("taskName") ?: "Audio Studio"
        findViewById<TextView>(R.id.tv_studio_title).text = taskName

        initModel()

        btnAction.setOnClickListener {
            if (isRecording) stopRecording()
            else checkPermissionAndStart()
        }
    }

    private fun initModel() {
        if (modelPath.isEmpty()) return
        try {
            audioClassifier = MediaPipeAudioClassifierHelper(this, modelPath)
        } catch (e: Exception) {
            Log.e("AudioStudio", "Model error", e)
            tvOutput.text = "Error loading model: ${e.message}"
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        val classifier = audioClassifier ?: return
        audioRecord = classifier.createAudioRecord()
        audioRecord?.startRecording()
        isRecording = true
        btnAction.text = "STOP AI LISTENING"
        tvStatus.text = "Microphone Active"
        
        thread {
            while (isRecording) {
                try {
                    val record = audioRecord ?: break
                    // Safety check before classification
                    if (!isRecording || record.state != AudioRecord.STATE_INITIALIZED) break
                    
                    val results = classifier.classify(record)
                    
                    // Audio level visualization placeholder
                    val level = (Math.random() * 100).toInt() 
                    
                    runOnUiThread {
                        if (isRecording) { // Extra safety check on UI thread
                            pbLevel.progress = level
                            val display = results?.joinToString("\n") { 
                                "${it.categories().firstOrNull()?.categoryName()}: %.2f".format(it.categories().firstOrNull()?.score()) 
                            } ?: "No sounds detected"
                            tvOutput.text = display
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AudioStudio", "Thread error", e)
                    break 
                }
                Thread.sleep(500)
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        btnAction.text = "START AI LISTENING"
        tvStatus.text = "Microphone Idle"
        pbLevel.progress = 0
    }

    private fun clearHelpers() {
        stopRecording()
        audioClassifier?.close()
        audioClassifier = null
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

