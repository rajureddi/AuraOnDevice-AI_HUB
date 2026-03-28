package com.aura_on_device_ai.mnnllm.android.visual

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aura_on_device_ai.mnnllm.android.mediapipe.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionActivity : AppCompatActivity() {

    private var _binding: com.aura_on_device_ai.mnnllm.android.databinding.ActivityVisionBinding? = null
    private val binding get() = _binding!!
    private var modelId: String? = null
    private var faceDetectorHelper: MediaPipeFaceDetectorHelper? = null
    private var faceLandmarkerHelper: MediaPipeFaceLandmarkerHelper? = null
    private var objectDetectorHelper: MediaPipeObjectDetectorHelper? = null
    private var handLandmarkerHelper: MediaPipeHandLandmarkerHelper? = null
    private var poseLandmarkerHelper: MediaPipePoseLandmarkerHelper? = null
    private var gestureHelper: MediaPipeGestureRecognizerHelper? = null
    private var holisticHelper: MediaPipeHolisticLandmarkerHelper? = null
    private var imageClassifierHelper: MediaPipeImageClassifierHelper? = null
    private var segmenterHelper: MediaPipeImageSegmenterHelper? = null
    private var interactiveSegmenterHelper: MediaPipeInteractiveSegmenterHelper? = null

    private lateinit var cameraExecutor: ExecutorService
    private var isUsingCamera = true
    private var isFrontCamera = true
    private var modelPath: String = ""
    private var taskName: String = "Vision Task"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            processImageFromGallery(it)
        }
    }

    private var currentRoiX = 0.5f
    private var currentRoiY = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = com.aura_on_device_ai.mnnllm.android.databinding.ActivityVisionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modelId = intent.getStringExtra("modelId")
        modelPath = intent.getStringExtra("modelPath") ?: ""
        taskName = intent.getStringExtra("taskName") ?: "AI Vision"
        
        binding.tvTaskNameDisplay.text = taskName
        initModel()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnUpload.setOnClickListener {
            switchToGallery()
            getContent.launch("image/*")
        }

        binding.btnDetect.setOnClickListener {
            runDetectionManual()
        }

        binding.btnSwitchCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }
        
        setupTouchListener()
    }




    private fun setupTouchListener() {
        binding.overlay.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val viewWidth = v.width.toFloat()
                val viewHeight = v.height.toFloat()
                if (viewWidth > 0 && viewHeight > 0) {
                    currentRoiX = event.x / viewWidth
                    currentRoiY = event.y / viewHeight
                    
                    if (!isUsingCamera) {
                        binding.imageView.drawable?.let {
                            if (it is android.graphics.drawable.BitmapDrawable) {
                                runDetection(it.bitmap)
                            }
                        }
                    }
                }
            }
            v.performClick()
            true
        }
    }

    private fun initModel() {
        if (modelPath.isEmpty()) {
            Toast.makeText(this, "Model path not found", Toast.LENGTH_SHORT).show()
            return
        }

        clearHelpers()
        
        try {
            when (modelId) {
                "Face-Detector" -> faceDetectorHelper = MediaPipeFaceDetectorHelper(this, modelPath)
                "Face-Landmarker" -> {
                    faceLandmarkerHelper = MediaPipeFaceLandmarkerHelper(this, modelPath)
                }

                "Object-Detector" -> objectDetectorHelper = MediaPipeObjectDetectorHelper(this, modelPath)
                "Hand-Landmarker" -> handLandmarkerHelper = MediaPipeHandLandmarkerHelper(this, modelPath)
                "Pose-Landmarker" -> poseLandmarkerHelper = MediaPipePoseLandmarkerHelper(this, modelPath)
                "Gesture-Recognizer" -> gestureHelper = MediaPipeGestureRecognizerHelper(this, modelPath)
                "Holistic" -> holisticHelper = MediaPipeHolisticLandmarkerHelper(this, modelPath)
                "Image-Classifier" -> imageClassifierHelper = MediaPipeImageClassifierHelper(this, modelPath)
                "Segmenter" -> segmenterHelper = MediaPipeImageSegmenterHelper(this, modelPath)
                "Embedder" -> imageClassifierHelper = MediaPipeImageClassifierHelper(this, modelPath) // Using classifier as placeholder for embedder UI
                "Interactive-Segmenter", "Segmenter-Interactive" -> interactiveSegmenterHelper = MediaPipeInteractiveSegmenterHelper(this, modelPath)
            }
            Log.d(TAG, "Initialized model: $modelId from $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model from $modelPath", e)
            Toast.makeText(this, "Model init failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearHelpers() {
        faceDetectorHelper?.close()
        faceDetectorHelper = null
        faceLandmarkerHelper?.close()
        faceLandmarkerHelper = null
        objectDetectorHelper?.close()
        objectDetectorHelper = null
        handLandmarkerHelper?.close()
        handLandmarkerHelper = null
        poseLandmarkerHelper?.close()
        poseLandmarkerHelper = null
        gestureHelper?.close()
        gestureHelper = null
        holisticHelper?.close()
        holisticHelper = null
        interactiveSegmenterHelper?.close()
        interactiveSegmenterHelper = null
        segmenterHelper?.close()
        segmenterHelper = null
        imageClassifierHelper?.close()
        imageClassifierHelper = null

        binding.overlay.clear()
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isUsingCamera) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        
        // Correct rotation
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        runDetection(rotatedBitmap)
        imageProxy.close()
    }

    private fun runDetectionManual() {
        if (!isUsingCamera) {
            // Case: Image from Gallery
            binding.imageView.drawable?.let { drawable ->
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    runDetection(drawable.bitmap)
                }
            }
        } else {
            // Case: Camera Stream - run on current frame
            binding.viewFinder.bitmap?.let { bitmap ->
                runDetection(bitmap)
            }
        }
    }

    private fun runDetection(bitmap: Bitmap) {
        runOnUiThread {
            _binding?.progressBar?.visibility = View.VISIBLE
        }
        cameraExecutor.execute {
            val faceDetectResult = faceDetectorHelper?.detect(bitmap)
            val faceLandmarkResult = faceLandmarkerHelper?.detect(bitmap)
            val objectDetectResult = objectDetectorHelper?.detect(bitmap)
            val handResult = handLandmarkerHelper?.detect(bitmap)
            val poseResult = poseLandmarkerHelper?.detect(bitmap)
            val gestureRes = gestureHelper?.recognize(bitmap)
            val holisticRes = holisticHelper?.detect(bitmap)
            val interactiveSegRes = interactiveSegmenterHelper?.segment(bitmap, currentRoiX, currentRoiY)
            val standardSegRes = segmenterHelper?.segment(bitmap)
            val classRes = imageClassifierHelper?.classify(bitmap)
            
            val drawWidth = bitmap.width
            val drawHeight = bitmap.height

            runOnUiThread {
                val currentBinding = _binding ?: return@runOnUiThread
                currentBinding.overlay.setResults(
                    faceDetectorResult = faceDetectResult,
                    faceLandmarkerResult = faceLandmarkResult,
                    handLandmarkerResult = handResult,
                    poseLandmarkerResult = poseResult,
                    gestureResult = gestureRes,
                    objectDetectorResult = objectDetectResult,
                    holisticResult = holisticRes,
                    segmentationResult = standardSegRes ?: interactiveSegRes,
                    imageClassifierResult = classRes,
                    imageWidth = drawWidth,
                    imageHeight = drawHeight,
                    isFullCrop = isUsingCamera,
                    isFrontCamera = isFrontCamera
                )
                currentBinding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun switchToGallery() {
        isUsingCamera = false
        binding.viewFinder.visibility = View.GONE
        binding.imageView.visibility = View.VISIBLE
        binding.overlay.clear()
    }

    private fun switchToCamera() {
        isUsingCamera = true
        binding.imageView.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.overlay.clear()
        startCamera()
    }

    private fun processImageFromGallery(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        cameraExecutor.execute {
            try {
                contentResolver.openInputStream(uri).use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    runOnUiThread {
                        binding.imageView.setImageBitmap(bitmap)
                    }
                    runDetection(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from gallery", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        clearHelpers()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearHelpers()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "VisionActivity"
    }

    // Modern ViewBinding toBitmap extension
    private fun ImageProxy.toBitmap(): Bitmap {
        // More robust conversion for RGBA_8888
        val plane = planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        
        if (rowStride == width * pixelStride) {
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            // Slower but safer row-by-row copy for hardware buffers with padding
            val rowBytes = width * pixelStride
            val rowBuffer = java.nio.ByteBuffer.allocateDirect(rowBytes * height)
            val rowData = ByteArray(rowBytes)
            for (y in 0 until height) {
                buffer.position(y * rowStride)
                buffer.get(rowData)
                rowBuffer.put(rowData)
            }
            rowBuffer.rewind()
            bitmap.copyPixelsFromBuffer(rowBuffer)
        }
        return bitmap
    }
}

