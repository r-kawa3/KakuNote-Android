package com.rkawa.kakunote

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 起動すると漢字のデータを読み込み、下のタブと見出しの骨組みが出る。 */
@RunWith(AndroidJUnit4::class)
class LaunchTest {

    @get:Rule(order = 0)
    val reset = ResetAppData()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForHeading(title: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText(title) and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun loadsKanjiDataAndShowsSearchTab() {
        waitForHeading("名前を探す")
    }

    @Test
    fun tabsSwitchScreens() {
        waitForHeading("名前を探す")
        compose.onNodeWithText("調べる").performClick()
        waitForHeading("画数を調べる")
        // この時点で「お気に入り」はタブの名前だけ（見出しは押したあとに出る）
        compose.onNodeWithText("お気に入り").performClick()
        waitForHeading("お気に入り")
        compose.onNodeWithText("探す").performClick()
        waitForHeading("名前を探す")
    }

    @Test
    fun settingsOpensAndClosesWithBack() {
        waitForHeading("名前を探す")
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        waitForHeading("設定")
        Espresso.pressBack()
        waitForHeading("名前を探す")
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        compose.onNodeWithText("閉じる").performClick()
        waitForHeading("名前を探す")
    }
}
