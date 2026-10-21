package com.rkawa.kakunote.logic

/** 文字の種類。 */
enum class Script(val raw: String) {
    KANJI("kanji"),
    HIRAGANA("hiragana"),
    KATAKANA("katakana");

    companion object {
        fun of(raw: String): Script? = entries.firstOrNull { it.raw == raw }
    }
}

/** 1 文字の情報（仕様書 strokes.md「画数の決め方（新字体式）」）。 */
data class CharInfo(
    val char: String,
    /** 新字体式の画数（漢字は kanji.modern_strokes、それ以外は non_kanji.strokes） */
    val strokes: Int,
    val script: Script,
    /** 名前に使える字なら true（常用漢字・人名用漢字、または non_kanji の字） */
    val nameOK: Boolean,
    /** 名の 1 文字目に置けるなら true */
    val initialOK: Boolean,
    /** 「探す」で自動の候補に入れるなら true */
    val autoOK: Boolean,
    /** 数え間違えやすい別の画数（「調べる」で併記する） */
    val altStrokes: List<Int>,
) {
    /** 字のコードポイント */
    val codePoint: Int get() = char.codePointAt(0)
}

/** 画数がわからない字があるときのエラー。 */
class UnknownStrokeError(val chars: List<String>) : Exception()

/** DB を読めなかったときのエラー。 */
class KanjiDataError(message: String) : Exception(message)

/** 読み取り専用の SQL の行。列の番号は 0 から。 */
interface SqlRow {
    fun text(column: Int): String
    fun int(column: Int): Int
}

/**
 * 漢字 DB を読む口。アプリでは Android の SQLite、単体テストでは JDBC で読む
 * （JVM の単体テストでは Android の SQLite が使えないため）。
 */
interface SqlReader {
    fun forEachRow(sql: String, body: (SqlRow) -> Unit)
}

/**
 * 同梱の漢字 DB（kanji.sqlite）の中身。起動後に 1 回だけ読み込み、あとはメモリ上で引く。
 * 字はコードポイントで引く。
 */
class KanjiData(
    val chars: Map<Int, CharInfo>,
    val meta: Map<String, String>,
    /** 漢字の読み（音・訓・名乗りの kana と stem。ひらがな）。読みでの絞り込みに使う（仕様書 search.md「読み」） */
    val readings: Map<Int, Set<String>> = emptyMap(),
) {

    /** 1 文字（コードポイント 1 つ）の情報。2 つ以上のコードポイントからなる文字列なら null。 */
    fun info(char: String): CharInfo? {
        if (char.isEmpty() || char.codePointCount(0, char.length) != 1) return null
        return chars[char.codePointAt(0)]
    }

    /** 各字の画数。画数がわからない字があれば UnknownStrokeError を投げる。 */
    fun strokes(of: List<String>): List<Int> {
        val infos = of.map { info(it) }
        val unknown = of.filterIndexed { i, _ -> infos[i] == null }
        if (unknown.isNotEmpty()) throw UnknownStrokeError(unknown)
        return infos.map { it!!.strokes }
    }

    /**
     * 「探す」で自動の候補に入れる字（仕様書 search.md「位置ごとの候補の字」）。
     * 漢字 → ひらがな → カタカナ、同じ種類の中ではコードポイントの小さい順。
     */
    fun autoCandidates(script: ScriptFilter, initial: Boolean): List<CharInfo> =
        chars.values
            .filter { script.accepts(it.script) && it.autoOK && it.nameOK && (it.initialOK || !initial) }
            .sortedWith(compareBy<CharInfo> { it.script.ordinal }.thenBy { it.codePoint })

    /** 名の警告。画数がわからない字は、ここでは警告しない（別のエラーとして扱う）。 */
    fun meiWarnings(mei: List<String>): List<NameWarning> {
        val warnings = mutableListOf<NameWarning>()
        mei.forEachIndexed { index, char ->
            val info = info(char) ?: return@forEachIndexed
            if (!info.nameOK) {
                warnings.add(NameWarning.NotNameChar(char, index + 1))
            } else if (index == 0 && !info.initialOK) {
                warnings.add(NameWarning.NotInitial(char))
            }
        }
        return warnings
    }

    companion object {
        fun load(database: SqlReader): KanjiData {
            val alts = HashMap<Int, MutableList<Int>>()
            database.forEachRow("SELECT char, strokes FROM alt_stroke ORDER BY char, strokes") { row ->
                alts.getOrPut(row.scalar(0)) { mutableListOf() }.add(row.int(1))
            }

            val chars = HashMap<Int, CharInfo>()
            database.forEachRow(
                "SELECT char, modern_strokes, name_ok FROM kanji WHERE modern_strokes IS NOT NULL"
            ) { row ->
                val scalar = row.scalar(0)
                chars[scalar] = CharInfo(
                    char = codePointString(scalar), strokes = row.int(1), script = Script.KANJI,
                    nameOK = row.int(2) == 1, initialOK = true, autoOK = true, altStrokes = alts[scalar] ?: emptyList(),
                )
            }
            database.forEachRow("SELECT char, script, strokes, initial_ok, auto_ok FROM non_kanji") { row ->
                val scalar = row.scalar(0)
                val script = Script.of(row.text(1))
                    ?: throw KanjiDataError("non_kanji の script が不正: ${codePointString(scalar)}")
                chars[scalar] = CharInfo(
                    char = codePointString(scalar), strokes = row.int(2), script = script, nameOK = true,
                    initialOK = row.int(3) == 1, autoOK = row.int(4) == 1, altStrokes = emptyList(),
                )
            }

            val meta = HashMap<String, String>()
            database.forEachRow("SELECT key, value FROM meta") { row -> meta[row.text(0)] = row.text(1) }

            val readings = HashMap<Int, MutableSet<String>>()
            database.forEachRow("SELECT char, kana, stem FROM reading") { row ->
                readings.getOrPut(row.scalar(0)) { mutableSetOf() }.addAll(listOf(row.text(1), row.text(2)))
            }
            return KanjiData(chars, meta, readings)
        }

        private fun SqlRow.scalar(column: Int): Int {
            val text = text(column)
            if (text.isEmpty() || text.codePointCount(0, text.length) != 1) {
                throw KanjiDataError("1 文字ではない: $text")
            }
            return text.codePointAt(0)
        }
    }
}
