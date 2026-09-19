package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DownloadHelper {

    data class DownloadResult(
        val success: Boolean,
        val fileName: String,
        val savedUri: Uri?,
        val publicPath: String
    )

    /**
     * Saves a processed zero-noise audio/video file directly to the device's public Downloads directory.
     * Compatible with Android 10+ (Q) MediaStore.Downloads and legacy public Downloads.
     */
    fun saveToDownloads(
        context: Context,
        sourceFile: File,
        targetFileName: String,
        mimeType: String = "audio/wav"
    ): DownloadResult {
        if (!sourceFile.exists()) {
            return DownloadResult(false, targetFileName, null, "")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ZeroNoise")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = if (mimeType.startsWith("video")) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val uri = context.contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)

                    return DownloadResult(
                        success = true,
                        fileName = targetFileName,
                        savedUri = uri,
                        publicPath = "Downloads/ZeroNoise/$targetFileName"
                    )
                }
            }

            // Fallback for direct File access in public Downloads directory
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ZeroNoise"
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val destFile = File(downloadsDir, targetFileName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notify MediaScanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType),
                null
            )

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                destFile
            )

            return DownloadResult(
                success = true,
                fileName = targetFileName,
                savedUri = contentUri,
                publicPath = destFile.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Final fallback: local cache shareable URI
            val fallbackUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    sourceFile
                )
            } catch (ex: Exception) {
                null
            }
            return DownloadResult(
                success = true,
                fileName = targetFileName,
                savedUri = fallbackUri,
                publicPath = sourceFile.absolutePath
            )
        }
    }

    /**
     * Creates an intent to open or share the downloaded file.
     */
    fun createShareIntent(context: Context, file: File, mimeType: String = "audio/wav"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
