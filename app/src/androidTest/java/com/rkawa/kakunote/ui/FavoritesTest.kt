package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import com.rkawa.kakunote.data.FavoriteStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** お気に入り: 一覧のハートで保存・削除し、「お気に入り」タブで見比べる。 */
@RunWith(AndroidJUnit4::class)
class FavoritesTest {

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

    /** 2 文字・総画数 2（一一、一乙、乙一、乙乙）の一覧を出す */
    private fun searchTwoStrokes() {
        compose.onNodeWithTag("search.total").performTextInput("2")
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasTestTag("result.total")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openFavoritesTab() {
        compose.onAllNodes(hasText("お気に入り") and !isHeading()).onFirst().performClick()
        waitForHeading("お気に入り")
    }

    @Test
    fun emptyShowsGoSearch() {
        openFavoritesTab()
        compose.onNodeWithText("お気に入りはまだありません").assertExists()
        compose.onNodeWithTag("favorites.goSearch").performClick()
        waitForHeading("名前を探す")
    }

    @Test
    fun saveFromListAndCompare() {
        searchTwoStrokes()
        compose.onNodeWithTag("favorite.toggle.一乙").performClick()
        compose.onNodeWithTag("favorite.toggle.乙乙").performClick()
        // 押すとすぐ「保存済み」になる
        compose.onNodeWithTag("favorite.toggle.一乙").assertContentDescriptionEquals("お気に入りから外す")
        compose.onNodeWithTag("favorite.toggle.一一").assertContentDescriptionEquals("お気に入りに追加")
        openFavoritesTab()
        compose.onNodeWithTag("favorites.total").assertTextEquals("2件")
        // 新しく保存したものが先。五格（姓なしなので総画数）と条件の要約が出る
        compose.onAllNodesWithTag("favorite.card").assertCountEquals(2)
        val first = compose.onAllNodesWithTag("favorite.card").fetchSemanticsNodes()[0]
            .config.first { it.key.name == "ContentDescription" }.value.toString()
        assertTrue(first, first.contains("乙乙、総画数 2画、2文字・総画数 2"))
        // 押すと詳細
        compose.onAllNodesWithTag("favorite.card").onFirst().performClick()
        waitForHeading("乙乙")
        compose.onNodeWithTag("lookup.kaku.total").assertTextEquals("2画")
    }

    @Test
    fun toggleTwiceRemoves() {
        searchTwoStrokes()
        compose.onNodeWithTag("favorite.toggle.一一").performClick()
        compose.onNodeWithTag("favorite.toggle.一一").performClick()
        openFavoritesTab()
        compose.onNodeWithTag("favorites.empty").assertExists()
    }

    @Test
    fun swipeDeletes() {
        searchTwoStrokes()
        compose.onNodeWithTag("favorite.toggle.一一").performClick()
        openFavoritesTab()
        compose.onAllNodesWithTag("favorite.card").onFirst().performTouchInput { swipeLeft() }
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasTestTag("favorites.empty")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun keepsAfterRestart() {
        searchTwoStrokes()
        compose.onNodeWithTag("favorite.toggle.乙一").performClick()
        // 画面を作り直しても、一覧もお気に入りも残っている
        compose.activityRule.scenario.recreate()
        waitForHeading("候補")
        compose.onNodeWithTag("favorite.toggle.乙一").assertContentDescriptionEquals("お気に入りから外す")
        openFavoritesTab()
        compose.onNodeWithTag("favorites.total").assertTextEquals("1件")
        compose.activityRule.scenario.close()
        // 開き直したアプリでも残っている
        val store = FavoriteStore(
            com.rkawa.kakunote.data.AndroidWritableSql(
                InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(FavoriteStore.FILE_NAME),
            ),
        )
        assertTrue(store.all().map { it.mei }.toString(), store.all().map { it.mei } == listOf("乙一"))
    }

    private fun assertTrue(message: String, value: Boolean) = org.junit.Assert.assertTrue(message, value)
}
