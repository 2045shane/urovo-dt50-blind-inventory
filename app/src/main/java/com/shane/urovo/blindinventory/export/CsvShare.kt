package com.shane.urovo.blindinventory.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shane.urovo.blindinventory.R
import com.shane.urovo.blindinventory.ui.CsvPayload
import java.io.File

object CsvShare {
    fun share(context: Context, payload: CsvPayload) {
        val directory = File(context.cacheDir, "exports")
        val file = ExportFiles.allocate(directory, payload.fileName)
        file.writeText(payload.csv, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(file.name, uri)
        }
        val matches = context.packageManager.queryIntentActivities(
            send,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        for (info in matches) {
            context.grantUriPermission(
                info.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        try {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_title)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.share_saved, file.absolutePath),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
