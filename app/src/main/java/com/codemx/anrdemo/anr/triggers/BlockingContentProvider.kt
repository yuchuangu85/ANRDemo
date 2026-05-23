package com.codemx.anrdemo.anr.triggers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class BlockingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val blockMs = uri.getQueryParameter("blockMs")?.toLongOrNull() ?: AnrDefaults.PROVIDER_BLOCK_MS
        Log.d(AnrLogTags.TRIGGER, "BlockingContentProvider query blockMs=$blockMs thread=${Thread.currentThread().name}")
        SystemClock.sleep(blockMs)
        return MatrixCursor(arrayOf("scenario", "blockedMs")).apply {
            addRow(arrayOf<Any?>("content-provider", blockMs))
        }
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.com.codemx.anrdemo.blocking"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
