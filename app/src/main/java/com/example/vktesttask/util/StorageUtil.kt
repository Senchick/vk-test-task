package com.example.vktesttask.util

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.vktesttask.model.FileType
import com.example.vktesttask.model.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

suspend fun getAllFilesFlow(root: File): Flow<File> = flow {
    if (root.exists()) {
        if (root.isDirectory) {
            root.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    emitAll(getAllFilesFlow(file))
                } else {
                    emit(file)
                }
            }
        } else {
            emit(root)
        }
    }
}

fun getStorages(context: Context) = ContextCompat.getExternalFilesDirs(context, null).map {
    val storage = it.absolutePath
        .split("Android/data/")[0]

    File(storage)
}

fun File.getFileCreationTimeAndSize(): Pair<Long, Long> {
    val fileSize = length()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val path = toPath()
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
        Pair(attributes.creationTime().toMillis(), fileSize)
    } else {
        Pair(lastModified(), fileSize)
    }
}

fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var i = 0
    while (size > 1024 && i < units.size - 1) {
        size /= 1024
        i++
    }

    return String.format(Locale.ENGLISH, "%.2f %s", size, units[i])
}

fun File.getFileType(): FileType {
    if (isDirectory) {
        return FileType.DIR
    }
    // можно было использовать HashMap, но с when меньше кода
    return when (extension) {
        "mp4", "mov", "avi", "webm" -> FileType.VIDEO
        "jpeg", "jpg", "png", "webp" -> FileType.IMAGE
        "mp3", "ogg", "flac", "wav" -> FileType.AUDIO
        "txt" -> FileType.TEXT
        else -> FileType.FILE
    }
}

fun File.toFolder(): Folder {
    val timeAndSize = getFileCreationTimeAndSize()

    return Folder(
        fileType = getFileType(),
        creationDate = timeAndSize.first,
        size = timeAndSize.second,
        name = name,
        extension = extension,
        subFiles = listFiles()?.size,
        file = this
    )
}
