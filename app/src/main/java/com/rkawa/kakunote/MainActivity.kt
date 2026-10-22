package com.rkawa.kakunote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.rkawa.kakunote.data.BundledKanji
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.KakuNoteApp
import com.rkawa.kakunote.ui.theme.Wa
import com.rkawa.kakunote.ui.theme.WaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 起動画面（生成り色に「画」の印）。漢字のデータを読み込むあいだは出しておく
        installSplashScreen().setKeepOnScreenCondition { BundledKanji.state.value is BundledKanji.State.Loading }
        // 画面の端まで背景を広げ、状態バー・ナビゲーションバーは明るい背景用（暗い文字・アイコン）にする。
        // 端末がダークモードでも、いつも明るい和の色で見せる
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)
        setContent {
            WaTheme {
                KanjiDataGate { data -> KakuNoteApp(data) }
            }
        }
    }
}

/** 漢字のデータを読み込んでから、content を出す。読み込みの間と、読み込めなかったときは、その画面を出す。 */
@Composable
fun KanjiDataGate(content: @Composable (KanjiData) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by BundledKanji.state.collectAsState()
    LaunchedEffect(Unit) { BundledKanji.load(context) }

    when (val current = state) {
        is BundledKanji.State.Loaded -> content(current.data)
        BundledKanji.State.Loading -> Message("漢字のデータを読み込んでいます", loading = true)
        is BundledKanji.State.Failed -> Message(
            "漢字のデータを読み込めませんでした。\nアプリを終了して、もう一度開いてください。",
            retry = { scope.launch { BundledKanji.load(context) } },
        )
    }
}

@Composable
private fun Message(text: String, loading: Boolean = false, retry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().background(Wa.background).safeDrawingPadding().padding(Wa.Metrics.screenPadding).testTag("kanjiDataGate"),
        verticalArrangement = Arrangement.spacedBy(Wa.Metrics.screenPadding, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) CircularProgressIndicator(color = Wa.accent)
        Text(text, color = Wa.ink, style = Wa.gothic(), textAlign = TextAlign.Center)
        if (retry != null) {
            Button(onClick = retry, colors = ButtonDefaults.buttonColors(containerColor = Wa.accent)) {
                Text("もう一度読み込む")
            }
        }
    }
}
