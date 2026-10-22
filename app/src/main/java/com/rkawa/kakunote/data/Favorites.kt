package com.rkawa.kakunote.data

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import org.json.JSONArray
import org.json.JSONObject

/** お気に入りの 1 件。 */
data class Favorite(
    val id: Long,
    /** 姓。姓なしで探した候補なら null */
    val sei: String?,
    val mei: String,
    /** 探したときの条件。わからなければ null */
    val query: SearchQuery?,
    /** 保存した日時（1970 年からのミリ秒） */
    val createdAt: Long,
)

/**
 * 書き込みもできる SQL の口。アプリでは Android の SQLite、単体テストでは JDBC を使う
 * （JVM の単体テストでは Android の SQLite が使えないため）。値は文字列か数で渡す。
 */
interface WritableSql {
    fun execute(sql: String, args: List<Any> = emptyList())

    /** 行を読む。行の列は 0 から */
    fun query(sql: String, args: List<Any> = emptyList(), row: (List<Any?>) -> Unit)

    /** 直前の execute で変わった行の数 */
    fun changes(): Int
}

/**
 * お気に入りの保存。同梱の漢字 DB とは別の SQLite ファイルに書く（同梱 DB を差し替えても消えない）。
 * 形は iOS の FavoriteStore と同じ。同じ姓・名は 1 件だけ（姓が違えば別）。
 */
class FavoriteStore(private val db: WritableSql) {

    init {
        migrate()
    }

    /** 保存したお気に入り。新しく保存したものが先。 */
    fun all(): List<Favorite> {
        val favorites = mutableListOf<Favorite>()
        db.query("SELECT id, sei, mei, query, created_at FROM favorite ORDER BY created_at DESC, id DESC") { row ->
            val sei = row[1] as? String ?: ""
            val queryJson = row[3] as? String ?: ""
            favorites.add(
                Favorite(
                    id = (row[0] as Number).toLong(),
                    sei = sei.ifEmpty { null },
                    mei = row[2] as? String ?: "",
                    query = if (queryJson.isEmpty()) null else SearchQueryCodec.decodeOrNull(queryJson),
                    createdAt = (row[4] as Number).toLong(),
                ),
            )
        }
        return favorites
    }

    /** 保存する。同じ姓・名がすでにあれば何もせず false を返す。 */
    fun add(sei: String?, mei: String, query: SearchQuery?, createdAt: Long = System.currentTimeMillis()): Boolean {
        db.execute(
            "INSERT OR IGNORE INTO favorite (sei, mei, query, created_at) VALUES (?, ?, ?, ?)",
            listOf(sei ?: "", mei, query?.let { SearchQueryCodec.encode(it) } ?: "", createdAt),
        )
        return db.changes() > 0
    }

    fun remove(sei: String?, mei: String) {
        db.execute("DELETE FROM favorite WHERE sei = ? AND mei = ?", listOf(sei ?: "", mei))
    }

    private fun migrate() {
        var version = 0
        db.query("PRAGMA user_version") { version = (it[0] as Number).toInt() }
        if (version < 1) {
            // sei と mei は文字列のまま（バイト列）で比べるので、正準等価な別の字も区別される
            db.execute(
                """
                CREATE TABLE IF NOT EXISTS favorite (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    sei        TEXT    NOT NULL DEFAULT '',
                    mei        TEXT    NOT NULL,
                    query      TEXT    NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    UNIQUE (sei, mei)
                )
                """.trimIndent(),
            )
        }
        // 形を変えるときは、SCHEMA_VERSION を上げて、ここに移行を足す
        db.execute("PRAGMA user_version = $SCHEMA_VERSION")
    }

    companion object {
        /** 保存の形のバージョン（PRAGMA user_version） */
        const val SCHEMA_VERSION = 1
        const val FILE_NAME = "favorites.sqlite"
    }
}

/**
 * 探したときの条件（SearchQuery）を JSON の文字列にする。形は共通テストケース（search.json の input）と同じ。
 * 後から項目を足しても古いものを読めるように、ない項目は初期値にする。
 */
object SearchQueryCodec {

    fun encode(query: SearchQuery): String = JSONObject().apply {
        put("sei", query.sei ?: JSONObject.NULL)
        put("positions", JSONArray(query.positions.map { position ->
            when (position) {
                is PositionCondition.Fixed -> JSONObject().put("kind", "fixed").put("char", position.char)
                is PositionCondition.Strokes ->
                    JSONObject().put("kind", "strokes").put("strokes", position.strokes).put("script", position.script.raw)
                is PositionCondition.Free -> JSONObject().put("kind", "free").put("script", position.script.raw)
            }
        }))
        put("conditions", JSONObject().apply {
            for ((kind, values) in query.conditions.entries.sortedBy { it.key.ordinal }) put(kind.raw, JSONArray(values.sorted()))
        })
        put("avoid", JSONArray(query.avoid))
        put("use", JSONArray(query.use))
        put("reading", query.reading ?: JSONObject.NULL)
        put("reading_prefix", query.readingPrefix)
    }.toString()

    fun decodeOrNull(text: String): SearchQuery? = try {
        decode(JSONObject(text))
    } catch (_: Exception) {
        null
    }

    private fun decode(json: JSONObject): SearchQuery {
        val positionsJson = json.getJSONArray("positions")
        val positions = (0 until positionsJson.length()).map { i ->
            val item = positionsJson.getJSONObject(i)
            val script = ScriptFilter.of(item.optString("script", "kanji")) ?: ScriptFilter.KANJI
            when (item.optString("kind")) {
                "fixed" -> PositionCondition.Fixed(item.getString("char"))
                "strokes" -> PositionCondition.Strokes(item.getInt("strokes"), script)
                else -> PositionCondition.Free(script)
            }
        }
        val conditionsJson = json.getJSONObject("conditions")
        val conditions = conditionsJson.keys().asSequence().mapNotNull { key ->
            val kind = KakuKind.of(key) ?: return@mapNotNull null
            val values = conditionsJson.getJSONArray(key)
            kind to (0 until values.length()).map { values.getInt(it) }.toSet()
        }.toMap()
        fun strings(name: String): List<String> =
            json.optJSONArray(name)?.let { array -> (0 until array.length()).map { array.getString(it) } } ?: emptyList()
        return SearchQuery(
            sei = if (json.isNull("sei")) null else json.optString("sei"),
            positions = positions,
            conditions = conditions,
            avoid = strings("avoid"),
            use = strings("use"),
            reading = if (json.isNull("reading")) null else json.optString("reading"),
            readingPrefix = json.optBoolean("reading_prefix", false),
        )
    }
}
