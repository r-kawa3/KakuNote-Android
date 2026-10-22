package com.rkawa.kakunote.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/** 設定画面。今は枠だけ（中身はチケット 09・10 で作る）。端末の戻る操作でも閉じる。 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(
        Modifier
            .fillMaxSize()
            .background(Wa.background)
            .safeDrawingPadding()
            .padding(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "設定",
                style = Wa.mincho(22.sp, bold = true),
                color = Wa.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onClose) { Text("閉じる", color = Wa.accent, style = Wa.gothic()) }
        }
        Text(
            "姓・避けたい字・使いたい字・「このアプリについて」を置く画面（チケット 09・10 で作る）",
            style = Wa.gothic(),
            color = Wa.secondaryText,
            modifier = Modifier.fillMaxWidth().waCard().padding(16.dp),
        )
    }
}
