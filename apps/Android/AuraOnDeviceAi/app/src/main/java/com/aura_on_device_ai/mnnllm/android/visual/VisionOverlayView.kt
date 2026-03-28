package com.aura_on_device_ai.mnnllm.android.visual

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifierResult
import android.graphics.BitmapFactory


class VisionOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var faceDetectorResult: FaceDetectorResult? = null
    private var faceLandmarkerResult: FaceLandmarkerResult? = null
    private var handLandmarkerResult: HandLandmarkerResult? = null
    private var poseLandmarkerResult: PoseLandmarkerResult? = null
    private var gestureResult: GestureRecognizerResult? = null
    private var objectDetectorResult: ObjectDetectorResult? = null
    private var holisticResult: HolisticLandmarkerResult? = null
    private var segmenterResult: ImageSegmenterResult? = null
    private var classifierResult: ImageClassifierResult? = null
    private var segmenterBitmap: Bitmap? = null



    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var scaleFactor: Float = 1f

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val linePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        style = Paint.Style.FILL
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    private var isFullCrop: Boolean = true // True for camera (Math.max), false for gallery (Math.min)
    private var isFrontCamera: Boolean = true // Mirror X for front camera

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (imageWidth <= 0 || imageHeight <= 0) return

        // Calculate scale and offsets
        val scale = if (isFullCrop) {
            Math.max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        } else {
            Math.min(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        }
        
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f

        // Draw Labels (Gesture/Classification)
        gestureResult?.gestures()?.firstOrNull()?.firstOrNull()?.let {
            val gestureText = "Gesture: ${it.categoryName()} (${String.format("%.2f", it.score())})"
            canvas.drawText(gestureText, 50f, 100f, textPaint)
        }

        classifierResult?.classificationResult()?.classifications()?.firstOrNull()?.categories()?.firstOrNull()?.let {
            val classText = "Class: ${it.categoryName()} (${String.format("%.2f", it.score())})"
            canvas.drawText(classText, 50f, 180f, textPaint)
        }

        // Draw Segmentation Mask
        segmenterBitmap?.let {
            val destRect = RectF(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight)
            val paint = Paint().apply { alpha = 128 } // 50% transparency for the mask
            canvas.drawBitmap(it, null, destRect, paint)
        }

        // Draw Object Detection
        objectDetectorResult?.detections()?.forEach { detection ->
            val category = detection.categories().firstOrNull()
            val score = category?.score() ?: 0f
            if (score < 0.5f) return@forEach // Only draw confident detections

            val bb = detection.boundingBox()
            var left = bb.left * scale + offsetX
            var right = bb.right * scale + offsetX
            val top = bb.top * scale + offsetY
            val bottom = bb.bottom * scale + offsetY

            // Handle Mirroring for Front Camera
            if (isFullCrop && isFrontCamera) {
                val oldLeft = left
                left = width - right
                right = width - oldLeft
            }

            // Clamp Bounding Box to Screen Bounds
            val drawRect = RectF(
                Math.max(0f, left),
                Math.max(0f, top),
                Math.min(width.toFloat(), right),
                Math.min(height.toFloat(), bottom)
            )

            canvas.drawRect(drawRect, boxPaint)

            // Draw Class Label with Visibility Check
            val label = "${category?.categoryName()} ${String.format("%.2f", score)}"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize
            
            // Adjust X to stay in screen
            val textX = Math.max(0f, Math.min(drawRect.left, width - textWidth))
            
            // Adjust Y: if box is at the very top, draw text inside the box
            val textY = if (drawRect.top - 10 < textHeight) {
                drawRect.top + textHeight + 10
            } else {
                drawRect.top - 10
            }
            
            canvas.drawText(label, textX, textY, textPaint)
        }

        // Draw Face Detector
        faceDetectorResult?.detections()?.forEach { detection ->
            val boundingBox = detection.boundingBox()
            var left = boundingBox.left * scale + offsetX
            var right = boundingBox.right * scale + offsetX
            val top = boundingBox.top * scale + offsetY
            val bottom = boundingBox.bottom * scale + offsetY
            
            if (isFullCrop && isFrontCamera) {
                val oldLeft = left
                left = width - right
                right = width - oldLeft
            }
            
            // Clamp to Screen
            val drawRect = RectF(
                Math.max(0f, left),
                Math.max(0f, top),
                Math.min(width.toFloat(), right),
                Math.min(height.toFloat(), bottom)
            )
            
            canvas.drawRect(drawRect, boxPaint)
        }

        // Draw Hand Landmarks & Skeleton (for both Hand Landmarker and Gesture Recognizer)
        val allHandResults = mutableListOf<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>()
        handLandmarkerResult?.landmarks()?.let { allHandResults.addAll(it) }
        gestureResult?.landmarks()?.let { allHandResults.addAll(it) }

        allHandResults.forEach { handLandmarks ->
            // Draw connections (Skeleton)
            MediaPipeConstants.HAND_CONNECTIONS.forEach { connection ->
                val start = handLandmarks[connection.first]
                val end = handLandmarks[connection.second]
                
                var startX = start.x() * scaledWidth + offsetX
                var endX = end.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) {
                    startX = width - startX
                    endX = width - endX
                }
                
                canvas.drawLine(
                    startX, start.y() * scaledHeight + offsetY,
                    endX, end.y() * scaledHeight + offsetY,
                    linePaint
                )
            }

            // Draw points (Joints)
            handLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 8f, pointPaint)
            }
        }

        // Draw Pose Landmarks & Skeleton
        poseLandmarkerResult?.landmarks()?.forEach { poseLandmarks ->
            // Draw connections (Skeleton)
            MediaPipeConstants.POSE_CONNECTIONS.forEach { connection ->
                val start = poseLandmarks[connection.first]
                val end = poseLandmarks[connection.second]
                
                var startX = start.x() * scaledWidth + offsetX
                var endX = end.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) {
                    startX = width - startX
                    endX = width - endX
                }
                
                canvas.drawLine(
                    startX, start.y() * scaledHeight + offsetY,
                    endX, end.y() * scaledHeight + offsetY,
                    linePaint
                )
            }

            // Draw points (Joints)
            poseLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 8f, pointPaint)
            }
        }

        faceLandmarkerResult?.faceLandmarks()?.forEach { landmarks ->
            // Draw connections (contours)
            MediaPipeConstants.FACE_CONTOURS.forEach { connection ->
                val start = landmarks[connection.first]
                val end = landmarks[connection.second]
                
                var startX = start.x() * scaledWidth + offsetX
                var endX = end.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) {
                    startX = width - startX
                    endX = width - endX
                }
                
                canvas.drawLine(
                    startX, start.y() * scaledHeight + offsetY,
                    endX, end.y() * scaledHeight + offsetY,
                    linePaint
                )
            }

            // Draw points
            landmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 2f, pointPaint)
            }
        }

        
        // Draw Holistic Landmarks
        holisticResult?.faceLandmarks()?.let { faceLandmarks ->
            faceLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 3f, pointPaint)
            }
        }
        holisticResult?.poseLandmarks()?.let { poseLandmarks ->
            poseLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 6f, pointPaint)
            }
        }
        holisticResult?.leftHandLandmarks()?.let { leftHandLandmarks ->
            leftHandLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 4f, pointPaint)
            }
        }
        holisticResult?.rightHandLandmarks()?.let { rightHandLandmarks ->
            rightHandLandmarks.forEach { landmark ->
                var x = landmark.x() * scaledWidth + offsetX
                if (isFullCrop && isFrontCamera) x = width - x
                canvas.drawCircle(x, landmark.y() * scaledHeight + offsetY, 4f, pointPaint)
            }
        }
    }




    fun setResults(
        faceDetectorResult: FaceDetectorResult? = null,
        faceLandmarkerResult: FaceLandmarkerResult? = null,
        handLandmarkerResult: HandLandmarkerResult? = null,
        poseLandmarkerResult: PoseLandmarkerResult? = null,
        gestureResult: GestureRecognizerResult? = null,
        objectDetectorResult: ObjectDetectorResult? = null,
        holisticResult: HolisticLandmarkerResult? = null,
        segmentationResult: ImageSegmenterResult? = null,
        imageClassifierResult: ImageClassifierResult? = null,
        imageWidth: Int,
        imageHeight: Int,
        isFullCrop: Boolean = true,
        isFrontCamera: Boolean = true
    ) {
        this.faceDetectorResult = faceDetectorResult
        this.faceLandmarkerResult = faceLandmarkerResult
        this.handLandmarkerResult = handLandmarkerResult
        this.poseLandmarkerResult = poseLandmarkerResult
        this.gestureResult = gestureResult
        this.objectDetectorResult = objectDetectorResult
        this.holisticResult = holisticResult
        this.segmenterResult = segmentationResult
        this.classifierResult = imageClassifierResult
        
        // Convert mask to bitmap for drawing
        if (segmentationResult == null) {
            segmenterBitmap = null
        } else {
            segmentationResult.categoryMask()?.ifPresent { mask ->
                segmenterBitmap = VisualizationHelper.createBitmapFromMask(mask)
            } ?: run {
                segmentationResult.confidenceMasks()?.ifPresent { masks ->
                    if (masks.isNotEmpty()) {
                        segmenterBitmap = VisualizationHelper.createBitmapFromMask(masks[0])
                    } else {
                        segmenterBitmap = null
                    }
                }
            }
        }
        
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFullCrop = isFullCrop
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    fun clear() {
        faceDetectorResult = null
        faceLandmarkerResult = null
        handLandmarkerResult = null
        poseLandmarkerResult = null
        gestureResult = null
        objectDetectorResult = null
        holisticResult = null
        segmenterResult = null
        classifierResult = null
        segmenterBitmap = null
        invalidate()
    }
}

