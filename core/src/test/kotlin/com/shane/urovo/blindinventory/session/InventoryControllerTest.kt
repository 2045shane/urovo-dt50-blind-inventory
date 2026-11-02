package com.shane.urovo.blindinventory.session

import com.shane.urovo.blindinventory.rfid.InitListener
import com.shane.urovo.blindinventory.rfid.MockRfidSdk
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryControllerTest {
    private var now: Instant = Instant.parse("2026-11-02T05:00:00Z")

    @Test
    fun injectWhileScanningDedupes() {
        val harness = harness()
        assertTrue(harness.controller.start())
        assertTrue(harness.sdk.inject("E2801", -55))
        assertTrue(harness.sdk.inject("E2801", -40))
        assertTrue(harness.sdk.inject("E2802", null))
        assertEquals(2, harness.session.uniqueCount())
        assertEquals(2, harness.session.records()[0].count)
        assertEquals(3, harness.session.totalReads())
        assertEquals(4, harness.changes)
    }

    @Test
    fun dripOnceEmitsOnlyWhileInventorying() {
        val harness = harness()
        assertFalse(harness.sdk.dripOnce())
        harness.controller.start()
        assertTrue(harness.sdk.dripOnce())
        assertEquals(1, harness.session.uniqueCount())
        assertEquals("E28011606000020900000001", harness.session.records().single().epc)
    }

    @Test
    fun stopThenRestartContinuesAndDropsTagsWhileStopped() {
        val harness = harness()
        harness.controller.start()
        harness.sdk.inject("E2801", -55)
        harness.controller.stop()
        assertEquals(SessionPhase.PAUSED, harness.session.phase)
        assertFalse(harness.sdk.inventorying)
        assertFalse(harness.sdk.inject("E2801", -10))
        assertFalse(harness.sdk.inject("E2809", -10))
        assertFalse(harness.sdk.dripOnce())
        assertEquals(1, harness.session.records().single().count)

        assertTrue(harness.controller.start())
        assertTrue(harness.sdk.inject("E2801", -22))
        assertEquals(1, harness.session.uniqueCount())
        assertEquals(2, harness.session.records().single().count)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z"), harness.session.records().single().firstSeen)
    }

    @Test
    fun clearStartsNewSessionAndIgnoresFurtherInjects() {
        val harness = harness()
        harness.controller.start()
        harness.sdk.inject("E2801", -55)
        harness.controller.clear()
        assertEquals(SessionPhase.IDLE, harness.session.phase)
        assertFalse(harness.sdk.inventorying)
        assertEquals(0, harness.session.uniqueCount())
        assertFalse(harness.sdk.inject("E2801", -55))
        assertEquals(0, harness.session.uniqueCount())
    }

    @Test
    fun failedInitRefusesStart() {
        val sdk = MockRfidSdk(pool = listOf("E2801"))
        sdk.failNextInit(message = "RFID 初始化失败：模拟模块被设置为失败")
        val session = InventorySession { now }
        val controller = InventoryController(session, sdk)
        var message = ""
        controller.init { success, text ->
            assertFalse(success)
            message = text
        }
        assertFalse(controller.start())
        assertEquals(SessionPhase.IDLE, session.phase)
        assertTrue(message.contains("初始化失败"))
    }

    @Test
    fun releaseDropsLaterTags() {
        val harness = harness()
        harness.controller.start()
        harness.controller.release()
        assertFalse(harness.sdk.inject("E2801", -20))
        assertEquals(0, harness.session.uniqueCount())
    }

    private fun harness(): Harness {
        val sdk = MockRfidSdk(pool = listOf("E28011606000020900000001"), random = kotlin.random.Random(1))
        val session = InventorySession { now }
        var changes = 0
        val controller = InventoryController(session, sdk) { changes += 1 }
        controller.init(InitListener { _, _ -> })
        return Harness(sdk, session, controller) { changes }
    }

    private class Harness(
        val sdk: MockRfidSdk,
        val session: InventorySession,
        val controller: InventoryController,
        private val changeCount: () -> Int,
    ) {
        val changes: Int get() = changeCount()
    }
}
