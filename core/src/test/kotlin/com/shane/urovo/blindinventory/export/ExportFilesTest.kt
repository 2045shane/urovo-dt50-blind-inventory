package com.shane.urovo.blindinventory.export

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFilesTest {
    @Test
    fun allocateUsesPreferredNameThenNumericSuffix() {
        val dir = Files.createTempDirectory("blind-export").toFile()
        val first = ExportFiles.allocate(dir, "blind-inventory-20261102-130000.csv")
        assertEquals("blind-inventory-20261102-130000.csv", first.name)
        first.writeText("a", Charsets.UTF_8)
        val second = ExportFiles.allocate(dir, "blind-inventory-20261102-130000.csv")
        assertEquals("blind-inventory-20261102-130000-2.csv", second.name)
        second.writeText("b", Charsets.UTF_8)
        val third = ExportFiles.allocate(dir, "blind-inventory-20261102-130000.csv")
        assertEquals("blind-inventory-20261102-130000-3.csv", third.name)
    }
}
