package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AutoUpdater {
    // TODO: Replace with your actual GitHub username and repository name to enable auto-updates
    // For example: "paionorr/safelock"
    private const val GITHUB_REPO = "YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    
    data class UpdateInfo(val version: String, val downloadUrl: String, val releaseNotes: String)

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        if (GITHUB_REPO == "YOUR_GITHUB_USERNAME/YOUR_REPO_NAME") return@withContext null
        
        try {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name") // e.g., "v1.0.1"
                
                val cleanLatest = tagName.removePrefix("v")
                val cleanCurrent = currentVersion.removePrefix("v")
                
                // Extremely simple version string mismatch check. 
                // For proper semantic versioning you'd parse components.
                if (cleanLatest != cleanCurrent && cleanLatest.isNotEmpty()) {
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        var downloadUrl = ""
                        for (i in 0 until assets.length()) {
                            val url = assets.getJSONObject(i).getString("browser_download_url")
                            if (url.contains("universal", ignoreCase = true)) {
                                downloadUrl = url
                                break
                            }
                        }
                        // Fallback to the first asset if universal is not found
                        if (downloadUrl.isEmpty()) {
                            downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        }
                        
                        val body = json.optString("body", "New update available for SafeLock!")
                        return@withContext UpdateInfo(tagName, downloadUrl, body)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AutoUpdater", "Failed to check for update: ${e.message}", e)
        }
        return@withContext null
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            
            val fileLength = connection.contentLength
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && !downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val outputFile = File(downloadDir, "safelock_update.apk")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            val input = connection.inputStream
            val output = FileOutputStream(outputFile)
            
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    withContext(Dispatchers.Main) {
                        onProgress((total * 100 / fileLength).toInt())
                    }
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
            
            installApk(context, outputFile)
        } catch (e: Exception) {
            Log.e("AutoUpdater", "Failed to download update: ${e.message}", e)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
