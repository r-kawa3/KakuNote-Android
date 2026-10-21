package com.rkawa.kakunote.data

import android.database.sqlite.SQLiteDatabase
import com.rkawa.kakunote.logic.SqlReader
import com.rkawa.kakunote.logic.SqlRow
import java.io.File

/** Android の SQLite で、DB を読み取り専用で読む。 */
class AndroidSqlReader(file: File) : SqlReader, AutoCloseable {

    private val database = SQLiteDatabase.openDatabase(
        file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    )

    override fun forEachRow(sql: String, body: (SqlRow) -> Unit) {
        database.rawQuery(sql, null).use { cursor ->
            val row = object : SqlRow {
                override fun text(column: Int): String = cursor.getString(column) ?: ""
                override fun int(column: Int): Int = cursor.getInt(column)
            }
            while (cursor.moveToNext()) body(row)
        }
    }

    override fun close() = database.close()
}
