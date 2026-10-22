package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 候補の一覧と、候補の詳細。 */
@RunWith(AndroidJUnit4::class)
class ResultScreenTest {

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

    private fun search(total: String, length: Int = 2) {
        compose.onNodeWithTag("search.total").performTextInput(total)
        repeat(length - 2) { compose.onNodeWithTag("search.length.plus").performClick() }
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("result.total") or hasTestTag("result.empty")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForHeading(title: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(title) and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun listShowsChipsTotalAndRowsInOrder() {
        search("2")
        compose.onNodeWithText("総画数 2画").assertExists()
        compose.onNodeWithText("2文字").assertExists()
        compose.onNodeWithTag("result.total").assertTextEquals("4件")
        val names = compose.onAllNodesWithTag("result.row").fetchSemanticsNodes().map {
            it.config.first { entry -> entry.key.name == "ContentDescription" }.value.toString()
        }
        assertTrue(names.toString(), names[0].contains("一一、1 + 1 = 2画"))
        assertTrue(names.toString(), names[3].contains("乙乙"))
    }

    @Test
    fun scrollingLoadsMoreRows() {
        // 4 文字・総画数 8 は 305,520 件。最初は 50 件だけ作り、下までスクロールすると足す
        search("8", length = 4)
        compose.onNodeWithTag("result.total").assertTextEquals("305,520件")
        assertTrue(compose.onAllNodesWithTag("result.row").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("result.list").performScrollToIndex(51)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasTestTag("result.row") and hasContentDescription("、", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("result.list").performScrollToIndex(101)
    }

    @Test
    fun emptyResult() {
        compose.onNodeWithTag("search.total").performTextInput("1")
        compose.onNodeWithTag("search.kind.fixed").performClick()
        compose.onNodeWithTag("search.fixed").performTextInput("陽")
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("result.empty")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("該当する名前がありません").assertExists()
    }

    @Test
    fun errorResult() {
        compose.onNodeWithTag("search.total").performTextInput("2")
        compose.onNodeWithTag("search.kind.fixed").performClick()
        compose.onNodeWithTag("search.fixed").performTextInput("陽菜")
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("result.error")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("1文字目に固定する字は 1 文字にしてください").assertExists()
    }

    @Test
    fun detailAndBackKeepState() {
        search("2")
        compose.onAllNodesWithTag("result.row").onFirst().performClick()
        waitForHeading("一一")
        compose.onNodeWithTag("lookup.kaku.total").assertTextEquals("2画")
        // 端末の戻る操作で一覧に、上の「‹ 戻る」で条件の入力に戻る。入力は残っている
        Espresso.pressBack()
        waitForHeading("候補")
        compose.onNodeWithTag("result.total").assertTextEquals("4件")
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("名前を探す")
        compose.onNodeWithTag("search.total").assertTextEquals("2")
    }

    @Test
    fun resultStaysAfterSwitchingTabs() {
        search("2")
        compose.onNodeWithText("調べる").performClick()
        waitForHeading("画数を調べる")
        compose.onNodeWithText("探す").performClick()
        waitForHeading("候補")
        // 選んでいる「探す」タブをもう一度押すと、条件の入力に戻る
        compose.onNodeWithText("探す").performClick()
        waitForHeading("名前を探す")
    }
}
