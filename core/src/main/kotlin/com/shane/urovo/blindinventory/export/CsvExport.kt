package com.shane.urovo.blindinventory.export

import com.shane.urovo.blindinventory.session.TagRecord
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * UTF-8 CSV，无 BOM。表头固定为 [HEADER]，列顺序不可增删。
 *
 * 时间是设备本地时区的 ISO-8601，秒精度，模式 `yyyy-MM-dd'T'HH:mm:ssXXX`。
 * 例如上海时区 `2026-11-02T13:00:00+08:00`；零时区写成 `Z`（ISO-8601）。
 * 行序是首次读到的顺序。屏幕列表是最近读到在前，两者集合一致、排序不同。
 */
object CsvExport {
    const val HEADER: String = "epc,count,first_seen,last_seen"

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    private val fileStampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun render(records: List<TagRecord>, zone: ZoneId): String {
        val body = StringBuilder()
        body.append(HEADER).append('\n')
        for (record in records) {
            body.append(escape(record.epc)).append(',')
            body.append(record.count).append(',')
            body.append(formatTimestamp(record.firstSeen, zone)).append(',')
            body.append(formatTimestamp(record.lastSeen, zone)).append('\n')
        }
        return body.toString()
    }

    fun toUtf8Bytes(records: List<TagRecord>, zone: ZoneId): ByteArray =
        render(records, zone).toByteArray(StandardCharsets.UTF_8)

    fun formatTimestamp(instant: Instant, zone: ZoneId): String =
        timestampFormatter.format(instant.atZone(zone))

    /** 不含路径分隔符。同一秒内重名由 [ExportFiles] 加后缀。 */
    fun fileName(instant: Instant, zone: ZoneId): String {
        val stamp = fileStampFormatter.format(instant.atZone(zone))
        return "blind-inventory-$stamp.csv"
    }

    fun escape(field: String): String {
        val quote = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!quote) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
