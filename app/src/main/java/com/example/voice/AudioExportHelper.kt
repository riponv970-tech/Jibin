package com.example.voice

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface SaveDownloadResult {
    data class Saved(val displayName: String, val uri: Uri) : SaveDownloadResult
    data class NeedsDocumentPicker(val suggestedName: String, val mimeType: String) : SaveDownloadResult
    data class Failed(val errorMessage: String) : SaveDownloadResult
}

object AudioExportHelper {

    suspend fun saveToDownloads(
        context: Context,
        clip: GeneratedAudioClip
    ): SaveDownloadResult = withContext(Dispatchers.IO) {
        if (!clip.file.exists() || clip.file.length() == 0L) {
            return@withContext SaveDownloadResult.Failed("অডিও ফাইল পাওয়া যায়নি।")
        }

        val timestamp = System.currentTimeMillis() / 1000
        val fileName = "jibon_voice_${clip.language.code}_$timestamp.${clip.extension}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, clip.mimeType)
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/JibonVoice"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                    ?: return@withContext SaveDownloadResult.Failed("Downloads ফোল্ডারে ফাইল তৈরি করা যায়নি।")

                resolver.openOutputStream(uri)?.use { out ->
                    clip.file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return@withContext SaveDownloadResult.Failed("ফাইল লেখার সময় সমস্যা হয়েছে।")
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                SaveDownloadResult.Saved(fileName, uri)
            } catch (e: Exception) {
                SaveDownloadResult.Failed(e.localizedMessage ?: "সেভ করতে সমস্যা হয়েছে।")
            }
        } else {
            SaveDownloadResult.NeedsDocumentPicker(
                suggestedName = fileName,
                mimeType = clip.mimeType
            )
        }
    }

    suspend fun copyFileToUri(
        context: Context,
        sourceFile: File,
        targetUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: error("Output stream unavailable")
            Unit
        }
    }

    fun buildShareIntent(context: Context, clip: GeneratedAudioClip): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            clip.file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = clip.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Jibon Voice - ${clip.language.nativeLabel}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
