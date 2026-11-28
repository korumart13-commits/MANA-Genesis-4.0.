package com.mana.launcher

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

/** Read-only process/device memory snapshot; this reports usage and does not claim to control it. */
data class ManaResourceSnapshot(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val appPssMb: Long
)

object ManaResourceSnapshotReader {
    fun read(context: Context): ManaResourceSnapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: error("Android ActivityManager is unavailable.")
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        val processMemory = Debug.MemoryInfo()
        Debug.getMemoryInfo(processMemory)
        return ManaResourceSnapshot(
            availableRamMb = memory.availMem / BYTES_PER_MIB,
            totalRamMb = memory.totalMem / BYTES_PER_MIB,
            appPssMb = processMemory.totalPss.toLong() / KIB_PER_MIB
        )
    }

    private const val BYTES_PER_MIB = 1024L * 1024L
    private const val KIB_PER_MIB = 1024L
}
