package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 設定画面（姓の登録・「このアプリについて」）と、登録した姓を使う「探す」「調べる」。 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {

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

    private fun registerSei(sei: String) {
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        waitForHeading("設定")
        compose.onNodeWithTag("settings.sei").performTextReplacement(sei)
    }

    private fun closeSettings() {
        compose.onNodeWithTag("settings.close").performClick()
        compose.waitUntil { compose.onAllNodes(hasTestTag("settings.close")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun registeredSeiShowsTenkakuAndSoukakuCard() {
        registerSei("田中")
        compose.onNodeWithTag("settings.tenkaku").assertTextEquals("9画")
        closeSettings()
        compose.onNodeWithTag("search.sei").assertTextEquals("姓「田中」")
        compose.onNodeWithTag("search.tenkaku").assertTextEquals("9画")
        compose.onNodeWithText("総格").assertExists()
        // 「姓を使う」をオフにすると、名前だけの「総画数」に替わる
        compose.onNodeWithTag("search.useSei").performClick()
        compose.onNodeWithText("総画数").assertExists()
        compose.onNodeWithTag("search.otherKaku").assertDoesNotExist()
    }

    @Test
    fun searchWithSeiAndOtherKaku() {
        // 共通テストケースの sei_two_conditions: 田中・2 文字・人格 15・総格 24 → 23,716 件
        registerSei("田中")
        closeSettings()
        compose.onNodeWithTag("search.total").performTextInput("24")
        compose.onNodeWithTag("search.otherKaku").performScrollTo().performClick()
        compose.onNodeWithTag("search.kaku.jinkaku").performScrollTo().performTextInput("15")
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasTestTag("result.total")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("result.total").assertTextEquals("23,716件")
        compose.onNodeWithText("人格 15画").assertExists()
        compose.onNodeWithText("総格 24画").assertExists()
        compose.onNodeWithText("姓「田中」").assertExists()
    }

    @Test
    fun unknownCharInSei() {
        registerSei("田A")
        compose.onNodeWithTag("settings.seiError").assertTextEquals("画数がわからない字があります: 「A」")
        closeSettings()
        compose.onNodeWithTag("search.tenkaku.error").assertTextEquals("画数がわからない字があります: 「A」")
    }

    @Test
    fun lookupUsesRegisteredSei() {
        compose.onNodeWithText("調べる").performClick()
        waitForHeading("画数を調べる")
        compose.onNodeWithTag("lookup.useSei").assertDoesNotExist()
        registerSei("田中")
        closeSettings()
        compose.onNodeWithTag("lookup.useSei").assertExists()
        compose.onNodeWithTag("lookup.sei").assertTextEquals("田中").assertIsNotEnabled()
        compose.onNodeWithTag("lookup.mei").performTextInput("健太")
        compose.onNodeWithTag("lookup.kaku.tenkaku").assertTextEquals("9画")
        // オフにすると自由に入力できる
        compose.onNodeWithTag("lookup.useSei").performClick()
        compose.onNodeWithTag("lookup.sei").performTextInput("林")
        compose.onNodeWithTag("lookup.kaku.tenkaku").assertTextEquals("8画")
    }

    @Test
    fun aboutShowsKanjidic2Version() {
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        waitForHeading("設定")
        compose.onNodeWithTag("settings.about").performClick()
        waitForHeading("このアプリについて")
        compose.onNodeWithTag("about.app.version").assertTextEquals("1.0.0")
        compose.onNodeWithTag("about.KANJIDIC2.version").assertTextEquals("2026-291")
        compose.onNodeWithText("CC BY-SA 4.0").assertExists()
        // 戻ると設定画面
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("設定")
    }

    @Test
    fun addButtonEnabledOnlyWithInput() {
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        waitForHeading("設定")
        compose.onNodeWithTag("settings.use.add").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("settings.use.input").performScrollTo().performTextInput("陽")
        compose.onNodeWithTag("settings.use.add").assertIsEnabled().performClick()
        compose.onNodeWithTag("settings.use.remove.陽").assertExists()
        compose.onNodeWithTag("settings.version").performScrollTo().assertTextEquals("KakuNote バージョン 1.0.0")
    }

    @Test
    fun seiKeepsAfterRestart() {
        registerSei("佐藤")
        closeSettings()
        compose.activityRule.scenario.recreate()
        waitForHeading("名前を探す")
        compose.onNodeWithTag("search.sei").assertTextEquals("姓「佐藤」")
    }
}
