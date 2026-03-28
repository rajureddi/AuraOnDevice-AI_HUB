package com.aura_on_device_ai.mls.api.download.custom

import android.util.Log
import com.aura_on_device_ai.mls.api.download.DownloadCoroutineManager
import com.aura_on_device_ai.mls.api.download.DownloadFileUtils.getLastFileName
import com.aura_on_device_ai.mls.api.download.DownloadFileUtils.repoFolderName
import com.aura_on_device_ai.mls.api.download.FileDownloadTask
import com.aura_on_device_ai.mls.api.download.ModelFileDownloader
import com.aura_on_device_ai.mls.api.download.ModelRepoDownloader
import com.aura_on_device_ai.mls.api.download.ModelIdUtils
import com.aura_on_device_ai.mls.api.HfFileMetadata
import java.io.File

class CustomModelDownloader(
    override var callback: ModelRepoDownloadCallback?,
    cacheRootPath: String
) : ModelRepoDownloader() {
    
    companion object {
        private const val TAG = "CustomModelDownloader"
        
        fun getCachePathRoot(modelDownloadPathRoot: String): String {
            return "$modelDownloadPathRoot/custom"
        }
    }

    override var cacheRootPath: String = getCachePathRoot(cacheRootPath)

    override fun setListener(callback: ModelRepoDownloadCallback?) {
        this.callback = callback
    }

    override fun download(modelId: String) {
        Log.d(TAG, "CustomModelDownloader download: $modelId")
        DownloadCoroutineManager.launchDownload {
            downloadCustomModel(modelId)
        }
    }

    private suspend fun downloadCustomModel(modelId: String) {
        val url = ModelIdUtils.getRepositoryPath(modelId)
        val fileName = getLastFileName(url)
        val destFile = getDownloadPath(modelId)
        
        if (destFile.exists()) {
            callback?.onDownloadFileFinished(modelId, destFile.absolutePath)
            return
        }

        val storageFolder = File(cacheRootPath, repoFolderName(url, "model"))
        if (!storageFolder.exists()) storageFolder.mkdirs()
        
        val task = FileDownloadTask().apply {
            this.relativePath = fileName
            this.fileMetadata = HfFileMetadata().apply {
                this.location = url
            }
            this.blobPath = File(storageFolder, "model.bin")
            this.blobPathIncomplete = File(storageFolder, "model.bin.incomplete")
            this.pointerPath = destFile
        }

        val listener = object : ModelFileDownloader.FileDownloadListener {
            override fun onDownloadDelta(fileName: String?, downloadedBytes: Long, totalBytes: Long, delta: Long): Boolean {
                callback?.onDownloadingProgress(modelId, "file", fileName, downloadedBytes, totalBytes)
                return pausedSet.contains(modelId)
            }
        }

        try {
            ModelFileDownloader().downloadFile(task, listener)
            callback?.onDownloadFileFinished(modelId, destFile.absolutePath)
        } catch (e: Exception) {
            callback?.onDownloadFailed(modelId, e)
        }
    }

    override suspend fun getRepoSize(modelId: String): Long = 0L

    override suspend fun checkUpdate(modelId: String) {}

    override fun getDownloadPath(modelId: String): File {
        val url = ModelIdUtils.getRepositoryPath(modelId)
        return File(cacheRootPath, getLastFileName(url))
    }

    override fun deleteRepo(modelId: String) {
        getDownloadPath(modelId).delete()
    }
}

