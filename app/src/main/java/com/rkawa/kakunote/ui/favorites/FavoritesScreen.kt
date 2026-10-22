package com.rkawa.kakunote.ui.favorites

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

/** 「お気に入り」のタブ。今は見出しだけ。 */
@Composable
fun FavoritesScreen(data: KanjiData, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    WaTabScreen(seal = "好", title = "お気に入り", onOpenSettings = onOpenSettings, modifier = modifier) {
        Text(
            "保存した名前を見比べる画面（チケット 08 で作る）",
            style = Wa.gothic(),
            color = Wa.secondaryText,
            modifier = Modifier.fillMaxWidth().waCard().padding(16.dp),
        )
    }
}
