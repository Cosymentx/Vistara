package com.obscura.wallpapers.wallpaper.cycler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UnlockCyclerReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cyclerController: CyclerController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            CoroutineScope(Dispatchers.IO).launch {
                cyclerController.performAutoChange()
            }
        }
    }
}
