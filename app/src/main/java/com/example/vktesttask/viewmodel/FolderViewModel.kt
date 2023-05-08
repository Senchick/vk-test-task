package com.example.vktesttask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vktesttask.model.Folder
import com.example.vktesttask.model.SortingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor() : ViewModel() {
    private val _data = MutableStateFlow<List<Folder>>(listOf())
    val data: StateFlow<List<Folder>> = _data

    private val _sortingType = MutableStateFlow(SortingType.NAME_ASC)

    init {
        viewModelScope.launch {
            _sortingType.collect {
                sortBy(it)
            }
        }
    }

    fun updateSortingType(sortingType: SortingType) {
        _sortingType.value = sortingType
    }

    private fun sortBy(sortingType: SortingType) {
        val value = _data.value

        _data.value = when (sortingType) {
            SortingType.NAME_ASC -> value.sortedBy { it.name }
            SortingType.NAME_DESC -> value.sortedByDescending { it.name }
            SortingType.SIZE_ASC -> value.sortedBy { it.size }
            SortingType.SIZE_DESC -> value.sortedByDescending { it.size }
            SortingType.CREATION_DATE_ASC -> value.sortedBy { it.creationDate }
            SortingType.CREATION_DATE_DESC -> value.sortedByDescending { it.creationDate }
            SortingType.EXTENSION_ASC -> value.sortedBy { it.extension }
            SortingType.EXTENSION_DESC -> value.sortedByDescending { it.extension }
        }
    }

    fun updateData(data: List<Folder>) {
        _data.value = data
    }
}