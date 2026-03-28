// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 alibaba Group Holding Limited. All rights reserved.
package com.aura_on_device_ai.mls.api

import com.google.gson.annotations.SerializedName

class HfRepoInfo {
    class SiblingItem {
        @JvmField
        var rfilename: String? = null
        @JvmField
        var sha: String? = null
        @JvmField
        var size: Long = 0
        @JvmField
        var lfs: LfsItem? = null
    }

    class LfsItem {
        @JvmField
        var size: Long = 0
        @JvmField
        var sha256: String? = null
        @JvmField
        var pointerSize: Int = 0
    }

    // Getters and Setters
    @JvmField
    var modelId: String? = null
    var revision: String? = null
    @JvmField
    var sha: String? = null
    @SerializedName("siblings")
    var siblings: MutableList<SiblingItem> = ArrayList()
}


