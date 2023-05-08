package com.example.vktesttask.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder")
data class FolderEntity(
    @PrimaryKey
    val path: String,
    val hash: String,
)
