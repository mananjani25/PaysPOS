package com.pays.pos.ui.fragments.allorders

data class PendingCounts(
    val allPendingCount: Int,
    val openPendingCount: Int,
    val phonePendingCount: Int,
    val onlinePendingCount: Int,
    val thirdPartyPendingCount: Int,
)
