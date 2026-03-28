// Created by ruoyi.sjd on 2024/12/18.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package com.aura_on_device_ai.mls.api

import android.app.Application
import com.aura_on_device_ai.mnnllm.android.update.UpdateChecker

object ApplicationProvider {

    var application: Application? = null
    fun set(application: Application?) {
        ApplicationProvider.application = application
        UpdateChecker.registerDownloadReceiver(application!!.applicationContext)
    }

    @JvmStatic
    fun get(): Application {
        return application!!
    }
}


