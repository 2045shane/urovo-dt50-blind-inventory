package com.shane.urovo.blindinventory.session

import java.time.Instant

data class TagRecord(
    val epc: String,
    val count: Int,
    val firstSeen: Instant,
    val lastSeen: Instant,
    val lastRssi: Int?,
)
