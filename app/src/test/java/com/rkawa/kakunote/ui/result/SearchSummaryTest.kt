package com.rkawa.kakunote.ui.result

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.logic.ShinjitaiSchool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 一覧のチップと行の文言（iOS の SearchSummary・ResultRow と同じ）。 */
class SearchSummaryTest {

    @Test
    fun nameOnlyChips() {
        val query = SearchQuery(
            sei = null,
            positions = listOf(
                PositionCondition.Fixed("陽"),
                PositionCondition.Free(ScriptFilter.HIRAGANA),
                PositionCondition.Strokes(5, ScriptFilter.KANJI),
                PositionCondition.Free(ScriptFilter.KANJI),
            ),
            conditions = mapOf(KakuKind.CHIKAKU to setOf(31, 20)),
        )
        assertEquals(
            listOf(
                SearchSummary.Chip("総画数 20・31画", true),
                SearchSummary.Chip("4文字", false),
                SearchSummary.Chip("1文字目「陽」固定", false),
                SearchSummary.Chip("2文字目 ひらがな", false),
                SearchSummary.Chip("3文字目 5画", false),
            ),
            SearchSummary.chips(query),
        )
    }

    @Test
    fun seiChipsWithReadingUseAndAvoid() {
        val query = SearchQuery(
            sei = "田中",
            positions = listOf(PositionCondition.Strokes(3, ScriptFilter.ANY)),
            conditions = mapOf(KakuKind.SOUKAKU to setOf(24), KakuKind.JINKAKU to setOf(15)),
            avoid = listOf("乙", "一"),
            use = listOf("陽", "葵"),
            reading = " はると ",
            readingPrefix = true,
        )
        assertEquals(
            listOf(
                "人格 15画", "総格 24画", "姓「田中」", "読み「はると」", "読みの頭の部分も使う", "1文字",
                "1文字目 3画・どれでも", "使いたい字: 陽・葵", "避けたい字 2字を除外",
            ),
            SearchSummary.chips(query).map { it.text },
        )
    }

    @Test
    fun rowBreakdown() {
        val conditions = mapOf(KakuKind.CHIKAKU to setOf(20))
        val two = ResultRow.make(NameSearch.Candidate("陽一", listOf(12, 1)), null, conditions, ShinjitaiSchool)
        assertEquals("12 + 1 = 13画", two.breakdown)
        assertNull(two.kakuNote)
        assertEquals("12画", ResultRow.make(NameSearch.Candidate("翔", listOf(12)), null, conditions, ShinjitaiSchool).breakdown)
    }

    @Test
    fun rowKakuNoteForConditionedKinds() {
        val row = ResultRow.make(
            NameSearch.Candidate("健太", listOf(11, 4)),
            seiStrokes = listOf(5, 4),
            conditions = mapOf(KakuKind.SOUKAKU to setOf(24), KakuKind.JINKAKU to setOf(15)),
            school = ShinjitaiSchool,
        )
        assertEquals("人格 15・総格 24", row.kakuNote)
    }
}
