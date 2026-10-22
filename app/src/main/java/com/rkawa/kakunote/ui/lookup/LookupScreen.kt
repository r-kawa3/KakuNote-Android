package com.rkawa.kakunote.ui.lookup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaTabScreen
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/** 「画数を調べる」のタブ。今は見出しだけ。 */
@Composable
fun LookupScreen(data: KanjiData, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    WaTabScreen(seal = "画", title = "画数を調べる", onOpenSettings = onOpenSettings, modifier = modifier) {
        Text(
            "姓と名を入れて、画数と五格を調べる画面（チケット 05 で作る）",
            style = Wa.gothic(),
            color = Wa.secondaryText,
            modifier = Modifier.fillMaxWidth().waCard().padding(16.dp),
        )
    }
}
