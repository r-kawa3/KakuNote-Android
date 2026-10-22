package com.rkawa.kakunote.ui.search

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.InputError
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.TextPreprocessor
import com.rkawa.kakunote.logic.UnknownStrokeError
import com.rkawa.kakunote.logic.Reading
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchError
import com.rkawa.kakunote.logic.SearchQuery

/** 名前の形の 1 マスの条件の種類。 */
enum class PositionKind(val label: String, val id: String) {
    FREE("自由", "free"),
    STROKES("画数指定", "strokes"),
    FIXED("固定", "fixed"),
}

/** 文字の種類の名前（画面の表示）。 */
val ScriptFilter.label: String
    get() = when (this) {
        ScriptFilter.KANJI -> "漢字"
        ScriptFilter.HIRAGANA -> "ひらがな"
        ScriptFilter.KATAKANA -> "カタカナ"
        ScriptFilter.ANY -> "どれでも"
    }

/** 1 つの位置（マス）の入力欄の状態。 */
data class PositionInput(
    val kind: PositionKind = PositionKind.FREE,
    val script: ScriptFilter = ScriptFilter.KANJI,
    val strokesText: String = "",
    val fixedText: String = "",
) {
    /** 検索の条件。入力が足りなければ null */
    val condition: PositionCondition?
        get() = when (kind) {
            PositionKind.FREE -> PositionCondition.Free(script)
            PositionKind.STROKES -> strokesText.trim().toIntOrNull()?.takeIf { it > 0 }?.let { PositionCondition.Strokes(it, script) }
            PositionKind.FIXED -> fixedText.trim().takeIf { it.isNotEmpty() }?.let { PositionCondition.Fixed(it) }
        }

    /** マスに出す文字。固定はその字、自由は「自由」、画数指定は「n画」 */
    val cellText: String
        get() = when (kind) {
            PositionKind.FREE -> "自由"
            PositionKind.STROKES -> strokesText.trim().toIntOrNull()?.let { "${it}画" } ?: "画数"
            PositionKind.FIXED -> fixedText.trim().ifEmpty { "固定" }
        }

    val hasFixedChar: Boolean get() = kind == PositionKind.FIXED && fixedText.isNotBlank()
}

/** 「探す」の入力の全部。マスの条件は 6 文字分を持ち、文字数を減らしても残しておく。 */
data class SearchForm(
    val length: Int = 2,
    val positions: List<PositionInput> = List(NameSearch.MAX_LENGTH) { PositionInput() },
    /** 名前の形で選んでいるマス（0 から）。null なら選んでいない（条件の欄を出さない） */
    val selectedIndex: Int? = null,
    /** 一番上のカードの入力。姓なしなら総画数（地格）、姓ありなら総格 */
    val totalText: String = "",
    /** 姓ありのときの、ほかの格（人格・地格・外格）の入力 */
    val otherKakuTexts: Map<KakuKind, String> = emptyMap(),
) {
    /** 選んでいるマスの入力。選んでいなければ null */
    val selected: PositionInput? get() = selectedIndex?.let { positions[it] }

    /** マスを押したとき: 選んでいるマスなら閉じ、ほかのマスならそれを選ぶ */
    fun toggleSelection(index: Int): SearchForm = copy(selectedIndex = if (selectedIndex == index) null else index)

    fun withLength(value: Int): SearchForm {
        val length = value.coerceIn(1, NameSearch.MAX_LENGTH)
        // 選んでいたマスがなくなったら、選ぶのをやめる
        return copy(length = length, selectedIndex = selectedIndex?.takeIf { it < length })
    }

    fun updateSelected(change: (PositionInput) -> PositionInput): SearchForm =
        copy(positions = positions.mapIndexed { i, p -> if (i == selectedIndex) change(p) else p })

    /**
     * 検索の条件にする。足りない入力があれば、その文言（と、選び直すマス）を返す。
     * avoid・use・readingPrefix は設定から、reading は「探す」の読みの欄から渡す。
     */
    fun toQuery(
        sei: String? = null,
        reading: String = "",
        avoid: List<String> = emptyList(),
        use: List<String> = emptyList(),
        readingPrefix: Boolean = false,
    ): FormResult {
        val totals = parseNumbers(totalText)
        val conditions = mutableMapOf<KakuKind, Set<Int>>()
        if (totals.isNotEmpty()) conditions[if (sei != null) KakuKind.SOUKAKU else KakuKind.CHIKAKU] = totals.toSet()
        if (sei != null) {
            for (kind in OTHER_KAKU_KINDS) {
                val values = parseNumbers(otherKakuTexts[kind] ?: "")
                if (values.isNotEmpty()) conditions[kind] = values.toSet()
            }
        }
        if (conditions.isEmpty()) {
            return FormResult.Invalid(if (sei != null) "総格か、ほかの格の画数を入力してください" else "総画数を入力してください")
        }
        val positionConditions = (0 until length).map { index ->
            positions[index].condition ?: return FormResult.Invalid("${index + 1}文字目の条件を入力してください", index)
        }
        return FormResult.Ready(
            SearchQuery(
                sei = sei, positions = positionConditions, conditions = conditions, avoid = avoid, use = use,
                reading = reading.trim().ifEmpty { null }, readingPrefix = readingPrefix,
            ),
        )
    }

    /**
     * 「この条件で探す」を押せるかを、押す前に調べる。toQuery で入力の不足を見たうえで、
     * 探さなくてもわかる誤り（読みにひらがな以外がある、固定の字が 1 文字でない・画数がわからない、
     * 姓に画数がわからない字がある）も見る。候補が多すぎるかどうかは、探してみないとわからないので見ない。
     */
    fun check(
        data: KanjiData,
        sei: String? = null,
        reading: String = "",
        avoid: List<String> = emptyList(),
        use: List<String> = emptyList(),
        readingPrefix: Boolean = false,
    ): FormResult {
        val result = toQuery(sei, reading, avoid, use, readingPrefix)
        if (result !is FormResult.Ready) return result
        val query = result.query
        try {
            query.reading?.let { Reading.normalize(it) }
            query.sei?.let { data.strokes(TextPreprocessor.characters(it)) }
            query.positions.forEachIndexed { index, position ->
                if (position is PositionCondition.Fixed) {
                    val chars = TextPreprocessor.characters(position.char)
                    if (chars.size != 1) return FormResult.Invalid(SearchMessages.message(SearchError.FixedNotSingleChar(index + 1)), index)
                    try {
                        data.strokes(chars)
                    } catch (error: UnknownStrokeError) {
                        return FormResult.Invalid(SearchMessages.message(SearchError.UnknownStrokes(error.chars)), index)
                    }
                }
            }
        } catch (error: SearchError) {
            return FormResult.Invalid(SearchMessages.message(error))
        } catch (error: UnknownStrokeError) {
            return FormResult.Invalid(SearchMessages.message(SearchError.UnknownStrokes(error.chars)))
        } catch (error: InputError) {
            return FormResult.Invalid(SearchMessages.message(SearchError.InvalidInput(error)))
        }
        return result
    }

    companion object {
        /** 姓ありのとき、折りたたみの中で条件にできる格（天格は姓だけで決まるので表示だけ） */
        val OTHER_KAKU_KINDS = listOf(KakuKind.JINKAKU, KakuKind.CHIKAKU, KakuKind.GAIKAKU)

        /** カタカナをひらがなに直す（「ー」などはそのまま）。読みの欄に、カタカナで入れても自動で直す */
        fun hiragana(text: String): String =
            buildString { text.codePoints().forEach { appendCodePoint(Reading.hiragana(it)) } }

        /** 「24, 31」「24、31」「24 31」のような入力から、正の整数を取り出す。 */
        fun parseNumbers(text: String): List<Int> =
            text.split(',', '、', '，', ' ', '　')
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
    }
}

/** 入力を検索の条件にした結果。 */
sealed class FormResult {
    data class Ready(val query: SearchQuery) : FormResult()

    /** selectIndex は、条件が足りないマス（あれば） */
    data class Invalid(val message: String, val selectIndex: Int? = null) : FormResult()
}

/** 検索のエラーの文言。iOS の ResultPresenter と同じ。 */
object SearchMessages {
    fun message(error: SearchError): String = when (error) {
        is SearchError.UnknownStrokes -> "画数がわからない字があります: " + error.chars.joinToString("、") { "「$it」" }
        is SearchError.FixedNotSingleChar -> "${error.position}文字目に固定する字は 1 文字にしてください"
        is SearchError.InvalidInput -> "入力に使えない文字（途中の空白など）があります"
        SearchError.TooManyResults -> "候補が多すぎます。総画数を絞るか、文字を固定してください"
        SearchError.InvalidReading -> "読みは、ひらがな（またはカタカナ）30 文字までで入れてください"
        SearchError.InvalidLength, SearchError.NoCondition, is SearchError.InvalidCondition -> "検索の条件が正しくありません"
    }
}
