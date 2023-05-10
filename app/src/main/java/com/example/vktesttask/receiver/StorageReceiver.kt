package com.example.vktesttask.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StorageReceiver(private val onStorageReceived: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MEDIA_MOUNTED) {
            onStorageReceived()
        }
    }
}