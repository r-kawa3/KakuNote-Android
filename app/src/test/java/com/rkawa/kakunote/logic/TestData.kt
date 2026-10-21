package com.rkawa.kakunote.logic

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.sql.DriverManager

/** 単体テストで使う漢字 DB（アプリに同梱するものと同じ src/main/assets/kanji.sqlite）。 */
object TestData {

    val kanji: KanjiData by lazy {
        val path = System.getProperty("kakunote.kanjiDb") ?: error("kakunote.kanjiDb が設定されていません")
        JdbcSqlReader(path).use { KanjiData.load(it) }
    }
}

/** JDBC（sqlite-jdbc）で DB を読む。JVM の単体テストでは Android の SQLite が使えないため。 */
class JdbcSqlReader(path: String) : SqlReader, AutoCloseable {

    private val connection = DriverManager.getConnection("jdbc:sqlite:file:$path?immutable=1")

    override fun forEachRow(sql: String, body: (SqlRow) -> Unit) {
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { result ->
                val row = object : SqlRow {
                    override fun text(column: Int): String = result.getString(column + 1) ?: ""
                    override fun int(column: Int): Int = result.getInt(column + 1)
                }
                while (result.next()) body(row)
            }
        }
    }

    override fun close() = connection.close()
}

/** リポジトリ直下の testcases/ にある共通テストケース（iOS のリポジトリからコピーしたもの）を読む。 */
object SharedTestCases {

    fun load(name: String): JSONObject {
        val dir = System.getProperty("kakunote.testcases") ?: error("kakunote.testcases が設定されていません")
        return JSONObject(File(dir, name).readText())
    }
}

fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }

/** コードポイントの並び。比べたときに食い違いが読みやすいように、文字列を分けて見せる。 */
fun codePoints(text: String): List<Int> = text.codePoints().toArray().toList()
