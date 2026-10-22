package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertIsEnabled
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithText
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
    fun seiPromptOpensSettings() {
        compose.onNodeWithText("姓を登録すると、総格で探せます").assertExists()
        compose.onNodeWithTag("search.setSei").performClick()
        compose.waitUntil { compose.onAllNodes(hasText("設定") and isHeading()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("settings.sei").performTextInput("田中")
        compose.onNodeWithTag("settings.close").performClick()
        compose.waitUntil { compose.onAllNodes(hasTestTag("search.sei")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search.setSei").assertDoesNotExist()
        compose.onNodeWithText("総格").assertExists()
    }

    @Test
    fun conditionsShowOnlyWhenCellSelected() {
        // はじめはどのマスも選ばれておらず、条件の欄は出ない
        compose.onNodeWithTag("search.kind.free").assertDoesNotExist()
        compose.onNodeWithText("マスを押すと、その文字の条件を選べます").assertExists()
        // マスを押すと出る。ほかのマスを押すと、そのマスの条件に替わる
        compose.onNodeWithTag("search.cell.2").performScrollTo().performClick()
        compose.onNodeWithTag("search.cell.2").assertIsSelected()
        compose.onNodeWithTag("search.kind.free").assertExists()
        compose.onNodeWithTag("search.cell.1").performScrollTo().performClick()
        compose.onNodeWithTag("search.cell.1").assertIsSelected()
        // 選んでいるマスをもう一度押すと閉じる
        compose.onNodeWithTag("search.cell.1").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.free").assertDoesNotExist()
    }

    @Test
    fun cannotSubmitWithoutTotal() {
        // 総画数がないうちは押せず、理由を出す。入れると押せる
        compose.onNodeWithTag("search.submit").assertIsNotEnabled()
        compose.onNodeWithTag("search.formError").assertTextEquals("総画数を入力してください")
        compose.onNodeWithTag("search.total").performTextInput("2")
        compose.onNodeWithTag("search.submit").assertIsEnabled()
        compose.onNodeWithTag("search.formError").assertDoesNotExist()
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
    fun cannotSubmitWithMissingOrWrongFixedChar() {
        compose.onNodeWithTag("search.total").performTextInput("15")
        compose.onNodeWithTag("search.cell.2").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.fixed").performScrollTo().performClick()
        compose.onNodeWithTag("search.submit").assertIsNotEnabled()
        compose.onNodeWithTag("search.formError").assertTextEquals("2文字目の条件を入力してください")
        // 2 文字入れても、画数がわからない字でも押せない
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextInput("陽菜")
        compose.onNodeWithTag("search.formError").assertTextEquals("2文字目に固定する字は 1 文字にしてください")
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextReplacement("A")
        compose.onNodeWithTag("search.formError").assertTextEquals("画数がわからない字があります: 「A」")
        compose.onNodeWithTag("search.submit").assertIsNotEnabled()
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextReplacement("陽")
        compose.onNodeWithTag("search.submit").assertIsEnabled()
    }

    @Test
    fun fixedFirstAndHiraganaSecond() {
        compose.onNodeWithTag("search.total").performTextInput("15")
        compose.onNodeWithTag("search.cell.1").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.fixed").performScrollTo().performClick()
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextInput("陽")
        compose.onNodeWithTag("search.cell.1").assertTextContains("陽", substring = true)
        compose.onNodeWithTag("search.cell.2").performScrollTo().performClick()
        compose.onNodeWithTag("search.script").performScrollTo().performClick()
        compose.waitUntil { compose.onAllNodes(hasTestTag("search.script.hiragana")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search.script.hiragana").performClick()
        assertTrue(submitAndWaitForCount().contains("[31件]"))
    }

    @Test
    fun strokesConditionAndDigitsOnly() {
        // 総画数 3 の 2 文字で、2 文字目は 1 画の漢字（一・乙）。総画数は数字だけ（区切りの「,」や空白は入らない）
        compose.onNodeWithTag("search.total").performTextInput("3, 4")
        compose.onNodeWithTag("search.total").assertTextEquals("34")
        compose.onNodeWithTag("search.total").performTextReplacement("3")
        compose.onNodeWithTag("search.cell.2").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.strokes").performScrollTo().performClick()
        compose.onNodeWithTag("search.strokes").performScrollTo().performTextInput("1")
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
