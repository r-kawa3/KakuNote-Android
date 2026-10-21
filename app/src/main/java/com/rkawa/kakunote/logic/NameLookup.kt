package com.rkawa.kakunote.logic

/** 「調べる」の 1 文字分の結果。 */
data class LookupChar(
    val char: String,
    val strokes: Int,
    /** 数え間違えやすい別の画数 */
    val altStrokes: List<Int>,
)

/** 「調べる」の結果（仕様書 kaku.md「調べる」画面）。 */
data class LookupResult(
    val sei: List<LookupChar>,
    val mei: List<LookupChar>,
    val warnings: List<NameWarning>,
    /** 姓と名の両方があるときの五格 */
    val kaku: Kaku?,
    /** 名だけのときの総画数（＝ 地格） */
    val nameOnlyTotal: Int?,
)

enum class NamePart { SEI, MEI }

sealed class LookupError : Exception() {
    /** 入力文字列が前処理の規則に合わない */
    data class InvalidInput(val part: NamePart, val error: InputError) : LookupError()

    /** 画数がわからない字がある（姓・名の順） */
    data class UnknownStrokes(val chars: List<String>) : LookupError()
}

object NameLookup {

    /** 姓・名の入力文字列から「調べる」の結果を作る。空の欄は入力なしとして扱う。 */
    fun lookup(sei: String, mei: String, data: KanjiData, school: KakuSchool): LookupResult {
        val seiChars = characters(sei, NamePart.SEI)
        val meiChars = characters(mei, NamePart.MEI)

        val unknown = (seiChars + meiChars).filter { data.info(it) == null }
        if (unknown.isNotEmpty()) throw LookupError.UnknownStrokes(unknown)

        val seiRows = seiChars.map { row(it, data) }
        val meiRows = meiChars.map { row(it, data) }
        val seiStrokes = seiRows.map { it.strokes }
        val meiStrokes = meiRows.map { it.strokes }

        return LookupResult(
            sei = seiRows,
            mei = meiRows,
            warnings = data.meiWarnings(meiChars),
            kaku = if (seiStrokes.isEmpty() || meiStrokes.isEmpty()) null else school.kaku(seiStrokes, meiStrokes),
            nameOnlyTotal = if (seiStrokes.isEmpty() && meiStrokes.isNotEmpty()) school.nameOnlyTotal(meiStrokes) else null,
        )
    }

    private fun characters(text: String, part: NamePart): List<String> =
        try {
            TextPreprocessor.characters(text)
        } catch (_: InputError.Empty) {
            emptyList()
        } catch (error: InputError) {
            throw LookupError.InvalidInput(part, error)
        }

    private fun row(char: String, data: KanjiData): LookupChar {
        val info = data.info(char)!!
        return LookupChar(char, info.strokes, info.altStrokes)
    }
}
