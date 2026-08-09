package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareManager {

    /**
     * Shares a single PDF or Image file using native Android dynamic share sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Document") {
        if (!file.exists()) return

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }

    /**
     * Shares multiple files (e.g. pages as JPG/PNG) using native dynamic share sheet.
     */
    fun shareMultipleFiles(context: Context, files: List<File>, mimeType: String, title: String = "Share Pages") {
        if (files.isEmpty()) return

        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        for (file in files) {
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(context, authority, file))
            }
        }

        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }

    /**
     * Shares a web link or text message using native Android share sheet.
     */
    fun shareTextLink(context: Context, linkText: String, title: String = "Share Web Link") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, linkText)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }
}
