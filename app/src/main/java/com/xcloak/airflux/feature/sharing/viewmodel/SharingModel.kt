package com.xcloak.airflux.feature.sharing.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.domain.model.SelectedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharingViewModel : ViewModel() {

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    fun addFiles(context: Context, uris: List<Uri>) {
        val resolved = uris.mapNotNull { FileUtils.resolveSelectedFile(context, it) }
        _selectedFiles.value = _selectedFiles.value + resolved
    }

    fun removeFile(file: SelectedFile) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != file.uri }
    }

    fun clearAll() {
        _selectedFiles.value = emptyList()
    }
}