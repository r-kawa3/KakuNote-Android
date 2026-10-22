package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 「調べる」画面: 姓名を入れると、画数と五格が出る。 */
@RunWith(AndroidJUnit4::class)
class LookupScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun openLookupTab() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("名前を探す") and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("調べる").performClick()
        compose.waitUntil { compose.onAllNodes(hasText("画数を調べる") and isHeading()).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun showsKakuForSeiAndMei() {
        compose.onNodeWithTag("lookup.sei").performTextInput("林")
        compose.onNodeWithTag("lookup.mei").performTextInput("健太")
        compose.onNodeWithTag("lookup.kaku.soukaku").assertTextEquals("23画")
        compose.onNodeWithTag("lookup.kaku.tenkaku").assertTextEquals("8画")
        compose.onNodeWithTag("lookup.kaku.jinkaku").assertTextEquals("19画")
        compose.onNodeWithTag("lookup.kaku.chikaku").assertTextEquals("15画")
        compose.onNodeWithTag("lookup.kaku.gaikaku").assertTextEquals("12画")
    }

    @Test
    fun odorijiAndHiragana() {
        compose.onNodeWithTag("lookup.sei").performTextInput("佐々木")
        compose.onNodeWithTag("lookup.mei").performTextInput("さくら")
        compose.onNodeWithTag("lookup.kaku.soukaku").assertTextEquals("21画")
        compose.onNodeWithTag("lookup.kaku.gaikaku").assertTextEquals("14画")
    }

    @Test
    fun nameOnlyShowsTotal() {
        compose.onNodeWithTag("lookup.mei").performTextInput("陽菜")
        compose.onNodeWithTag("lookup.kaku.total").assertTextEquals("23画")
    }

    @Test
    fun warnsAndReportsUnknownChars() {
        compose.onNodeWithTag("lookup.mei").performTextInput("澤")
        compose.onNodeWithText("「澤」（名の1文字目）は名前には使えません", substring = true).assertExists()
        compose.onNodeWithTag("lookup.mei").performTextReplacement("健B")
        compose.onNodeWithTag("lookup.error").assertTextEquals("⚠ 画数がわからない字があります: 「B」")
    }

    @Test
    fun inputStaysAfterSwitchingTabs() {
        compose.onNodeWithTag("lookup.mei").performTextInput("陽菜")
        compose.onNodeWithText("探す").performClick()
        compose.onNodeWithText("調べる").performClick()
        compose.onNodeWithTag("lookup.kaku.total").assertTextEquals("23画")
    }
}
