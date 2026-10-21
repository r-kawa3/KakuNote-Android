package com.rkawa.kakunote

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** アプリを起動すると、漢字のデータを読み込んで先に進む。 */
@RunWith(AndroidJUnit4::class)
class LaunchTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun loadsKanjiDataOnLaunch() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("読み込みました", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
