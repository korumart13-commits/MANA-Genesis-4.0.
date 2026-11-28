package com.mana.launcher

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

/** Read-only runtime budget snapshot; never claims to reserve or unlock system RAM. */
object ManaResourceGovernor {
    data class Snapshot(
        val totalRamMb: Long,
        val availableRamMb: Long,
        val appPssMb: Long,
        val lowMemory: Boolean,
        val recommendedProfile: String
    )

    fun snapshot(context: Context): Snapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        val pss = Debug.MemoryInfo()
        Debug.getMemoryInfo(pss)
        val total = memory.totalMem / (1024L * 1024L)
        val available = memory.availMem / (1024L * 1024L)
        val appPss = pss.totalPss.toLong() / 1024L
        val profile = when {
            memory.lowMemory || available < total * 0.12 -> "ЭКОНОМНЫЙ: выгружать необязательные модули и уменьшить контекст модели"
            available < total * 0.25 -> "СБАЛАНСИРОВАННЫЙ: ограничить параллельные задачи"
            else -> "ПОЛНЫЙ: можно запускать доступные модули по запросу"
        }
        return Snapshot(total, available, appPss, memory.lowMemory, profile)
    }
}
