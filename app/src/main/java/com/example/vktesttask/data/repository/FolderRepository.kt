package com.example.vktesttask.data.repository

import com.example.vktesttask.data.dao.FolderDao
import com.example.vktesttask.data.entity.FolderEntity
import javax.inject.Inject

class FolderRepository @Inject constructor(private val dao: FolderDao) {

    suspend fun getAll() = dao.getAll()
    suspend fun insertFolders(folders: List<FolderEntity>) = dao.insertFolders(folders)
    suspend fun insert(folder: FolderEntity) = dao.insert(folder)
    suspend fun getByPath(path: String) = dao.getByPath(path)
}
