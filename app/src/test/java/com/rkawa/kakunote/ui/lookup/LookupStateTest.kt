package com.rkawa.kakunote.ui.lookup

import com.rkawa.kakunote.logic.Kaku
import com.rkawa.kakunote.logic.NameWarning
import com.rkawa.kakunote.logic.ShinjitaiSchool
import com.rkawa.kakunote.logic.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 「調べる」の画面に出すものと文言（iOS の LookupPresenter と同じ文言）。 */
class LookupStateTest {

    private val data = TestData.kanji

    private fun state(sei: String, mei: String) = LookupState.of(sei, mei, data, ShinjitaiSchool)

    @Test
    fun emptyWhenBothBlank() {
        assertEquals(LookupState.Empty, state("", " "))
    }

    @Test
    fun showsKakuForSeiAndMei() {
        val shown = state("林", "健太") as LookupState.Shown
        assertEquals(Kaku(tenkaku = 8, jinkaku = 19, chikaku = 15, soukaku = 23, gaikaku = 12), shown.result.kaku)
    }

    @Test
    fun unknownCharsMessage() {
        assertEquals(LookupState.Failed("画数がわからない字があります: 「A」、「B」"), state("田A", "健B"))
    }

    @Test
    fun invalidInputMessages() {
        assertEquals(LookupState.Failed("姓の途中に空白があります"), state("田 中", "健"))
        assertEquals(LookupState.Failed("名の濁点・半濁点の付け方が正しくありません"), state("田中", "゙健"))
        assertEquals(LookupState.Failed("名の濁点・半濁点の付け方が正しくありません"), state("田中", "健゙"))
    }

    @Test
    fun warningMessages() {
        assertEquals("「澤」（名の1文字目）は名前には使えません", LookupMessages.message(NameWarning.NotNameChar("澤", 1)))
        assertEquals("「ん」は名の1文字目には使えません", LookupMessages.message(NameWarning.NotInitial("ん")))
        val shown = state("", "んな") as LookupState.Shown
        assertTrue(shown.result.warnings.contains(NameWarning.NotInitial("ん")))
    }

    @Test
    fun gaikakuNoteFollowsSingleCharRule() {
        assertEquals("総格−人格＋姓と名の字", LookupMessages.gaikakuNote(1, 1))
        assertEquals("総格−人格＋姓の字", LookupMessages.gaikakuNote(1, 2))
        assertEquals("総格−人格＋名の字", LookupMessages.gaikakuNote(2, 1))
        assertEquals("総格−人格", LookupMessages.gaikakuNote(2, 2))
    }

    @Test
    fun altStrokesText() {
        assertEquals("別 10画・11画", LookupMessages.altStrokes(listOf(10, 11)))
    }
}
