package com.aura_on_device_ai.mnnllm.android.chat.voice

import android.app.Application
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aura_on_device_ai.mls.api.download.DownloadInfo
import com.aura_on_device_ai.mls.api.download.DownloadListener
import com.aura_on_device_ai.mls.api.download.ModelDownloadManager
import com.aura_on_device_ai.mnnllm.android.modelmarket.ModelMarketItem
import com.aura_on_device_ai.mnnllm.android.modelmarket.ModelMarketItemWrapper
import com.aura_on_device_ai.mnnllm.android.modelmarket.ModelRepository
import com.aura_on_device_ai.mnnllm.android.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceModelMarketViewModel(application: Application) : AndroidViewModel(application), DownloadListener {

    companion object {
        private const val TAG = "VoiceModelMarketViewModel"
    }

    private val downloadManager = ModelDownloadManager.getInstance(application)
    private var allTtsModels: List<ModelMarketItemWrapper> = emptyList()
    private var allAsrModels: List<ModelMarketItemWrapper> = emptyList()
    private var mainHandler: Handler = Handler(application.mainLooper)
    
    private val _models = MutableLiveData<List<ModelMarketItemWrapper>>()
    val models: LiveData<List<ModelMarketItemWrapper>> = _models

    private val _progressUpdate = MutableLiveData<Pair<String, DownloadInfo>>()
    val progressUpdate: LiveData<Pair<String, DownloadInfo>> = _progressUpdate

    private val _itemUpdate = MutableLiveData<String>()
    val itemUpdate: LiveData<String> = _itemUpdate

    init {
        downloadManager.addListener(this)
    }

    fun loadTtsModels() {
        viewModelScope.launch {
            try {
                val ttsItems = ModelRepository.getMarketDataSuspend().ttsModels
                allTtsModels = processModels(ttsItems)
                applyRemovedFilter()
                Log.d(TAG, "Loaded ${allTtsModels.size} TTS models")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load TTS models", e)
                _models.postValue(emptyList())
            }
        }
    }

    fun loadAsrModels() {
        viewModelScope.launch {
            try {
                val asrItems = ModelRepository.getMarketDataSuspend().asrModels
                allAsrModels = processModels(asrItems)
                applyRemovedFilter()
                Log.d(TAG, "Loaded ${allAsrModels.size} ASR models")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ASR models", e)
                _models.postValue(emptyList())
            }
        }
    }

    private fun applyRemovedFilter() {
        val context = getApplication<Application>()
        val currentList = _models.value ?: emptyList()
        // Determine which list we are currently looking at based on the data
        // This is a bit tricky, but since the fragment calls loadTtsModels or loadAsrModels,
        // we can just filter allTtsModels or allAsrModels depending on what was just loaded.
        // For simplicity, we filter based on whether they were in the original lists.
        
        val filteredTts = allTtsModels.filter { !PreferenceUtils.isModelRemoved(context, it.modelMarketItem.modelId) }
        val filteredAsr = allAsrModels.filter { !PreferenceUtils.isModelRemoved(context, it.modelMarketItem.modelId) }
        
        // Use a heuristic to decide which one to post - usually we only display one type at a time
        // The fragment state currentModelType should really be here, but we can check the contents.
        if (currentList.isNotEmpty() && allTtsModels.any { it.modelMarketItem.modelId == currentList[0].modelMarketItem.modelId }) {
            _models.postValue(filteredTts)
        } else if (currentList.isNotEmpty() && allAsrModels.any { it.modelMarketItem.modelId == currentList[0].modelMarketItem.modelId }) {
            _models.postValue(filteredAsr)
        } else {
            // If empty or unsure, just post whichever one was updated last (represented by the calls to loadXXX)
            // For now, let's just use the current view's logic
            // Assuming fragment calls loadTtsModels -> it sets allTtsModels -> we post it
            // We need to know which one was requested.
        }
        
        // Re-fix: the original load methods should post the filtered versions directly
    }

    // Re-writing load methods to be cleaner
    fun loadTtsModelsFix() {
        viewModelScope.launch {
            try {
                val ttsItems = ModelRepository.getMarketDataSuspend().ttsModels
                allTtsModels = processModels(ttsItems)
                val context = getApplication<Application>()
                val filtered = allTtsModels.filter { !PreferenceUtils.isModelRemoved(context, it.modelMarketItem.modelId) }
                _models.postValue(filtered)
            } catch (e: Exception) {
                _models.postValue(emptyList())
            }
        }
    }

    fun loadAsrModelsFix() {
        viewModelScope.launch {
            try {
                val asrItems = ModelRepository.getMarketDataSuspend().asrModels
                allAsrModels = processModels(asrItems)
                val context = getApplication<Application>()
                val filtered = allAsrModels.filter { !PreferenceUtils.isModelRemoved(context, it.modelMarketItem.modelId) }
                _models.postValue(filtered)
            } catch (e: Exception) {
                _models.postValue(emptyList())
            }
        }
    }

    private suspend fun processModels(models: List<ModelMarketItem>): List<ModelMarketItemWrapper> = withContext(Dispatchers.IO) {
        return@withContext models.map { item ->
            // Get download info
            val downloadInfo = downloadManager.getDownloadInfo(item.modelId)
            ModelMarketItemWrapper(item, downloadInfo)
        }
    }

    private fun updateDownloadInfo(modelId: String) {
        // Update current displayed list
        val context = getApplication<Application>()
        val currentList = _models.value ?: emptyList()
        val updatedList = currentList.map { wrapper ->
            if (wrapper.modelMarketItem.modelId == modelId) {
                wrapper.downloadInfo = downloadManager.getDownloadInfo(wrapper.modelMarketItem.modelId)
            }
            wrapper
        }
        _models.postValue(updatedList)
    }

    fun startDownload(item: ModelMarketItem) {
        downloadManager.startDownload(item.modelId)
    }

    fun pauseDownload(item: ModelMarketItem) {
        downloadManager.pauseDownload(item.modelId)
    }

    fun deleteModel(item: ModelMarketItem) {
        viewModelScope.launch {
            downloadManager.deleteModel(item.modelId)
        }
    }

    fun removeModel(item: ModelMarketItem) {
        val context = getApplication<Application>()
        PreferenceUtils.removeModelFromMarket(context, item.modelId)
        // Refresh current list
        val currentList = _models.value ?: emptyList()
        val filtered = currentList.filter { it.modelMarketItem.modelId != item.modelId }
        _models.postValue(filtered)
    }

    fun updateModel(item: ModelMarketItem) {
        downloadManager.startDownload(item.modelId)
    }

    // DownloadListener implementation
    override fun onDownloadTotalSize(modelId: String, totalSize: Long) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    override fun onDownloadHasUpdate(modelId: String, downloadInfo: DownloadInfo) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }


    override fun onDownloadStart(modelId: String) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    override fun onDownloadProgress(modelId: String, downloadInfo: DownloadInfo) {
        mainHandler.post {
            _progressUpdate.value = Pair(modelId, downloadInfo)
        }
    }

    override fun onDownloadFailed(modelId: String, exception: Exception) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    override fun onDownloadFinished(modelId: String, path: String) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    override fun onDownloadPaused(modelId: String) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    override fun onDownloadFileRemoved(modelId: String) {
        mainHandler.post {
            updateDownloadInfo(modelId)
            _itemUpdate.value = modelId
        }
    }

    fun setDefaultTtsModel(modelId: String) {
        val context = getApplication<Application>()
        com.aura_on_device_ai.mnnllm.android.mainsettings.MainSettings.setDefaultTtsModel(context, modelId)
    }

    fun setDefaultAsrModel(modelId: String) {
        val context = getApplication<Application>()
        com.aura_on_device_ai.mnnllm.android.mainsettings.MainSettings.setDefaultAsrModel(context, modelId)
    }

    override fun onCleared() {
        super.onCleared()
        downloadManager.removeListener(this)
        mainHandler.removeCallbacksAndMessages(null)
    }
} 
