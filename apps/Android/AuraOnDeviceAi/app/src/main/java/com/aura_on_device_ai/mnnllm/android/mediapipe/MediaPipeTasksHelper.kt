package com.aura_on_device_ai.mnnllm.android.mediapipe

import android.content.Context
import android.util.Log
import android.os.SystemClock
import androidx.preference.PreferenceManager
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.components.containers.AudioData
import android.media.AudioRecord
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// Vision Imports
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

// Text Imports
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetector
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedderResult

// Containers and Results
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifierResult
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedderResult
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetectorResult

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.core.Delegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object MediaPipeHelper {
    private const val TAG = "MediaPipeHelper"

    /**
     * Helper mapping a local file to a ByteBuffer so MediaPipe Tasks can load models from internal/external storage
     * rather than being restricted to just the assets. directory.
     */
    fun loadModelBufferFromStorage(filePath: String): MappedByteBuffer? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Model file does not exist: $filePath")
            return null
        }
        return try {
            val fileInputStream = FileInputStream(file)
            val fileChannel = fileInputStream.channel
            fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model file into buffer: ${e.message}")
            null
        }
    }
}

class MediaPipeTextClassifierHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var textClassifier: TextClassifier? = null

    init {
        setupTextClassifier()
    }

    private fun setupTextClassifier() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()

        val options = TextClassifier.TextClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .build()

        try {
            textClassifier = TextClassifier.createFromOptions(context, options)
            Log.d("TextClassifierHelper", "Text classifier properly initialized")
        } catch (e: Exception) {
            Log.e("TextClassifierHelper", "Error initializing Text Classifier", e)
        }
    }

    /**
     * Runs text classification asynchronously on a background thread.
     */
    fun classify(text: String, listener: (List<Classifications>?, Long) -> Unit) {
        Thread {
            val startTime = SystemClock.uptimeMillis()
            val classifier = textClassifier
            if (classifier == null) {
                listener(null, 0)
                return@Thread
            }

            try {
                val result = classifier.classify(text)
                val inferenceTime = SystemClock.uptimeMillis() - startTime
                val classifications = result.classificationResult().classifications()
                listener(classifications, inferenceTime)
            } catch (e: Exception) {
                Log.e("TextClassifierHelper", "Classification failed", e)
                listener(null, 0)
            }
        }.start()
    }

    fun close() {
        textClassifier?.close()
        textClassifier = null
    }
}

class MediaPipeAudioClassifierHelper(
    private val context: Context,
    private val localFilePath: String,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 3
) {
    private var audioClassifier: AudioClassifier? = null

    init {
        setupAudioClassifier()
    }

    private fun setupAudioClassifier() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()

        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
            .setBaseOptions(baseOptions)
            .setRunningMode(com.google.mediapipe.tasks.audio.core.RunningMode.AUDIO_CLIPS)
            .build()

        try {
            audioClassifier = AudioClassifier.createFromOptions(context, options)
            Log.d("AudioClassifier", "Audio classifier properly initialized")
        } catch (e: Exception) {
            Log.e("AudioClassifier", "Error initializing Audio Classifier", e)
        }
    }

    fun createAudioRecord(): AudioRecord? {
        val classifier = audioClassifier ?: return null
        return try {
            classifier.createAudioRecord()
        } catch (e: Exception) {
            Log.e("AudioClassifier", "Failed to create AudioRecord", e)
            null
        }
    }

    fun classify(audioRecord: AudioRecord): List<Classifications>? {
        val classifier = audioClassifier ?: return null

        return try {
            val audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setSampleRate(audioRecord.sampleRate.toFloat())
                    .setNumOfChannels(audioRecord.channelCount)
                    .build(),
                audioRecord.bufferSizeInFrames
            )
            audioData.load(audioRecord)
            val result: AudioClassifierResult = classifier.classify(audioData)
            if (result.classificationResults().isNotEmpty()) {
                result.classificationResults().firstOrNull()?.classifications()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioClassifier", "Classification failed", e)
            null
        }
    }

    fun close() {
        audioClassifier?.close()
        audioClassifier = null
    }
}

class MediaPipeFaceDetectorHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var faceDetector: FaceDetector? = null

    init {
        setupFaceDetector()
    }

    private fun setupFaceDetector() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        try {
            faceDetector = FaceDetector.createFromOptions(context, options)
            Log.d("FaceDetectorHelper", "Face detector properly initialized")
        } catch (e: Exception) {
            Log.e("FaceDetectorHelper", "Error initializing Face Detector", e)
        }
    }

    fun detect(bitmap: Bitmap): FaceDetectorResult? {
        val detector = faceDetector ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            detector.detect(mpImage)
        } catch (e: Exception) {
            Log.e("FaceDetectorHelper", "Detection failed", e)
            null
        }
    }

    fun close() {
        faceDetector?.close()
        faceDetector = null
    }
}

class MediaPipeObjectDetectorHelper(
    private val context: Context,
    private val localFilePath: String,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 5
) {
    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        try {
            objectDetector = ObjectDetector.createFromOptions(context, options)
            Log.d("ObjectDetectorHelper", "Object detector properly initialized with threshold: $threshold")
        } catch (e: Exception) {
            Log.e("ObjectDetectorHelper", "Error initializing Object Detector", e)
        }
    }

    fun detect(bitmap: Bitmap): ObjectDetectorResult? {
        val detector = objectDetector ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            detector.detect(mpImage)
        } catch (e: Exception) {
            Log.e("ObjectDetectorHelper", "Detection failed", e)
            null
        }
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
    }
}





class MediaPipeLlmHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null
    private val TAG = "MediaPipeLlmHelper"

    init {
        setupLlmInference()
    }

    private fun setupLlmInference() {
        try {
            val options = LlmInferenceOptions.builder()
                .setModelPath(localFilePath)
                .setMaxTokens(2048)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            
            // Create the session for multi-turn support and set generation options
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(0.7f)
                .setTopK(40)
                .build()
            llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
            
            Log.d(TAG, "LlmInference and Session initialized from $localFilePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LlmInference: ${e.message}", e)
            throw e
        }
    }

    fun addQuery(text: String) {
        try {
            llmSession?.addQueryChunk(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add query chunk", e)
        }
    }

    fun addResponse(text: String) {
        try {
            // MediaPipe GenAI 0.10.33 does not have addResponseChunk in LlmInferenceSession.
            // Conversation history for the session is maintained by addQueryChunk and 
            // the internal state of the session after generateResponseAsync calls.
            Log.w(TAG, "addResponse (addResponseChunk) is not supported in this MediaPipe version. Seeding history may require manual prompt formatting.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add response chunk", e)
        }
    }

    /**
     * BLOCKING method: called from a background thread.
     * Use generateResponseAsync internally and wait for 'done'.
     */
    fun generateBlocking(
        prompt: String,
        bitmap: android.graphics.Bitmap? = null,
        onToken: (delta: String, isEnd: Boolean) -> Boolean
    ) {
        val session = llmSession ?: run {
            onToken("Error: LlmInference session NOT initialized", true)
            return
        }

        val latch = java.util.concurrent.CountDownLatch(1)
        var stopRequested = false

        try {
            // Multi-turn: add query to session and generate
            session.addQueryChunk(prompt)
            session.generateResponseAsync { delta, isEnd ->
                if (isEnd) {
                    onToken("", true)
                    latch.countDown()
                } else if (delta != null && delta.isNotEmpty() && !stopRequested) {
                    if (onToken(delta, false)) {
                        stopRequested = true // Stop signal from UI
                    }
                }
            }
            
            // Wait for Latch to be released when isEnd=true comes back from MediaPipe
            latch.await()
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e)
            onToken("Error during generation: ${e.message}", true)
            latch.countDown()
        }
    }

    fun close() {
        try {
            llmSession?.close()
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing inference", e)
        }
        llmInference = null
        llmSession = null
    }
}

class MediaPipeFaceLandmarkerHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .build()

        try {
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d("FaceLandmarkerHelper", "Face landmarker properly initialized")
        } catch (e: Exception) {
            Log.e("FaceLandmarkerHelper", "Error initializing Face Landmarker", e)
        }
    }

    fun detect(bitmap: Bitmap): FaceLandmarkerResult? {
        val landmarker = faceLandmarker ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            landmarker.detect(mpImage)
        } catch (e: Exception) {
            Log.e("FaceLandmarkerHelper", "Detection failed", e)
            null
        }
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}

class MediaPipeImageClassifierHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var classifier: ImageClassifier? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setRunningMode(RunningMode.IMAGE)
            .setMaxResults(5)
            .build()
        try { classifier = ImageClassifier.createFromOptions(context, options) } catch (e: Exception) { Log.e("ImageClassifier", "Init failed", e) }
    }
    fun classify(bitmap: Bitmap): ImageClassifierResult? {
        return try { classifier?.classify(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { classifier?.close(); classifier = null }
}

class MediaPipeGestureRecognizerHelper(
    private val context: Context,
    private val localFilePath: String,
    private val minHandDetectionConfidence: Float = 0.5f,
    private val minHandTrackingConfidence: Float = 0.5f,
    private val minHandPresenceConfidence: Float = 0.5f
) {
    private var recognizer: GestureRecognizer? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setMinHandDetectionConfidence(minHandDetectionConfidence)
            .setMinTrackingConfidence(minHandTrackingConfidence)
            .setMinHandPresenceConfidence(minHandPresenceConfidence)
            .setRunningMode(RunningMode.IMAGE)
            .build()
        try { recognizer = GestureRecognizer.createFromOptions(context, options) } catch (e: Exception) { Log.e("Gesture", "Init failed", e) }
    }
    fun recognize(bitmap: Bitmap): GestureRecognizerResult? {
        return try { recognizer?.recognize(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { recognizer?.close(); recognizer = null }
}

class MediaPipeHandLandmarkerHelper(
    private val context: Context,
    private val localFilePath: String,
    private val minHandDetectionConfidence: Float = 0.5f,
    private val minHandTrackingConfidence: Float = 0.5f,
    private val minHandPresenceConfidence: Float = 0.5f
) {
    private var landmarker: HandLandmarker? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setMinHandDetectionConfidence(minHandDetectionConfidence)
            .setMinTrackingConfidence(minHandTrackingConfidence)
            .setMinHandPresenceConfidence(minHandPresenceConfidence)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(2)
            .build()
        try { landmarker = HandLandmarker.createFromOptions(context, options) } catch (e: Exception) { Log.e("Hand", "Init failed", e) }
    }
    fun detect(bitmap: Bitmap): HandLandmarkerResult? {
        return try { landmarker?.detect(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { landmarker?.close(); landmarker = null }
}

class MediaPipePoseLandmarkerHelper(
    private val context: Context,
    private val localFilePath: String,
    private val minPoseDetectionConfidence: Float = 0.5f,
    private val minPoseTrackingConfidence: Float = 0.5f,
    private val minPosePresenceConfidence: Float = 0.5f
) {
    private var landmarker: PoseLandmarker? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
            .setMinTrackingConfidence(minPoseTrackingConfidence)
            .setMinPosePresenceConfidence(minPosePresenceConfidence)
            .setRunningMode(RunningMode.IMAGE)
            .build()
        try { landmarker = PoseLandmarker.createFromOptions(context, options) } catch (e: Exception) { Log.e("Pose", "Init failed", e) }
    }
    fun detect(bitmap: Bitmap): PoseLandmarkerResult? {
        return try { landmarker?.detect(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { landmarker?.close(); landmarker = null }
}

class MediaPipeHolisticLandmarkerHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var landmarker: HolisticLandmarker? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = HolisticLandmarker.HolisticLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setRunningMode(RunningMode.IMAGE)
            .build()
        try { landmarker = HolisticLandmarker.createFromOptions(context, options) } catch (e: Exception) { Log.e("Holistic", "Init failed", e) }
    }
    fun detect(bitmap: Bitmap): HolisticLandmarkerResult? {
        return try { landmarker?.detect(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { landmarker?.close(); landmarker = null }
}

class MediaPipeImageSegmenterHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var segmenter: ImageSegmenter? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setRunningMode(RunningMode.IMAGE)
            .build()
        try { segmenter = ImageSegmenter.createFromOptions(context, options) } catch (e: Exception) { Log.e("Segmenter", "Init failed", e) }
    }
    fun segment(bitmap: Bitmap): ImageSegmenterResult? {
        return try { segmenter?.segment(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { segmenter?.close(); segmenter = null }
}

class MediaPipeLanguageDetectorHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var detector: LanguageDetector? = null

    init {
        setup()
    }

    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = LanguageDetector.LanguageDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .build()
        try {
            detector = LanguageDetector.createFromOptions(context, options)
            Log.d("LangDetector", "Language detector properly initialized")
        } catch (e: Exception) {
            Log.e("LangDetector", "Init failed", e)
        }
    }

    /**
     * Runs language detection asynchronously on a background thread.
     */
    fun detect(text: String, listener: (LanguageDetectorResult?, Long) -> Unit) {
        Thread {
            val startTime = SystemClock.uptimeMillis()
            val det = detector
            if (det == null) {
                listener(null, 0)
                return@Thread
            }

            try {
                val result = det.detect(text)
                val time = SystemClock.uptimeMillis() - startTime
                listener(result, time)
            } catch (e: Exception) {
                Log.e("LangDetector", "Detection failed", e)
                listener(null, 0)
            }
        }.start()
    }

    fun close() {
        detector?.close()
        detector = null
    }
}

class MediaPipeImageEmbedderHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var embedder: ImageEmbedder? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setRunningMode(RunningMode.IMAGE)
            .build()
        try { embedder = ImageEmbedder.createFromOptions(context, options) } catch (e: Exception) { Log.e("Embedder", "Init failed", e) }
    }
    fun embed(bitmap: Bitmap): ImageEmbedderResult? {
        return try { embedder?.embed(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { null }
    }
    fun close() { embedder?.close(); embedder = null }
}

class MediaPipeInteractiveSegmenterHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var segmenter: InteractiveSegmenter? = null
    init { setup() }
    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val options = InteractiveSegmenter.InteractiveSegmenterOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(buffer).build())
            .setOutputCategoryMask(true)
            .setOutputConfidenceMasks(false)
            .build()
        try { segmenter = InteractiveSegmenter.createFromOptions(context, options) } catch (e: Exception) { Log.e("Interactive", "Init failed", e) }
    }
    fun segment(bitmap: Bitmap, roiX: Float = 0.5f, roiY: Float = 0.5f): ImageSegmenterResult? {
        val segmenterPtr = segmenter ?: return null
        val mpImage = BitmapImageBuilder(bitmap).build()
        val roi = InteractiveSegmenter.RegionOfInterest.create(
            com.google.mediapipe.tasks.components.containers.NormalizedKeypoint.create(roiX, roiY)
        )
        return try { segmenterPtr.segment(mpImage, roi) } catch (e: Exception) { null }
    }
    fun close() { segmenter?.close(); segmenter = null }
}

class MediaPipeTextEmbedderHelper(
    private val context: Context,
    private val localFilePath: String
) {
    private var embedder: TextEmbedder? = null

    init {
        setup()
    }

    private fun setup() {
        val buffer = MediaPipeHelper.loadModelBufferFromStorage(localFilePath) ?: return
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .setDelegate(com.google.mediapipe.tasks.core.Delegate.CPU)
            .build()
            
        val options = TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setL2Normalize(true)
            .build()
            
        try {
            embedder = TextEmbedder.createFromOptions(context, options)
            Log.d("TextEmbedder", "Text embedder properly initialized with L2 normalization")
        } catch (e: Exception) {
            Log.e("TextEmbedder", "Init failed", e)
        }
    }

    /**
     * Embeds a single piece of text.
     */
    fun embed(text: String): TextEmbedderResult? {
        return try {
            embedder?.embed(text)
        } catch (e: Exception) {
            Log.e("TextEmbedder", "Embedding failed", e)
            null
        }
    }

    /**
     * Compares two pieces of text asynchronously and returns cosine similarity and inference time.
     */
    fun compare(firstText: String, secondText: String, listener: (Double?, Long) -> Unit) {
        Thread {
            val startTime = SystemClock.uptimeMillis()
            val emb = embedder
            if (emb == null) {
                listener(null, 0)
                return@Thread
            }

            try {
                val firstResult = emb.embed(firstText).embeddingResult().embeddings().first()
                val secondResult = emb.embed(secondText).embeddingResult().embeddings().first()
                
                val similarity = TextEmbedder.cosineSimilarity(firstResult, secondResult)
                val time = SystemClock.uptimeMillis() - startTime
                listener(similarity, time)
            } catch (e: Exception) {
                Log.e("TextEmbedder", "Comparison failed", e)
                listener(null, 0)
            }
        }.start()
    }

    fun close() {
        embedder?.close()
        embedder = null
    }
}
