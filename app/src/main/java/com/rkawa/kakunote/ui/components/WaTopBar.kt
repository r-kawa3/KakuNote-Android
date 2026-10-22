package com.rkawa.kakunote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.ui.theme.Wa

/** 進んだ先の画面の上の帯: 左に「‹ 戻る」、真ん中に題名。端末の戻る操作でも戻れる（呼ぶ側で BackHandler）。 */
@Composable
fun WaTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).testTag("nav.back")) {
            Text("‹ 戻る", style = Wa.gothic(16.sp), color = Wa.accent)
        }
        Text(
            title,
            style = Wa.mincho(18.sp, bold = true),
            color = Wa.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 90.dp).semantics { heading() },
        )
    }
}
