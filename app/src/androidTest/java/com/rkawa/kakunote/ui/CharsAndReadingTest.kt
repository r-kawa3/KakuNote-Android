package com.rkawa.kakunote.ui

import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rkawa.kakunote.MainActivity
import com.rkawa.kakunote.ResetAppData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 避けたい字・使いたい字・読みの頭の部分（設定）と、「探す」の読み。期待する件数は共通テストケースと同じ。 */
@RunWith(AndroidJUnit4::class)
class CharsAndReadingTest {

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

    private fun openSettings() {
        compose.onAllNodesWithContentDescription("設定").onFirst().performClick()
        waitForHeading("設定")
    }

    private fun addChars(tag: String, text: String) {
        compose.onNodeWithTag("$tag.input").performScrollTo().performTextInput(text)
        compose.onNodeWithTag("$tag.add").performClick()
    }

    private fun closeSettings() {
        compose.onNodeWithTag("settings.close").performClick()
        waitForHeading("名前を探す")
    }

    /** 探して、件数（例: 「1件」）か、0 件・エラーの画面の目印を返す */
    private fun search(total: String, reading: String? = null): String {
        compose.onNodeWithTag("search.total").performTextInput(total)
        reading?.let { compose.onNodeWithTag("search.reading").performScrollTo().performTextInput(it) }
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            listOf("result.total", "result.empty", "result.error").any {
                compose.onAllNodes(hasTestTag(it)).fetchSemanticsNodes().isNotEmpty()
            }
        }
        return listOf("result.total", "result.empty", "result.error").first {
            compose.onAllNodes(hasTestTag(it)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun avoidChar() {
        openSettings()
        addChars("settings.avoid", "乙")
        compose.onNodeWithTag("settings.avoid.remove.乙").assertExists()
        closeSettings()
        // avoid_auto_candidates: 2 文字・総画数 2・避けたい字「乙」 → 一一 だけ
        search("2")
        compose.onNodeWithTag("result.total").assertTextEquals("1件")
        compose.onNodeWithText("避けたい字 1字を除外").assertExists()
    }

    @Test
    fun useCharAndRemove() {
        openSettings()
        addChars("settings.use", "乙")
        closeSettings()
        // use_any_position: 2 文字・総画数 2・使いたい字「乙」 → 3 件
        search("2")
        compose.onNodeWithTag("result.total").assertTextEquals("3件")
        compose.onNodeWithText("使いたい字: 乙").assertExists()
        // 外すと元の 4 件に戻る
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("名前を探す")
        openSettings()
        compose.onNodeWithTag("settings.use.remove.乙").performScrollTo().performClick()
        compose.onNodeWithTag("settings.use.remove.乙").assertDoesNotExist()
    }

    @Test
    fun useRejectsNotNameChar() {
        openSettings()
        addChars("settings.use", "澤")
        compose.onNodeWithTag("settings.use.error").assertTextEquals("「澤」は名前には使えない字です")
    }

    @Test
    fun readingFilters() {
        // reading_haruto: 2 文字・総画数 16・読み「はると」 → 98 件
        search("16", reading = "はると")
        compose.onNodeWithTag("result.total").assertTextEquals("98件")
        compose.onNodeWithText("読み「はると」").assertExists()
    }

    @Test
    fun katakanaReadingBecomesHiragana() {
        compose.onNodeWithTag("search.reading").performScrollTo().performTextInput("ハルト")
        compose.onNodeWithTag("search.reading").performImeAction()
        compose.onNodeWithTag("search.reading").assertTextEquals("はると")
        search("16")
        compose.onNodeWithTag("result.total").assertTextEquals("98件")
    }

    @Test
    fun invalidReadingCannotSubmit() {
        compose.onNodeWithTag("search.total").performTextInput("16")
        compose.onNodeWithTag("search.reading").performScrollTo().performTextInput("haru")
        compose.onNodeWithTag("search.formError").assertTextEquals("読みは、ひらがな（またはカタカナ）30 文字までで入れてください")
        compose.onNodeWithTag("search.submit").assertIsNotEnabled()
    }

    @Test
    fun readingPrefix() {
        // reading_prefix_off / on: 心・春を固定、総画数 13、読み「こはる」 → オフは 0 件、オンは 1 件
        compose.onNodeWithTag("search.cell.1").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.fixed").performScrollTo().performClick()
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextInput("心")
        compose.onNodeWithTag("search.cell.2").performScrollTo().performClick()
        compose.onNodeWithTag("search.kind.fixed").performScrollTo().performClick()
        compose.onNodeWithTag("search.fixed").performScrollTo().performTextInput("春")
        compose.onNodeWithTag("search.total").performScrollTo().performTextInput("13")
        compose.onNodeWithTag("search.reading").performScrollTo().performTextInput("こはる")
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasTestTag("result.empty")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav.back").performClick()
        waitForHeading("名前を探す")
        openSettings()
        compose.onNodeWithTag("settings.readingPrefix").performScrollTo().performClick()
        closeSettings()
        compose.onNodeWithTag("search.submit").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasTestTag("result.total")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("result.total").assertTextEquals("1件")
        compose.onNodeWithText("読みの頭の部分も使う").assertExists()
    }
}
