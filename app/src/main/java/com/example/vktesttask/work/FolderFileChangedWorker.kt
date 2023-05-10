package com.example.vktesttask.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.vktesttask.CustomApplication
import com.example.vktesttask.data.entity.FolderEntity
import com.example.vktesttask.data.repository.FolderRepository
import com.example.vktesttask.util.MD5
import com.example.vktesttask.util.getAllFilesFlow
import com.example.vktesttask.util.getStorages
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class FolderFileChangedWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FolderRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            getStorages(applicationContext).forEach { storage ->
                getAllFilesFlow(storage).collect { file ->
                    val md5 = MD5.calculateMD5(file) // Предполагается, что вы имеете в виду файл, а не 'it'
                    val folderEntity = FolderEntity(file.path, md5)
                    repository.insert(folderEntity)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return (appContext as CustomApplication).foregroundInfo
    }

    companion object {
        fun startUpFilesChangedWork() = OneTimeWorkRequestBuilder<FolderFileChangedWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
