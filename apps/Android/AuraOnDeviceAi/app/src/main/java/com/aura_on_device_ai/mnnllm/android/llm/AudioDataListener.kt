// Created by ruoyi.sjd on 2025/5/7.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.aura_on_device_ai.mnnllm.android.llm


interface AudioDataListener {
    fun onAudioData(data: FloatArray, isEnd: Boolean): Boolean
}

