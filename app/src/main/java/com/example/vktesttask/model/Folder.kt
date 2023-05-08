package com.example.vktesttask.model

data class Folder(
    val fileType: FileType,
    val creationDate: Long,
    val name: String,
    val path: String,
    val size: Long? = null,
    val extension: String? = null
)
