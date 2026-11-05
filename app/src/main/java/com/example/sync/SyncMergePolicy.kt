package com.example.sync

/** Content differences are conflicts; wall-clock timestamps alone never authorize an overwrite. */
internal object SyncMergePolicy {
    fun hasDiverged(local: Any, incoming: Any): Boolean = local != incoming
}
