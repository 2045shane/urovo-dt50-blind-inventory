package com.shane.urovo.blindinventory.export

import java.io.File

object ExportFiles {
    fun allocate(directory: File, preferredName: String): File {
        require(!preferredName.contains('/') && !preferredName.contains('\\')) {
            "export file name must not contain a path separator"
        }
        directory.mkdirs()
        val direct = File(directory, preferredName)
        if (!direct.exists()) return direct
        val dot = preferredName.lastIndexOf('.')
        val stem = if (dot > 0) preferredName.substring(0, dot) else preferredName
        val ext = if (dot > 0) preferredName.substring(dot) else ""
        var suffix = 2
        while (true) {
            val candidate = File(directory, "$stem-$suffix$ext")
            if (!candidate.exists()) return candidate
            suffix += 1
        }
    }
}
