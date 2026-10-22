package com.rkawa.kakunote.ui.search

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

/** 「名前を探す」のタブ。今は見出しだけ。 */
@Composable
fun SearchScreen(data: KanjiData, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    WaTabScreen(seal = "名", title = "名前を探す", onOpenSettings = onOpenSettings, modifier = modifier) {
        Text(
            "総画数・文字数・名前の形を入れて、名前の候補を探す画面（チケット 06 で作る）",
            style = Wa.gothic(),
            color = Wa.secondaryText,
            modifier = Modifier.fillMaxWidth().waCard().padding(16.dp),
        )
    }
}
