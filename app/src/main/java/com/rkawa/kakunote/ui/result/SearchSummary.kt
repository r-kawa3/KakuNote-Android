package com.rkawa.kakunote.ui.result

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.ui.search.label

/** 候補の一覧の上に並べる、検索の条件のチップ。文言は iOS の SearchSummary と同じ。 */
object SearchSummary {

    data class Chip(
        val text: String,
        /** 格の条件（藍で塗る）なら true、それ以外（枠だけ）は false */
        val isPrimary: Boolean,
    )

    fun chips(query: SearchQuery): List<Chip> {
        val chips = mutableListOf<Chip>()
        val kinds = if (query.sei == null) {
            listOf(KakuKind.CHIKAKU to "総画数")
        } else {
            listOf(KakuKind.JINKAKU to "人格", KakuKind.CHIKAKU to "地格", KakuKind.SOUKAKU to "総格", KakuKind.GAIKAKU to "外格")
        }
        for ((kind, title) in kinds) {
            val values = query.conditions[kind]
            if (!values.isNullOrEmpty()) chips.add(Chip("$title " + values.sorted().joinToString("・") + "画", true))
        }
        query.sei?.let { chips.add(Chip("姓「$it」", false)) }
        val reading = query.reading?.trim()
        if (!reading.isNullOrEmpty()) {
            chips.add(Chip("読み「$reading」", false))
            if (query.readingPrefix) chips.add(Chip("読みの頭の部分も使う", false))
        }
        chips.add(Chip("${query.positions.size}文字", false))
        query.positions.forEachIndexed { index, position ->
            val label = "${index + 1}文字目"
            when (position) {
                is PositionCondition.Fixed -> chips.add(Chip("$label「${position.char}」固定", false))
                is PositionCondition.Strokes -> {
                    val script = if (position.script == ScriptFilter.KANJI) "" else "・" + position.script.label
                    chips.add(Chip("$label ${position.strokes}画$script", false))
                }
                // 自由・漢字は初期値なので、チップにしない
                is PositionCondition.Free ->
                    if (position.script != ScriptFilter.KANJI) chips.add(Chip("$label ${position.script.label}", false))
            }
        }
        if (query.use.isNotEmpty()) chips.add(Chip("使いたい字: " + query.use.joinToString("・"), false))
        if (query.avoid.isNotEmpty()) chips.add(Chip("避けたい字 ${query.avoid.size}字を除外", false))
        return chips
    }
}

/** 候補の一覧の 1 行。 */
data class ResultRow(
    val name: String,
    /** 名の各字の画数の内訳（例: 「12 + 8 = 20画」、1 文字なら「12画」） */
    val breakdown: String,
    /** 姓ありの検索で、条件にした格の値（例: 「人格 15・総格 24」）。姓なしなら null */
    val kakuNote: String?,
) {
    companion object {
        fun make(
            candidate: NameSearch.Candidate,
            seiStrokes: List<Int>?,
            conditions: Map<KakuKind, Set<Int>>,
            school: KakuSchool,
        ): ResultRow {
            val total = candidate.strokes.sum()
            val breakdown = if (candidate.strokes.size == 1) {
                "${total}画"
            } else {
                candidate.strokes.joinToString(" + ") + " = ${total}画"
            }
            val kakuNote = seiStrokes?.let { sei ->
                val kaku = school.kaku(sei, candidate.strokes)
                listOf(KakuKind.JINKAKU to "人格", KakuKind.CHIKAKU to "地格", KakuKind.SOUKAKU to "総格", KakuKind.GAIKAKU to "外格")
                    .filter { conditions[it.first] != null }
                    .joinToString("・") { "${it.second} ${kaku.value(it.first)}" }
            }
            return ResultRow(candidate.name, breakdown, kakuNote)
        }
    }
}
