package com.rkawa.kakunote.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test

/** 共通テストケース（testcases/strokes.json）で、入力文字の前処理と 1 文字の画数を確かめる。 */
class StrokesTest {

    private val data = TestData.kanji

    @Test
    fun commonStrokeCases() {
        val cases = SharedTestCases.load("strokes.json").getJSONArray("cases").objects()
        assertFalse(cases.isEmpty())

        for (testCase in cases) {
            val id = testCase.optString("id", "?")
            val input = testCase.getString("input")

            when (testCase.optString("error")) {
                "invalid_input" -> assertThrows(id, InputError::class.java) { TextPreprocessor.characters(input) }

                "unknown_stroke" -> {
                    val chars = TextPreprocessor.characters(input)
                    try {
                        data.strokes(chars)
                        fail("$id: エラーにならない")
                    } catch (error: UnknownStrokeError) {
                        assertEquals(id, testCase.getJSONArray("unknown_chars").strings(), error.chars)
                    }
                }

                else -> {
                    val chars = TextPreprocessor.characters(input)
                    assertEquals(id, testCase.getJSONArray("chars").strings().map(::codePoints), chars.map(::codePoints))
                    assertEquals(id, testCase.getJSONArray("strokes").ints(), data.strokes(chars))
                    testCase.optJSONArray("alt_strokes")?.let { expected ->
                        assertEquals(
                            id,
                            (0 until expected.length()).map { expected.getJSONArray(it).ints() },
                            chars.map { data.info(it)?.altStrokes ?: emptyList() },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun canonicallyEquivalentCompatIdeographsStayDistinct() {
        // 海 U+6D77 と互換漢字の 海 U+FA45 は、正準等価だが画数は別
        assertEquals(9, data.info("海")?.strokes)
        assertEquals(10, data.info("海")?.strokes)
        assertEquals(listOf(listOf(0xFA45)), TextPreprocessor.characters("海").map(::codePoints))
        // 漢字に濁点を付けても、NFC で別の字に置き換わらずにエラーになる
        assertThrows(InputError::class.java) { TextPreprocessor.characters("海゙") }
    }

    @Test
    fun surrogatePairIsOneChar() {
        // 𠮷（U+20BB7）は UTF-16 では 2 つだが、1 文字として扱う
        assertEquals(listOf("𠮷", "野"), TextPreprocessor.characters("𠮷野"))
    }

    @Test
    fun bundledDataMatchesTestCaseVersion() {
        val version = SharedTestCases.load("search.json").getJSONObject("data_version")
        assertEquals(version.getString("kanjidic2_database_version"), data.meta["kanjidic2_database_version"])
    }
}
