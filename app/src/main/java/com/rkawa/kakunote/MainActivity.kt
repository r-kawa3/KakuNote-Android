package com.rkawa.kakunote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rkawa.kakunote.data.BundledKanji
import com.rkawa.kakunote.logic.KanjiData
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                KanjiDataGate { data ->
                    // 画面はチケットで作る。今は読み込めたことだけを出す
                    Message("漢字のデータ（KANJIDIC2 ${data.meta["kanjidic2_database_version"]}）を読み込みました")
                }
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

// 色は和のデザインの土台（チケット 04）でテーマにまとめる。今は iOS の WaTheme と同じ値を直接使う
private val background = Color(0xFFF3ECDC)
private val ink = Color(0xFF2A2723)
private val accent = Color(0xFF1F4E6B)

@Composable
private fun Message(text: String, loading: Boolean = false, retry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().background(background).padding(22.dp).testTag("kanjiDataGate"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) CircularProgressIndicator(color = accent)
        Text(text, color = ink, textAlign = TextAlign.Center)
        if (retry != null) {
            Button(onClick = retry, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text("もう一度読み込む")
            }
        }
    }
}
