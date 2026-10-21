package com.rkawa.kakunote.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 共通テストケース（testcases/kaku.json と strokes.json の mei_warnings）で、五格と名の警告を確かめる。 */
class KakuTest {

    private val data = TestData.kanji
    private val school: KakuSchool = ShinjitaiSchool

    @Test
    fun commonKakuCases() {
        val cases = SharedTestCases.load("kaku.json").getJSONArray("cases").objects()
        assertFalse(cases.isEmpty())

        for (testCase in cases) {
            val id = testCase.optString("id", "?")
            val sei = data.strokes(TextPreprocessor.characters(testCase.getString("sei")))
            val mei = data.strokes(TextPreprocessor.characters(testCase.getString("mei")))
            assertEquals(id, testCase.getJSONArray("sei_strokes").ints(), sei)
            assertEquals(id, testCase.getJSONArray("mei_strokes").ints(), mei)

            val expected = testCase.getJSONObject("kaku")
            val kaku = school.kaku(sei, mei)
            for (kind in KakuKind.entries) {
                assertEquals("$id ${kind.raw}", expected.getInt(kind.raw), kaku.value(kind))
            }
        }
    }

    @Test
    fun commonNameOnlyCases() {
        val cases = SharedTestCases.load("kaku.json").getJSONArray("name_only").objects()
        assertFalse(cases.isEmpty())

        for (testCase in cases) {
            val id = testCase.optString("id", "?")
            val mei = data.strokes(TextPreprocessor.characters(testCase.getString("mei")))
            assertEquals(id, testCase.getJSONArray("mei_strokes").ints(), mei)
            assertEquals(id, testCase.getInt("chikaku"), school.nameOnlyTotal(mei))
        }
    }

    @Test
    fun commonMeiWarningCases() {
        val cases = SharedTestCases.load("strokes.json").getJSONArray("mei_warnings").objects()
        assertFalse(cases.isEmpty())

        for (testCase in cases) {
            val id = testCase.optString("id", "?")
            val mei = TextPreprocessor.characters(testCase.getString("mei"))
            val expected = testCase.getJSONArray("warnings").objects().map { item ->
                val char = item.optString("char")
                when (item.optString("type")) {
                    "not_initial" -> NameWarning.NotInitial(char)
                    else -> NameWarning.NotNameChar(char, item.optInt("position"))
                }
            }
            assertEquals(id, expected, data.meiWarnings(mei))
        }
    }

    @Test
    fun lookupShowsKakuForSeiAndMei() {
        val result = NameLookup.lookup("林", "健太", data, school)
        assertEquals(listOf(8), result.sei.map { it.strokes })
        assertEquals(listOf(11, 4), result.mei.map { it.strokes })
        assertEquals(Kaku(tenkaku = 8, jinkaku = 19, chikaku = 15, soukaku = 23, gaikaku = 12), result.kaku)
        assertNull(result.nameOnlyTotal)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun lookupNameOnlyShowsTotal() {
        val result = NameLookup.lookup("", "陽菜", data, school)
        assertNull(result.kaku)
        assertEquals(23, result.nameOnlyTotal)
    }

    @Test
    fun lookupDoesNotWarnForSei() {
        // 澤 は名前に使えない字だが、姓なら警告しない
        val result = NameLookup.lookup("澤", "澤", data, school)
        assertEquals(listOf(NameWarning.NotNameChar("澤", 1)), result.warnings)
    }

    @Test
    fun lookupReportsUnknownChars() {
        try {
            NameLookup.lookup("田A", "健B", data, school)
            fail("エラーにならない")
        } catch (error: LookupError) {
            assertEquals(LookupError.UnknownStrokes(listOf("A", "B")), error)
        }
    }

    @Test
    fun lookupReportsInvalidInput() {
        try {
            NameLookup.lookup("田 中", "健", data, school)
            fail("エラーにならない")
        } catch (error: LookupError) {
            assertEquals(LookupError.InvalidInput(NamePart.SEI, InputError.SpaceInside), error)
        }
    }
}
