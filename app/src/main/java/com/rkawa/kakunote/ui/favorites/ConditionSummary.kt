package com.rkawa.kakunote.ui.favorites

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.SearchQuery

/** 探したときの条件を、お気に入りの一覧に出す短い文にする（iOS の ConditionSummary と同じ）。 */
object ConditionSummary {

    fun text(query: SearchQuery): String {
        val parts = mutableListOf("${query.positions.size}文字")
        query.reading?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add("読み「$it」") }
        query.positions.forEachIndexed { index, position ->
            if (position is PositionCondition.Fixed) parts.add("${index + 1}文字目「${position.char}」")
        }
        val kinds = if (query.sei == null) {
            listOf(KakuKind.CHIKAKU to "総画数")
        } else {
            listOf(KakuKind.JINKAKU to "人格", KakuKind.CHIKAKU to "地格", KakuKind.SOUKAKU to "総格", KakuKind.GAIKAKU to "外格")
        }
        for ((kind, title) in kinds) {
            val values = query.conditions[kind]
            if (!values.isNullOrEmpty()) parts.add("$title " + values.sorted().joinToString(", "))
        }
        return parts.joinToString("・")
    }
}
