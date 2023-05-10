package com.example.vktesttask.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vktesttask.data.repository.FolderRepository
import com.example.vktesttask.model.FileType
import com.example.vktesttask.model.SortingType
import com.example.vktesttask.util.MD5
import com.example.vktesttask.util.getStorages
import com.example.vktesttask.util.toFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(private val repository: FolderRepository) : ViewModel() {
    private val _uiState: MutableStateFlow<FolderUiState> = MutableStateFlow(
        FolderUiState(
            emptyList(),
            SortingType.NAME_ASC,
            null,
            null,
            emptyList(),
            0
        )
    )
    val uiState: StateFlow<FolderUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.collect {
                folderUpdate(it)
            }
        }
    }

    private suspend fun folderUpdate(it: FolderUiState) {
        if (it.selectedIndex != null) {
            val currentFile: File?

            if (it.currentFile == null || it.selectedIndex != it.prevSelectedIndex) {
                currentFile = it.storages[it.selectedIndex]
            } else {
                currentFile = it.currentFile
            }

            val data = currentFile.listFiles()?.map {
                val hash = repository.getByPath(it.path)?.hash

                if (hash != null) {
                    it.toFolder().copy(
                        isChanged = hash != MD5.calculateMD5(it)
                    )
                } else {
                    it.toFolder()
                }
            } ?: emptyList()
            _uiState.value = _uiState.value.copy(
                currentFile = currentFile,
                data = data,
                prevSelectedIndex = it.selectedIndex
            )
        }

        sortBy(it.sortingType)
    }

    fun initializeStorages(context: Context) {
        _uiState.value = _uiState.value.copy(
            storages = getStorages(context),
            selectedIndex = 0,
            sortingType = SortingType.NAME_ASC
        )
    }

    fun updateSortingType(sortingType: SortingType) {
        _uiState.value = _uiState.value.copy(
            sortingType = sortingType
        )
    }

    private fun sortBy(sortingType: SortingType) {
        val value = _uiState.value.data

        _uiState.value = _uiState.value.copy(
            data = when (sortingType) {
                SortingType.NAME_ASC -> value.sortedBy { it.name }
                SortingType.NAME_DESC -> value.sortedByDescending { it.name }
                SortingType.SIZE_ASC -> value.sortedBy { it.size }
                SortingType.SIZE_DESC -> value.sortedByDescending { it.size }
                SortingType.CREATION_DATE_ASC -> value.sortedBy { it.creationDate }
                SortingType.CREATION_DATE_DESC -> value.sortedByDescending { it.creationDate }
                SortingType.EXTENSION_ASC -> value.sortedBy { it.extension }
                SortingType.EXTENSION_DESC -> value.sortedByDescending { it.extension }
            }.sortedBy { it.fileType != FileType.DIR }
        )
    }

    fun updateStorage(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index)
    }

    suspend fun updateFolder() {
        folderUpdate(_uiState.value)
    }

    fun navigateUp() {
        val parentFile = _uiState.value.currentFile?.parentFile

        _uiState.value = _uiState.value.copy(currentFile = parentFile)
    }

    fun canNavigateUp(): Boolean {
        return _uiState.value.currentFile?.parentFile != null
    }

    fun navigateTo(file: File) {
        if (file.isDirectory) {
            _uiState.value = _uiState.value.copy(currentFile = file)
        }
    }
}
