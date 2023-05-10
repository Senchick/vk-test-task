package com.example.vktesttask.viewmodel

import com.example.vktesttask.MainActivity
import com.example.vktesttask.model.Folder
import com.example.vktesttask.model.SortingType
import com.example.vktesttask.ui.adapter.FolderAdapter
import java.io.File

data class FolderUiState(
    val data: List<Folder>,
    val sortingType: SortingType,
    val selectedIndex: Int?,
    val currentFile: File?,
    val storages: List<File>,
    val prevSelectedIndex: Int
)

