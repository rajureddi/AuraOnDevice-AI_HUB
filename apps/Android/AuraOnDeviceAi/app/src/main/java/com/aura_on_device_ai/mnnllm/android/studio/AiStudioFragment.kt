package com.aura_on_device_ai.mnnllm.android.studio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aura_on_device_ai.mnnllm.android.R
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class AiStudioFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudioTaskAdapter

    data class StudioTask(
        val name: String,
        val description: String,
        val modelUrl: String,
        val fileName: String,
        val taskType: String, // e.g., "Face-Detector", "Pose-Landmarker"
        val category: String // "Vision", "Text", "Audio"
    )

    private val tasks = listOf(
        // Face Detection & Landmarks
        StudioTask("Face Detection (Short)", "BlazeFace short-range detection", "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite", "face_detection_short_range.tflite", "Face-Detector", "Vision"),
        StudioTask("Face Landmarker", "468-point face mesh tracking", "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task", "face_landmarker.task", "Face-Landmarker", "Vision"),
        
        // Hand & Gesture
        StudioTask("Hand Landmarker", "21-point skeletal hand tracking", "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task", "hand_landmarker.task", "Hand-Landmarker", "Vision"),
        StudioTask("Gesture Recognizer", "Detect hand signs and gestures", "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task", "gesture_recognizer.task", "Gesture-Recognizer", "Vision"),
        
        // Pose
        StudioTask("Pose Landmarker (Heavy)", "High-accuracy body skeletal", "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/1/pose_landmarker_heavy.task", "pose_landmarker_heavy.task", "Pose-Landmarker", "Vision"),
        StudioTask("Pose Landmarker (Full)", "Standard body skeletal tracking", "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task", "pose_landmarker_full.task", "Pose-Landmarker", "Vision"),
        StudioTask("Pose Landmarker (Lite)", "Lightweight body skeletal tracking", "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task", "pose_landmarker_lite.task", "Pose-Landmarker", "Vision"),
        
        // Object & Image
        StudioTask("Object Detector (Lite0)", "Fastest object detector", "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/1/efficientdet_lite0.tflite", "efficientdet-lite0.tflite", "Object-Detector", "Vision"),
        StudioTask("Object Detector (Lite2)", "High-quality object detector", "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite2/float32/1/efficientdet_lite2.tflite", "efficientdet-lite2.tflite", "Object-Detector", "Vision"),
        StudioTask("Image Classifier (Lite0)", "Fast image classification", "https://storage.googleapis.com/mediapipe-models/image_classifier/efficientnet_lite0/float32/1/efficientnet_lite0.tflite", "efficientnet-lite0.tflite", "Image-Classifier", "Vision"),
        StudioTask("Image Classifier (Lite2)", "High-quality classification", "https://storage.googleapis.com/mediapipe-models/image_classifier/efficientnet_lite2/float32/1/efficientnet_lite2.tflite", "efficientnet-lite2.tflite", "Image-Classifier", "Vision"),
       
        // Embedders
        StudioTask("Image Embedder (Small)", "Fastest image embedding", "https://storage.googleapis.com/mediapipe-models/image_embedder/mobilenet_v3_small/float32/1/mobilenet_v3_small.tflite", "mobilenet_v3_small.tflite", "Embedder", "Vision"),
        StudioTask("Image Embedder (Large)", "Robust image embedding", "https://storage.googleapis.com/mediapipe-models/image_embedder/mobilenet_v3_large/float32/1/mobilenet_v3_large.tflite", "mobilenet_v3_large.tflite", "Embedder", "Vision"),
        
        // Segmentation
        StudioTask("Interactive Segmenter", "User-guided object clipping", "https://storage.googleapis.com/mediapipe-models/interactive_segmenter/magic_touch/float32/1/magic_touch.tflite", "interactive_segmentation_model.tflite", "Segmenter", "Vision"),
        
        // Audio
        StudioTask("Audio Classifier", "YamNet sound identification", "https://storage.googleapis.com/mediapipe-models/audio_classifier/yamnet/float32/1/yamnet.tflite", "yamnet.tflite", "Audio-Classifier", "Audio"),
        
        // Text
        StudioTask("Text Embedder (BERT)", "MobileBERT contextual embedding", "https://storage.googleapis.com/mediapipe-models/text_embedder/bert_embedder/float32/1/bert_embedder.tflite", "mobile_bert.tflite", "Embedder", "Text"),
        StudioTask("Text Embedder (Average)", "Lightweight text comparison", "https://storage.googleapis.com/mediapipe-models/text_embedder/average_word_embedder/float32/1/average_word_embedder.tflite", "average_word.tflite", "Embedder", "Text"),
        StudioTask("Language Detector", "Identify input text language", "https://storage.googleapis.com/mediapipe-models/language_detector/language_detector/float32/1/language_detector.tflite", "detection_model.tflite", "Lang-Detector", "Text"),
        StudioTask("Text Classifier", "Identify topics in input text", "https://storage.googleapis.com/mediapipe-models/text_classifier/bert_classifier/float32/1/bert_classifier.tflite", "bert_classifier.tflite", "Text-Classifier", "Text")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ai_studio, container, false)
        recyclerView = view.findViewById(R.id.rv_studio_tasks)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = StudioTaskAdapter(tasks)
        recyclerView.adapter = adapter
        return view
    }

    inner class StudioTaskAdapter(private val taskList: List<StudioTask>) : RecyclerView.Adapter<StudioTaskAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_task_name)
            val desc: TextView = view.findViewById(R.id.tv_task_desc)
            val btnAction: Button = view.findViewById(R.id.btn_task_action)
            val progress: ProgressBar = view.findViewById(R.id.pb_task_download)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_studio_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = taskList[position]
            holder.name.text = task.name
            holder.desc.text = task.description
            
            val modelFile = File(requireContext().getExternalFilesDir(null), task.fileName)
            
            if (modelFile.exists()) {
                holder.btnAction.text = "Launch"
                holder.progress.visibility = View.GONE
            } else {
                holder.btnAction.text = "Download"
                holder.progress.visibility = View.GONE
            }

            holder.btnAction.setOnClickListener {
                if (modelFile.exists()) {
                    // Launch Correct Activity based on category
                    val activityClass = when(task.category) {
                        "Text" -> com.aura_on_device_ai.mnnllm.android.studio.TextStudioActivity::class.java
                        "Audio" -> com.aura_on_device_ai.mnnllm.android.studio.AudioStudioActivity::class.java
                        else -> com.aura_on_device_ai.mnnllm.android.visual.VisionActivity::class.java
                    }
                    val intent = Intent(context, activityClass).apply {
                        putExtra("modelPath", modelFile.absolutePath)
                        putExtra("modelId", task.taskType)
                        putExtra("taskName", task.name)
                    }
                    startActivity(intent)
                } else {
                    // Start Download with Progress
                    holder.btnAction.isEnabled = false
                    holder.progress.visibility = View.VISIBLE
                    holder.progress.isIndeterminate = false
                    holder.progress.progress = 0
                    
                    thread {
                        try {
                            val url = URL(task.modelUrl)
                            val connection = url.openConnection()
                            connection.connect()
                            
                            val lengthOfFile = connection.contentLength
                            val input = connection.getInputStream()
                            val output = modelFile.outputStream()
                            
                            val data = ByteArray(1024)
                            var total: Long = 0
                            var count: Int
                            while (input.read(data).also { count = it } != -1) {
                                total += count.toLong()
                                if (lengthOfFile > 0) {
                                    val progressValue = (total * 100 / lengthOfFile).toInt()
                                    activity?.runOnUiThread { holder.progress.progress = progressValue }
                                }
                                output.write(data, 0, count)
                            }
                            
                            output.flush()
                            output.close()
                            input.close()
                             
                            activity?.runOnUiThread {
                                holder.btnAction.isEnabled = true
                                holder.btnAction.text = "Launch"
                                holder.progress.visibility = View.GONE
                                Toast.makeText(context, "${task.name} downloaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                holder.btnAction.isEnabled = true
                                holder.progress.visibility = View.GONE
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        override fun getItemCount() = taskList.size
    }
}

