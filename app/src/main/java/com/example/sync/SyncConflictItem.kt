package com.example.sync

/** Une révision concurrente conservée pour consultation et arbitrage explicite. */
data class SyncConflictItem(
    val key: String,
    val kind: String,
    val uid: String,
    val label: String,
    val localValue: String,
    val incomingValue: String,
    val localUpdatedAt: Long,
    val incomingUpdatedAt: Long,
    val pendingBundleFile: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val resolution: String = "",
    val resolvedAt: Long = 0L
)
