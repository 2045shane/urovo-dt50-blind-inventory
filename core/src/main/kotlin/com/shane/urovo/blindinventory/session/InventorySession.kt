package com.shane.urovo.blindinventory.session

import java.time.Instant

/**
 * 一场盘点的 EPC 去重表。
 *
 * A4 写死：**停止后再开始 = 继续同一场**，不新建、不清空。
 * 只有 [clear] 结束本场并清空。未处于 [SessionPhase.SCANNING] 时收到的标签直接丢弃。
 *
 * 去重键：去掉首尾空白后的 EPC 原文，区分大小写，不改写十六进制大小写。
 */
class InventorySession(
    private val clock: () -> Instant = { Instant.now() },
) {
    private val byEpc = LinkedHashMap<String, MutableTag>()

    var phase: SessionPhase = SessionPhase.IDLE
        private set

    fun start() {
        phase = SessionPhase.SCANNING
    }

    fun stop() {
        if (phase == SessionPhase.SCANNING) {
            phase = SessionPhase.PAUSED
        }
    }

    fun clear() {
        byEpc.clear()
        phase = SessionPhase.IDLE
    }

    /** @return 写入了本场则为 true（新标签或 count+1）。 */
    fun onTag(epcId: String, rssi: Int?, at: Instant = clock()): Boolean {
        if (phase != SessionPhase.SCANNING) return false
        val epc = epcId.trim()
        if (epc.isEmpty()) return false
        val existing = byEpc[epc]
        if (existing == null) {
            byEpc[epc] = MutableTag(
                epc = epc,
                count = 1,
                firstSeen = at,
                lastSeen = at,
                lastRssi = rssi,
            )
        } else {
            existing.count += 1
            existing.lastSeen = at
            existing.lastRssi = rssi
        }
        return true
    }

    fun uniqueCount(): Int = byEpc.size

    fun totalReads(): Int = byEpc.values.sumOf { it.count }

    /** 首次出现顺序，供 CSV。 */
    fun records(): List<TagRecord> = byEpc.values.map { it.toRecord() }

    /** 最近读到的在前，供屏幕。同一时刻保持原相对顺序。 */
    fun recent(limit: Int): List<TagRecord> {
        require(limit >= 0)
        return byEpc.values
            .sortedByDescending { it.lastSeen }
            .take(limit)
            .map { it.toRecord() }
    }

    private class MutableTag(
        val epc: String,
        var count: Int,
        val firstSeen: Instant,
        var lastSeen: Instant,
        var lastRssi: Int?,
    ) {
        fun toRecord() = TagRecord(epc, count, firstSeen, lastSeen, lastRssi)
    }
}

enum class SessionPhase {
    IDLE,
    SCANNING,
    PAUSED,
}
