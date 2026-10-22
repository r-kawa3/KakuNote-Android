package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 「探す」画面の条件の入力。 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule(order = 0)
    val reset = ResetAppData()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForSearchTab() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("名前を探す") and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun cellCount() = (1..6).count { compose.onAllNodes(hasTestTag("search.cell.$it")).fetchSemanticsNodes().isNotEmpty() }

    /** 「この条件で探す」を押して、候補の一覧の件数（例: 「4件」）を返す。 */
    private fun submitAndWaitForCount(): String {
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("result.total")).fetchSemanticsNodes().isNotEmpty()
        }
        return compose.onNodeWithTag("result.total").fetchSemanticsNode().config
            .first { it.key.name == "Text" }.value.toString()
    }

    @Test
    fun lengthStepperChangesCells() {
        assertEquals(2, cellCount())
        compose.onNodeWithTag("search.length.plus").performClick()
        assertEquals(3, cellCount())
        repeat(5) { compose.onNodeWithTag("search.length.minus").performClick() }
        assertEquals(1, cellCount())
        compose.onNodeWithTag("search.length.minus").assertIsNotEnabled()
        repeat(7) { compose.onNodeWithTag("search.length.plus").performClick() }
        assertEquals(6, cellCount())
        compose.onNodeWithTag("search.length.plus").assertIsNotEnabled()
    }

    @Test
    fun requiresTotal() {
        compose.onNodeWithTag("search.submit").performClick()
        compose.onNodeWithTag("search.formError").assertTextEquals("総画数を入力してください")
    }

    @Test
    fun twoKanjiTotalTwoHasFourCandidates() {
        compose.onNodeWithTag("search.total").performTextInput("2")
        assertTrue(submitAndWaitForCount().contains("[4件]"))
    }

    @Test
    fun fourCharsTotalEight() {
        compose.onNodeWithTag("search.total").performTextInput("8")
        repeat(2) { compose.onNodeWithTag("search.length.plus").performClick() }
        assertTrue(submitAndWaitForCount().contains("305,520件"))
    }

    @Test
    fun fixedCharMissingSelectsThatCell() {
        compose.onNodeWithTag("search.total").performTextInput("15")
        compose.onNodeWithTag("search.cell.2").performClick()
        compose.onNodeWithTag("search.kind.fixed").performClick()
        compose.onNodeWithTag("search.cell.1").performClick()
        compose.onNodeWithTag("search.submit").performClick()
        compose.onNodeWithTag("search.formError").assertTextEquals("2文字目の条件を入力してください")
        compose.onNodeWithTag("search.cell.2").assertIsSelected()
    }

    @Test
    fun fixedFirstAndHiraganaSecond() {
        compose.onNodeWithTag("search.total").performTextInput("15")
        compose.onNodeWithTag("search.kind.fixed").performClick()
        compose.onNodeWithTag("search.fixed").performTextInput("陽")
        compose.onNodeWithTag("search.cell.1").assertTextContains("陽", substring = true)
        compose.onNodeWithTag("search.cell.2").performClick()
        compose.onNodeWithTag("search.script").performClick()
        compose.waitUntil { compose.onAllNodes(hasTestTag("search.script.hiragana")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search.script.hiragana").performClick()
        assertTrue(submitAndWaitForCount().contains("[31件]"))
    }

    @Test
    fun strokesConditionAndSeparators() {
        // 総画数 2・3 の 2 文字で、2 文字目は 1 画の漢字（一・乙）
        compose.onNodeWithTag("search.total").performTextInput("2, 3")
        compose.onNodeWithTag("search.cell.2").performClick()
        compose.onNodeWithTag("search.kind.strokes").performClick()
        compose.onNodeWithTag("search.strokes").performTextInput("1")
        compose.onNodeWithTag("search.cell.2").assertTextContains("1画", substring = true)
        // 1 文字目が 1 画（2 字）か 2 画の漢字で、2 文字目が 2 字
        assertTrue(submitAndWaitForCount().endsWith("件]"))
    }

    @Test
    fun tooManyResultsShowsError() {
        compose.onNodeWithTag("search.total").performTextInput("30")
        repeat(4) { compose.onNodeWithTag("search.length.plus").performClick() }
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("result.total") or hasTestTag("result.error")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertEquals(expected: Int, actual: Int) = org.junit.Assert.assertEquals(expected, actual)
    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
