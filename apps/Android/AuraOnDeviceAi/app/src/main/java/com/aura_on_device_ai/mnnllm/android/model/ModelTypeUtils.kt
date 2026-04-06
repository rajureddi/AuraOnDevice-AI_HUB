// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package com.aura_on_device_ai.mnnllm.android.model

import java.util.Locale
import java.io.File
import com.aura_on_device_ai.mnnllm.android.modelist.ModelListManager
import com.aura_on_device_ai.mnnllm.android.modelsettings.ModelConfig

object ModelTypeUtils {

    fun isAudioModel(modelId: String): Boolean {
        if (modelId.lowercase(Locale.getDefault()).contains("audio") || isOmni(modelId)) {
            return true
        }
        val tags = ModelListManager.getModelTags(modelId)
        return isAudioModelByTags(tags) || ModelListManager.isAudioModel(modelId)
    }

    fun isMultiModalModel(modelName: String): Boolean {
        return isAudioModel(modelName) || isVisualModel(modelName) || isDiffusionModel(modelName) || isOmni(modelName)
    }

    fun isQnnModel(modelId: String): Boolean {
        val normalizedId = modelId.lowercase(Locale.getDefault())
        if (normalizedId.contains("qnn")) {
            return true
        }
        val tags = ModelListManager.getModelTags(modelId)
        if (isQnnModel(tags)) return true
        
        // Final fallback: check the model's own directory name if we can find it
        val configPath = ModelConfig.getDefaultConfigFile(modelId)
        if (configPath != null) {
            val file = File(configPath)
            val dir = if (file.isDirectory) file else file.parentFile
            if (dir?.name?.lowercase(Locale.getDefault())?.contains("qnn") == true) return true
        }
        return false
    }

    fun isDiffusionModel(modelName: String): Boolean {
        return modelName.lowercase(Locale.getDefault()).contains("stable-diffusion")
    }

    fun isVisualModel(modelId: String): Boolean {
        if (modelId.lowercase(Locale.getDefault()).contains("vl") || 
            modelId.lowercase(Locale.getDefault()).contains("visual")) {
            return true
        }
        val tags = ModelListManager.getModelTags(modelId)
        return isVisualModelByTags(tags) || ModelListManager.isVisualModel(modelId)
    }

    fun isVideoModel(modelId: String): Boolean {
        return modelId.lowercase(Locale.getDefault()).contains("video") ||
                ModelListManager.isVideoModel(modelId)
    }

    fun isR1Model(modelName: String): Boolean {
        return modelName.lowercase(Locale.getDefault()).contains("deepseek-r1") || 
               modelName.lowercase(Locale.getDefault()).contains("gemma3")
    }

    fun isThinkingModel(modelName: String): Boolean {
        val lowerName = modelName.lowercase(Locale.getDefault())
        return lowerName.contains("deepseek-r1") || lowerName.contains("gemma3") || lowerName.contains("think")
    }

    fun isOmni(modelName: String): Boolean {
        return modelName.lowercase(Locale.getDefault()).contains("omni")
    }

    fun isSupportThinkingSwitchByTags(extraTags: List<String>): Boolean {
        return extraTags.any { it.equals("ThinkingSwitch", ignoreCase = true) }
    }

    fun isQnnModel(tags: List<String>): Boolean {
        return tags.any { it.equals("QNN", ignoreCase = true) }
    }

    fun supportAudioOutput(modelName: String): Boolean {
        return isOmni(modelName)
    }

    /**
     * Check if the model is a TTS (Text-to-Speech) model
     */
    fun isTtsModel(modelName: String): Boolean {
        return modelName.lowercase(Locale.getDefault()).contains("bert-vits") ||
               modelName.lowercase(Locale.getDefault()).contains("tts")
    }

    /**
     * Check if the model is a TTS model based on tags
     */
    fun isTtsModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("TTS", ignoreCase = true) }
    }

    /**
     * Check if the model is an ASR (Automatic Speech Recognition) model based on tags
     */
    fun isAsrModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("ASR", ignoreCase = true) }
    }

    /**
     * Check if the model is a thinking model based on tags
     */
    fun isThinkingModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("Think", ignoreCase = true) }
    }

    /**
     * Check if the model is a visual model based on tags
     */
    fun isVisualModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("Vision", ignoreCase = true) }
    }

    /**
     * Check if the model is a video model based on tags
     */
    fun isVideoModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("Video", ignoreCase = true) }
    }

    /**
     * Check if the model is an audio model based on tags
     */
    fun isAudioModelByTags(tags: List<String>): Boolean {
        return tags.any { it.equals("Audio", ignoreCase = true) }
    }

    /**
     * Check if the model is a MediaPipe model (GenAI/Tasks)
     */
    fun isMediaPipeModel(modelId: String, configPath: String? = null): Boolean {
        val lowerId = modelId.lowercase(Locale.getDefault())
        if (lowerId.contains("mediapipe") || lowerId.contains("genai") || 
            lowerId.contains("litert") ||
            lowerId.endsWith(".task") || lowerId.endsWith(".bin") || 
            lowerId.endsWith(".litertlm") || lowerId.endsWith(".tflite") || lowerId.endsWith(".litert")) {
            return true
        }
        
        // Check config path or model file extension
        if (configPath != null) {
            val lowerPath = configPath.lowercase(Locale.getDefault())
            if (lowerPath.endsWith(".task") || lowerPath.endsWith(".bin") || 
                lowerPath.endsWith(".litertlm") || lowerPath.endsWith(".tflite") || lowerPath.endsWith(".litert")) {
                return true
            }
            
            // If it's a directory, check for MediaPipe files but ensure no .mnn files (which would indicate MNN engine)
            val dir = File(configPath)
            if (dir.isDirectory) {
                val files = dir.listFiles()
                val hasMnn = files?.any { it.name.lowercase(Locale.getDefault()).endsWith(".mnn") } ?: false
                if (hasMnn) return false
                
                val hasMediaPipeFile = files?.any { 
                    val name = it.name.lowercase(Locale.getDefault())
                    name.endsWith(".task") || name.endsWith(".bin") || 
                    name.endsWith(".litertlm") || name.endsWith(".tflite") || name.endsWith(".litert")
                } ?: false
                if (hasMediaPipeFile) return true
            }
        }
        
        return false
    }
    /**
     * Check if the model is a LiteRT-LM model (Modern Engine API)
     */
    fun isLiteRTLm(configPath: String?): Boolean {
        if (configPath == null) return false
        val lowerPath = configPath.lowercase(Locale.getDefault())
        if (lowerPath.endsWith(".litertlm") || lowerPath.endsWith(".litert") || lowerPath.endsWith(".tflite")) {
            return true
        }
        val dir = File(configPath)
        if (dir.isDirectory) {
            return dir.listFiles()?.any { 
                val name = it.name.lowercase(Locale.getDefault())
                name.endsWith(".litertlm") || name.endsWith(".litert") || name.endsWith(".tflite")
            } ?: false
        }
        return false
    }

    /**
     * Check if the model is a MediaPipe Task model (Legacy GenAI Tasks)
     */
    fun isMediaPipeTask(configPath: String?): Boolean {
        if (configPath == null) return false
        val lowerPath = configPath.lowercase(Locale.getDefault())
        if (lowerPath.endsWith(".task") || lowerPath.endsWith(".bin")) {
            return true
        }
        val dir = File(configPath)
        if (dir.isDirectory) {
            return dir.listFiles()?.any { 
                val name = it.name.lowercase(Locale.getDefault())
                name.endsWith(".task") || name.endsWith(".bin")
            } ?: false
        }
        return false
    }
}


