package com.shane.urovo.blindinventory.session

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventorySessionTest {
    private var now: Instant = Instant.parse("2026-11-02T05:00:00Z")

    @Test
    fun duplicateEpcIncrementsCountAndKeepsUnique() {
        val session = scanningSession()
        assertTrue(session.onTag("E2801", rssi = -60, at = now))
        now = now.plusSeconds(5)
        assertTrue(session.onTag("E2801", rssi = -42, at = now))
        assertTrue(session.onTag("E2802", rssi = null, at = now))

        assertEquals(2, session.uniqueCount())
        assertEquals(3, session.totalReads())
        val first = session.records()[0]
        assertEquals("E2801", first.epc)
        assertEquals(2, first.count)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z"), first.firstSeen)
        assertEquals(now, first.lastSeen)
        assertEquals(-42, first.lastRssi)
        assertEquals(null, session.records()[1].lastRssi)
    }

    @Test
    fun tagsAreIgnoredUntilScanningAndWhilePaused() {
        val session = InventorySession { now }
        assertFalse(session.onTag("E2801", -50))
        assertEquals(0, session.uniqueCount())

        session.start()
        assertTrue(session.onTag("E2801", -50))
        session.stop()
        assertEquals(SessionPhase.PAUSED, session.phase)
        assertFalse(session.onTag("E2801", -40))
        assertFalse(session.onTag("E2809", -40))
        assertEquals(1, session.uniqueCount())
        assertEquals(1, session.records().single().count)
    }

    @Test
    fun stopThenStartContinuesSameSession() {
        val session = scanningSession()
        session.onTag("E2801", -50, now)
        session.stop()
        session.onTag("E2801", -10, now.plusSeconds(1))
        session.start()
        assertEquals(SessionPhase.SCANNING, session.phase)
        assertTrue(session.onTag("E2801", -33, now.plusSeconds(2)))
        assertEquals(1, session.uniqueCount())
        assertEquals(2, session.records().single().count)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z"), session.records().single().firstSeen)
    }

    @Test
    fun clearIsTheOnlyNewSession() {
        val session = scanningSession()
        session.onTag("E2801", -50)
        session.clear()
        assertEquals(SessionPhase.IDLE, session.phase)
        assertEquals(0, session.uniqueCount())
        assertFalse(session.onTag("E2801", -50))
        assertEquals(0, session.totalReads())
    }

    @Test
    fun blankEpcIsDroppedAndTrimmedTextDedupes() {
        val session = scanningSession()
        assertFalse(session.onTag("   ", -50))
        assertTrue(session.onTag("  E2801  ", -50))
        assertTrue(session.onTag("E2801", -40))
        assertEquals(1, session.uniqueCount())
        assertEquals(2, session.records().single().count)
        assertEquals("E2801", session.records().single().epc)
    }

    @Test
    fun caseIsPreserved() {
        val session = scanningSession()
        session.onTag("e2801", -50)
        session.onTag("E2801", -50)
        assertEquals(2, session.uniqueCount())
    }

    @Test
    fun recordsStayInFirstSeenOrderAndRecentIsLastSeenFirst() {
        val session = scanningSession()
        session.onTag("A", -50, now)
        session.onTag("B", -50, now.plusSeconds(1))
        session.onTag("A", -40, now.plusSeconds(2))
        assertEquals(listOf("A", "B"), session.records().map { it.epc })
        assertEquals(listOf("A", "B"), session.recent(8).map { it.epc })
        assertEquals(listOf("A"), session.recent(1).map { it.epc })
    }

    private fun scanningSession(): InventorySession {
        val session = InventorySession { now }
        session.start()
        return session
    }
}
