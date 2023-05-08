package com.example.vktesttask.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.vktesttask.data.dao.FolderDao
import com.example.vktesttask.data.entity.FolderEntity

@Database(entities = [FolderEntity::class], version = 0, exportSchema = false)
abstract class FolderDatabase : RoomDatabase() {
    abstract fun dao(): FolderDao
}