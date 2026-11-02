package com.shane.urovo.blindinventory.export

import com.shane.urovo.blindinventory.session.InventorySession
import com.shane.urovo.blindinventory.session.TagRecord
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val first = Instant.parse("2026-11-02T05:00:00Z")
    private val second = Instant.parse("2026-11-02T05:00:05Z")

    @Test
    fun headerAndLocalIsoTimestamps() {
        val session = InventorySession { first }
        session.start()
        session.onTag("E28011606000020900000001", -60, first)
        session.onTag("E28011606000020900000002", null, first.plusSeconds(1))
        session.onTag("E28011606000020900000001", -40, second)

        val csv = CsvExport.render(session.records(), shanghai)
        assertEquals(
            "epc,count,first_seen,last_seen\n" +
                "E28011606000020900000001,2,2026-11-02T13:00:00+08:00,2026-11-02T13:00:05+08:00\n" +
                "E28011606000020900000002,1,2026-11-02T13:00:01+08:00,2026-11-02T13:00:01+08:00\n",
            csv,
        )
        assertEquals("epc,count,first_seen,last_seen", csv.lineSequence().first())
        assertFalse(csv.startsWith("\uFEFF"))
    }

    @Test
    fun utcUsesZuluAndBytesHaveNoBom() {
        val records = listOf(
            TagRecord("ABC", 1, first, first, null),
        )
        val csv = CsvExport.render(records, ZoneId.of("UTC"))
        assertTrue(csv.contains("2026-11-02T05:00:00Z"))
        val bytes = CsvExport.toUtf8Bytes(records, ZoneId.of("UTC"))
        assertEquals('e'.code.toByte(), bytes[0])
        assertArrayEquals(csv.toByteArray(StandardCharsets.UTF_8), bytes)
    }

    @Test
    fun escapesCommaQuoteAndNewlineWithoutAddingColumns() {
        val weird = "EPC,\"x\"\nline"
        val csv = CsvExport.render(
            listOf(TagRecord(weird, 4, first, second, null)),
            shanghai,
        )
        assertEquals(
            "epc,count,first_seen,last_seen\n" +
                "\"EPC,\"\"x\"\"\nline\",4,2026-11-02T13:00:00+08:00,2026-11-02T13:00:05+08:00\n",
            csv,
        )
    }

    @Test
    fun emptySessionIsHeaderOnly() {
        assertEquals(
            "epc,count,first_seen,last_seen\n",
            CsvExport.render(emptyList(), shanghai),
        )
    }

    @Test
    fun fileNameContainsLocalDateTime() {
        assertEquals(
            "blind-inventory-20261102-130005.csv",
            CsvExport.fileName(second, shanghai),
        )
        assertEquals(
            "blind-inventory-20261102-050005.csv",
            CsvExport.fileName(second, ZoneId.of("UTC")),
        )
    }
}
