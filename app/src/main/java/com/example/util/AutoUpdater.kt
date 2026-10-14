package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AutoUpdater {
    private const val GITHUB_REPO = "Rohitjjn/App-Lock-App"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    data class UpdateInfo(val version: String, val downloadUrl: String, val releaseNotes: String)

    private fun openConnection(url: String): HttpURLConnection {
        require(URL(url).protocol == "https") { "Updates require HTTPS" }
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(API_URL)
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            currentCoroutineContext().ensureActive()
            val json = JSONObject(response)
            val tagName = json.getString("tag_name")
            if (!ReleaseVersion.isNewer(tagName, currentVersion)) return@withContext null

            val assets = json.getJSONArray("assets")
            val apkUrls = (0 until assets.length()).map {
                assets.getJSONObject(it).getString("browser_download_url")
            }.filter { it.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkUrls.firstOrNull { it.contains("universal", ignoreCase = true) }
                ?: apkUrls.firstOrNull() ?: return@withContext null
            UpdateInfo(tagName, downloadUrl, json.optString("body", "New update available for SafeLock!"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("AutoUpdater", "Unable to check for updates")
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** True means the installer was opened, not that Android completed installation. */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var partialFile: File? = null
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: error("Download storage is unavailable")
            check(downloadDir.isDirectory || downloadDir.mkdirs())
            val outputFile = File(downloadDir, "safelock_update.apk")
            val partial = File(downloadDir, "safelock_update.apk.part")
            partialFile = partial
            connection = openConnection(downloadUrl)
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            val fileLength = connection.contentLengthLong
            var total = 0L
            var lastProgress = -1
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val data = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(data)
                        if (count == -1) break
                        output.write(data, 0, count)
                        total += count
                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).coerceIn(0, 100).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                }
            }
            check(total > 0 && (fileLength < 0 || total == fileLength)) { "Incomplete download" }
            currentCoroutineContext().ensureActive()
            check(!outputFile.exists() || outputFile.delete())
            check(partial.renameTo(outputFile))
            withContext(Dispatchers.Main) {
                onProgress(100)
                installApk(context, outputFile)
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("AutoUpdater", "Unable to download or open update")
            false
        } finally {
            partialFile?.delete()
            connection?.disconnect()
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
