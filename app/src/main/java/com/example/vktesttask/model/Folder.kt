package com.example.vktesttask.model

import java.io.File

data class Folder(
    val fileType: FileType,
    val creationDate: Long,
    val name: String,
    val file: File,
    val size: Long? = null,
    val extension: String? = null,
    val subFiles: Int? = null,
    val isChanged: Boolean = false
)
