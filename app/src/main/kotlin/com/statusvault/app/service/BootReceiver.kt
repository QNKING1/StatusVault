package com.statusvault.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.statusvault.app.worker.StatusScanWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule the periodic status scan work
            StatusScanWorker.enqueuePeriodicWork(context)
        }
    }
}
