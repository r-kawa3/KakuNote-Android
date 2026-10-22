package com.rkawa.kakunote.ui.favorites

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.theme.Wa

/** 候補をお気に入りに保存・削除するハートのボタン。保存済みは朱の塗りつぶし、未保存は補足の色の線。 */
@Composable
fun FavoriteButton(favorites: FavoritesModel, sei: String?, mei: String, query: SearchQuery?, modifier: Modifier = Modifier) {
    val saved = favorites.isFavorite(sei, mei)
    IconButton(
        onClick = { favorites.toggle(sei, mei, query) },
        modifier = modifier
            .size(Wa.Metrics.minTapSize)
            .testTag("favorite.toggle.$mei")
            .semantics {
                contentDescription = if (saved) "お気に入りから外す" else "お気に入りに追加"
                stateDescription = if (saved) "保存済み" else ""
            },
    ) {
        Icon(
            if (saved) WaIcons.HeartFilled else WaIcons.Heart,
            contentDescription = null,
            tint = if (saved) Wa.seal else Wa.secondaryText,
            modifier = Modifier.size(24.dp),
        )
    }
}
