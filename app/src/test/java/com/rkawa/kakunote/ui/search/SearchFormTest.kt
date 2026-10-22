package com.rkawa.kakunote.ui.search

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchError
import com.rkawa.kakunote.logic.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Test

/** 「探す」の入力から検索の条件を作る。 */
class SearchFormTest {

    @Test
    fun parsesSeparators() {
        assertEquals(listOf(24, 31, 32, 5), SearchForm.parseNumbers("24, 31、32，5"))
        assertEquals(listOf(24, 31), SearchForm.parseNumbers(" 24 31 0 -3 あ"))
        assertEquals(emptyList<Int>(), SearchForm.parseNumbers(""))
    }

    @Test
    fun nameOnlyQueryUsesChikaku() {
        val form = SearchForm(totalText = "20, 24")
        assertEquals(
            FormResult.Ready(
                SearchQuery(
                    sei = null,
                    positions = List(2) { PositionCondition.Free(ScriptFilter.KANJI) },
                    conditions = mapOf(KakuKind.CHIKAKU to setOf(20, 24)),
                ),
            ),
            form.toQuery(),
        )
    }

    @Test
    fun seiQueryUsesSoukaku() {
        val result = SearchForm(totalText = "24").toQuery(sei = "田中") as FormResult.Ready
        assertEquals(mapOf(KakuKind.SOUKAKU to setOf(24)), result.query.conditions)
        assertEquals("田中", result.query.sei)
    }

    @Test
    fun missingTotal() {
        assertEquals(FormResult.Invalid("総画数を入力してください"), SearchForm().toQuery())
        assertEquals(FormResult.Invalid("総格か、ほかの格の画数を入力してください"), SearchForm().toQuery(sei = "田中"))
    }

    @Test
    fun missingPositionSelectsThatCell() {
        val form = SearchForm(totalText = "20", length = 3, selectedIndex = 1)
            .updateSelected { it.copy(kind = PositionKind.FIXED) }
        assertEquals(FormResult.Invalid("2文字目の条件を入力してください", 1), form.toQuery())
    }

    @Test
    fun positionConditionsAndCellText() {
        val strokes = PositionInput(kind = PositionKind.STROKES, script = ScriptFilter.ANY, strokesText = " 12 ")
        assertEquals(PositionCondition.Strokes(12, ScriptFilter.ANY), strokes.condition)
        assertEquals("12画", strokes.cellText)
        assertEquals("画数", PositionInput(kind = PositionKind.STROKES).cellText)
        val fixed = PositionInput(kind = PositionKind.FIXED, fixedText = "陽")
        assertEquals(PositionCondition.Fixed("陽"), fixed.condition)
        assertEquals("陽", fixed.cellText)
        assertEquals("固定", PositionInput(kind = PositionKind.FIXED).cellText)
        assertEquals("自由", PositionInput().cellText)
    }

    @Test
    fun lengthKeepsSelectionInside() {
        val form = SearchForm(length = 4, selectedIndex = 3).withLength(2)
        assertEquals(2, form.length)
        assertEquals(1, form.selectedIndex)
        assertEquals(6, form.withLength(9).length)
        assertEquals(1, form.withLength(0).length)
    }

    @Test
    fun errorMessages() {
        assertEquals("候補が多すぎます。総画数を絞るか、文字を固定してください", SearchMessages.message(SearchError.TooManyResults))
        assertEquals("画数がわからない字があります: 「A」", SearchMessages.message(SearchError.UnknownStrokes(listOf("A"))))
        assertEquals("2文字目に固定する字は 1 文字にしてください", SearchMessages.message(SearchError.FixedNotSingleChar(2)))
    }
}
