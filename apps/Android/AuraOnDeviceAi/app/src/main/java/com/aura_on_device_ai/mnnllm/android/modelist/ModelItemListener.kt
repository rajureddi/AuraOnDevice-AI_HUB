package com.aura_on_device_ai.mnnllm.android.modelist

import com.aura_on_device_ai.mls.api.ModelItem

interface ModelItemListener {
    fun onItemClicked(modelItem: ModelItem)
    fun onItemLongClicked(modelItem: ModelItem): Boolean
    fun onItemDeleted(modelItem: ModelItem)
    fun onItemRemoved(modelItem: ModelItem)
    fun onItemUpdate(modelItem: ModelItem)
}
