package com.focusly.app.feature.shell

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.R
import com.focusly.app.domain.model.BackupImportResult
import com.focusly.app.domain.repository.LocalBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class BackupStatusUi {
    data object ExportSuccess : BackupStatusUi()
    data class ImportSuccess(val sessionsCount: Int, val tasksCount: Int, val subjectsCount: Int) : BackupStatusUi()
    data class Error(val reason: String) : BackupStatusUi()
}

@HiltViewModel
class ProfileBackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localBackupRepository: LocalBackupRepository
) : ViewModel() {

    private val _status = MutableStateFlow<BackupStatusUi?>(null)
    val status: StateFlow<BackupStatusUi?> = _status.asStateFlow()

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = localBackupRepository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file")
                }
            }.onSuccess {
                _status.value = BackupStatusUi.ExportSuccess
            }.onFailure { err ->
                _status.value = BackupStatusUi.Error(err.message ?: appContext.getString(R.string.backup_import_error, "Export failed"))
            }
        }
    }

    fun importBackupFromUri(uri: Uri, onRestored: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val rawJson = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Unable to read selected file")
                }
                localBackupRepository.importBackupJson(rawJson)
            }.onSuccess { result ->
                when (result) {
                    is BackupImportResult.Success -> {
                        _status.value = BackupStatusUi.ImportSuccess(
                            sessionsCount = result.sessionsCount,
                            tasksCount = result.tasksCount,
                            subjectsCount = result.subjectsCount
                        )
                        onRestored()
                    }
                    is BackupImportResult.InvalidFormat -> {
                        _status.value = BackupStatusUi.Error(result.reason)
                    }
                }
            }.onFailure { err ->
                _status.value = BackupStatusUi.Error(err.message ?: "Unable to read backup file")
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
