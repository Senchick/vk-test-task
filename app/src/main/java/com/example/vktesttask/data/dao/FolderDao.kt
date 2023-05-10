package com.example.vktesttask.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vktesttask.data.entity.FolderEntity

@Dao
interface FolderDao {
    @Query("select * from folder")
    suspend fun getAll(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Query("select * from folder where path=:path")
    suspend fun getByPath(path: String): FolderEntity?
}
