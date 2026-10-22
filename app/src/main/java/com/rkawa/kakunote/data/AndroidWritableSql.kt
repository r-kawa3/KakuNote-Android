package com.rkawa.kakunote.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Android の SQLite で、読み書きする（お気に入りなど、端末の中に保存するもの）。 */
class AndroidWritableSql(file: File) : WritableSql, AutoCloseable {

    private val database = SQLiteDatabase.openOrCreateDatabase(file, null)

    override fun execute(sql: String, args: List<Any>) {
        database.execSQL(sql, args.toTypedArray())
    }

    override fun query(sql: String, args: List<Any>, row: (List<Any?>) -> Unit) {
        database.rawQuery(sql, args.map { it.toString() }.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                row((0 until cursor.columnCount).map { column ->
                    when (cursor.getType(column)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column)
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(column)
                        else -> cursor.getString(column)
                    }
                })
            }
        }
    }

    override fun changes(): Int {
        var count = 0
        database.rawQuery("SELECT changes()", null).use { if (it.moveToFirst()) count = it.getInt(0) }
        return count
    }

    override fun close() = database.close()
}
