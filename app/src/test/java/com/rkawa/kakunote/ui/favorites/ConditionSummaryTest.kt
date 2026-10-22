package com.rkawa.kakunote.ui.favorites

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionSummaryTest {

    @Test
    fun nameOnly() {
        val query = SearchQuery(
            null, listOf(PositionCondition.Fixed("陽"), PositionCondition.Free(ScriptFilter.KANJI)),
            mapOf(KakuKind.CHIKAKU to setOf(24, 20)), reading = "はるな",
        )
        assertEquals("2文字・読み「はるな」・1文字目「陽」・総画数 20, 24", ConditionSummary.text(query))
    }

    @Test
    fun withSei() {
        val query = SearchQuery(
            "田中", listOf(PositionCondition.Free(ScriptFilter.KANJI)),
            mapOf(KakuKind.SOUKAKU to setOf(24), KakuKind.JINKAKU to setOf(15)),
        )
        assertEquals("1文字・人格 15・総格 24", ConditionSummary.text(query))
    }
}
