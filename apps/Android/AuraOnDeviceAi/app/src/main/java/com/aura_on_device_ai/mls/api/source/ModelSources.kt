// Created by ruoyi.sjd on 2025/2/11.
// Copyright (c) 2024 alibaba Group Holding Limited All rights reserved.
package com.aura_on_device_ai.mls.api.source

import com.aura_on_device_ai.mnnllm.android.R

class ModelSources {
    enum class ModelSourceType {
        MODEL_SCOPE,
        HUGGING_FACE,
        MODELERS,
        LOCAL,
        URL
    }

    val remoteSourceType: ModelSourceType
        get() {
            return ModelSourceType.MODEL_SCOPE
        }

    private object InstanceHolder {
        val instance: ModelSources = ModelSources()
    }

    val config: ModelSourceConfig
        get() {
            if (mockConfig == null) {
                mockConfig = ModelSourceConfig.createMockSourceConfig()
            }
            return mockConfig!!
        }

    companion object {
        private var mockConfig: ModelSourceConfig? = null

        const val sourceHuffingFace = "HuggingFace"
        const val sourceModelers = "Modelers"
        const val sourceModelScope = "ModelScope"
        const val sourceUrl = "URL"

        val sourceList = listOf(
            sourceModelers,
            sourceHuffingFace,
            sourceModelScope,
            sourceUrl
        )

        val sourceDisPlayList = listOf<Int>(
            R.string.source_modelers,
            R.string.source_huggingface,
            R.string.source_modelscope,
            R.string.source_url
        )

        @JvmStatic
        fun get(): ModelSources {
            return InstanceHolder.instance
        }
    }
}


