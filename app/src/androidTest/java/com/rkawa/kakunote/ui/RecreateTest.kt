package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 画面が作り直されても（回転・ダークモードの切り替え・文字の大きさの変更など）、進んだ先の画面が閉じない。 */
@RunWith(AndroidJUnit4::class)
class RecreateTest {

    @get:Rule(order = 0)
    val reset = ResetAppData()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForSearchTab() = waitForHeading("名前を探す")

    private fun waitForHeading(title: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText(title) and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun search(total: String, length: Int = 2) {
        compose.onNodeWithTag("search.total").performTextInput(total)
        repeat(length - 2) { compose.onNodeWithTag("search.length.plus").performClick() }
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasTestTag("result.total")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun recreate() {
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    private fun firstRowName(): String =
        compose.onAllNodesWithTag("result.row").fetchSemanticsNodes().first()
            .config.first { it.key.name == "ContentDescription" }.value.toString()

    @Test
    fun resultListStaysWithScrollPosition() {
        search("8", length = 4)
        // 最初は 50 件だけ。下までスクロールすると続きを作るので、2 回に分けて進む
        compose.onNodeWithTag("result.list").performScrollToIndex(51)
        compose.waitUntil { compose.onAllNodesWithTag("result.row").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("result.list").performScrollToIndex(80)
        compose.waitForIdle()
        val before = firstRowName()
        recreate()
        waitForHeading("候補")
        // スクロールした位置のまま（上の件数は画面の外）
        assertTrue("${firstRowName()} / $before", firstRowName() == before)
    }

    @Test
    fun detailStaysAndBackGoesToList() {
        search("2")
        compose.onAllNodesWithTag("result.row").onFirst().performClick()
        waitForHeading("一一")
        recreate()
        waitForHeading("一一")
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("候補")
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("名前を探す")
        compose.onNodeWithTag("search.total").assertTextEquals("2")
    }

    @Test
    fun favoriteDetailStays() {
        search("2")
        compose.onNodeWithTag("favorite.toggle.乙乙").performClick()
        compose.onNodeWithText("お気に入り").performClick()
        waitForHeading("お気に入り")
        compose.onAllNodesWithTag("favorite.card").onFirst().performClick()
        waitForHeading("乙乙")
        recreate()
        waitForHeading("乙乙")
        compose.onNodeWithTag("lookup.kaku.total").assertTextEquals("2画")
    }

    @Test
    fun settingsAndAboutStay() {
        compose.onNodeWithTag("search.total").performTextInput("5")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("設定")).onFirst().performClick()
        waitForHeading("設定")
        compose.onNodeWithTag("settings.about").performScrollTo().performClick()
        waitForHeading("このアプリについて")
        recreate()
        waitForHeading("このアプリについて")
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("設定")
        compose.onNodeWithTag("settings.close").performClick()
        waitForHeading("名前を探す")
        compose.onNodeWithTag("search.total").assertTextEquals("5")
    }
}
