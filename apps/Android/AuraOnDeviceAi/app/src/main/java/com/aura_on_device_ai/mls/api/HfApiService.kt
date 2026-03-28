// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 alibaba Group Holding Limited All rights reserved.
package com.aura_on_device_ai.mls.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Minimal HuggingFace API service for the framework
 */
interface HfApiService {
    @GET("/api/models/{repoId}")
    fun getRepoInfoInternal(
        @Path("repoId", encoded = true) repoId: String,
        @Query("revision") revision: String? = "main",
        @Query("blobs") blobs: Boolean = true
    ): Call<HfRepoInfo>
}


