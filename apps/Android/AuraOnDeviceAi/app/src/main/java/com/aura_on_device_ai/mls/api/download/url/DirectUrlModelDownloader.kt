package com.aura_on_device_ai.mls.api.download.url

import android.util.Log
import com.aura_on_device_ai.mls.api.FileDownloadException
import com.aura_on_device_ai.mls.api.HfFileMetadata
import com.aura_on_device_ai.mls.api.download.DownloadCoroutineManager
import com.aura_on_device_ai.mls.api.download.DownloadExecutor.Companion.executor
import com.aura_on_device_ai.mls.api.download.DownloadFileUtils.getLastFileName
import com.aura_on_device_ai.mls.api.download.DownloadPausedException
import com.aura_on_device_ai.mls.api.download.FileDownloadTask
import com.aura_on_device_ai.mls.api.download.ModelFileDownloader
import com.aura_on_device_ai.mls.api.download.ModelIdUtils
import com.aura_on_device_ai.mls.api.download.ModelRepoDownloader
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloader for direct URLs (e.g. from HuggingFace /resolve/ links)
 * modelId format: "URL/https://huggingface.co/path/to/model.task"
 */
class DirectUrlModelDownloader(override var callback: ModelRepoDownloadCallback?,
                              override var cacheRootPath: String
) : ModelRepoDownloader() {

    companion object {
        private const val TAG = "DirectUrlDownloader"

        fun getCachePathRoot(modelDownloadPathRoot: String): String {
            return "$modelDownloadPathRoot/url"
        }

        fun getModelPath(modelsDownloadPathRoot: String, modelId: String): File {
            val repoPath = ModelIdUtils.getRepositoryPath(modelId)
            val url = if (repoPath.startsWith("URL/")) repoPath.substring(4) else repoPath
            val fileName = getLastFileName(url)
            return File(getCachePathRoot(modelsDownloadPathRoot), fileName)
        }
    }

    private var httpClient: OkHttpClient? = null

    private fun getHttpClient(): OkHttpClient {
        if (httpClient == null) {
            httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
        return httpClient!!
    }

    override fun setListener(callback: ModelRepoDownloadCallback?) {
        this.callback = callback
    }

    override fun download(modelId: String) {
        val repoPath = ModelIdUtils.getRepositoryPath(modelId)
        val url = if (repoPath.startsWith("URL/")) repoPath.substring(4) else repoPath
        Log.d(TAG, "Downloading direct model from URL: $url")
        
        executor!!.submit {
            try {
                callback?.onDownloadTaskAdded()
                downloadSingleFile(modelId, url)
                callback?.onDownloadTaskRemoved()
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $modelId", e)
                callback?.onDownloadFailed(modelId, e)
            }
        }
    }

    private fun downloadSingleFile(modelId: String, url: String) {
        val fileName = getLastFileName(url)
        val downloadPath = getModelPath(cacheRootPath.substringBeforeLast("/"), modelId)
        downloadPath.parentFile?.mkdirs()
        
        if (downloadPath.exists()) {
            callback?.onDownloadFileFinished(modelId, downloadPath.absolutePath)
            return
        }

        // Fetch metadata via HEAD request to get total size
        val request = Request.Builder().url(url).head().build()
        var totalSize = 0L
        try {
            getHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    totalSize = response.header("Content-Length")?.toLong() ?: 0L
                    val lastModified = 0L // Not strictly needed
                    callback?.onRepoInfo(modelId, lastModified, totalSize)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get HEAD info for $url", e)
        }

        val fileDownloader = ModelFileDownloader()
        val task = FileDownloadTask()
        task.relativePath = fileName
        task.pointerPath = downloadPath
        task.blobPath = File(downloadPath.absolutePath + ".tmp")
        task.blobPathIncomplete = File(downloadPath.absolutePath + ".incomplete")
        
        task.fileMetadata = HfFileMetadata()
        task.fileMetadata!!.location = url
        task.fileMetadata!!.size = totalSize
        
        // Ensure parent dirs
        task.blobPath!!.parentFile?.mkdirs()

        val listener = object : ModelFileDownloader.FileDownloadListener {
            override fun onDownloadDelta(fileName: String?, downloadedBytes: Long, totalBytes: Long, delta: Long): Boolean {
                callback?.onDownloadingProgress(modelId, "file", fileName, downloadedBytes, totalSize)
                return pausedSet.contains(modelId)
            }
        }

        try {
            fileDownloader.downloadFile(task, listener)
            // After successful ModelFileDownloader.downloadFile, it creates a symlink or moves the file.
            // But wait! ModelFileDownloader is designed for the blob structure.
            // Let's manually move the file since this is a single direct file.
            if (task.blobPath!!.exists()) {
                task.blobPath!!.renameTo(downloadPath)
            }
            callback?.onDownloadFileFinished(modelId, downloadPath.absolutePath)
        } catch (e: DownloadPausedException) {
            pausedSet.remove(modelId)
            callback?.onDownloadPaused(modelId)
        } catch (e: Exception) {
            callback?.onDownloadFailed(modelId, e)
        }
    }

    override suspend fun getRepoSize(modelId: String): Long {
        val repoPath = ModelIdUtils.getRepositoryPath(modelId)
        val url = if (repoPath.startsWith("URL/")) repoPath.substring(4) else repoPath
        return withContext(DownloadCoroutineManager.downloadDispatcher) {
            runCatching {
                val request = Request.Builder().url(url).head().build()
                getHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.header("Content-Length")?.toLong() ?: 0L
                    } else 0L
                }
            }.getOrElse { 0L }
        }
    }

    override suspend fun checkUpdate(modelId: String) {
        // Simple direct URL doesn't support complex checking without cache control headers
    }

    override fun getDownloadPath(modelId: String): File {
        return getModelPath(cacheRootPath.substringBeforeLast("/"), modelId)
    }

    override fun deleteRepo(modelId: String) {
        val file = getDownloadPath(modelId)
        if (file.exists()) {
            file.delete()
        }
    }
}
