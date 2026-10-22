package com.rkawa.kakunote.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.data.Favorite
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaHeader
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.lookup.LookupState
import com.rkawa.kakunote.ui.theme.Wa

/** 「お気に入り」タブ: 保存した名前と五格を並べて見比べる。押すと詳細、左にスワイプすると削除。 */
@Composable
fun FavoritesScreen(
    favorites: FavoritesModel,
    data: KanjiData,
    school: KakuSchool,
    onOpenSettings: () -> Unit,
    onSelect: (Favorite) -> Unit,
    onGoSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        WaHeader(
            "好", "お気に入り", onOpenSettings,
            Modifier.padding(horizontal = Wa.Metrics.screenPadding).padding(top = 12.dp),
        )
        if (favorites.items.isEmpty()) {
            EmptyFavorites(favorites.errorMessage, onGoSearch)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().testTag("favorites.list"),
                contentPadding = PaddingValues(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${favorites.items.size}件",
                        style = Wa.gothic(13.sp, FontWeight.Bold),
                        color = Wa.secondaryText,
                        modifier = Modifier.testTag("favorites.total"),
                    )
                }
                items(favorites.items, key = { it.id }) { favorite ->
                    SwipeToDelete(onDelete = { favorites.remove(favorite) }) {
                        FavoriteCard(favorite, data, school, onDelete = { favorites.remove(favorite) }) { onSelect(favorite) }
                    }
                }
            }
        }
    }
}

/** 左にスワイプすると削除する。 */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(Wa.seal, RoundedCornerShape(Wa.Metrics.cornerRadius)).padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("削除", style = Wa.gothic(15.sp, FontWeight.Bold), color = Wa.onAccent)
            }
        },
    ) { content() }
}

/** お気に入りの 1 枚: 明朝の大きな名前、五格（同じ並び・同じ幅）、探したときの条件。 */
@Composable
private fun FavoriteCard(favorite: Favorite, data: KanjiData, school: KakuSchool, onDelete: () -> Unit, onClick: () -> Unit) {
    val fullName = listOfNotNull(favorite.sei, favorite.mei).joinToString(" ")
    // 五格は保存せず、表示のたびに計算する
    val values = remember(favorite, data) {
        val result = (LookupState.of(favorite.sei ?: "", favorite.mei, data, school) as? LookupState.Shown)?.result
        val kaku = result?.kaku
        when {
            kaku != null -> listOf("天格" to kaku.tenkaku, "人格" to kaku.jinkaku, "地格" to kaku.chikaku, "外格" to kaku.gaikaku, "総格" to kaku.soukaku)
            result?.nameOnlyTotal != null -> listOf("総画数" to result.nameOnlyTotal)
            else -> emptyList()
        }
    }
    val summary = favorite.query?.let { ConditionSummary.text(it) }
    val description = (listOf(fullName) + values.map { "${it.first} ${it.second}画" } + listOfNotNull(summary)).joinToString("、")
    Column(
        Modifier
            .fillMaxWidth()
            .background(Wa.background)
            .waCard()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("favorite.card")
            .clearAndSetSemantics {
                contentDescription = description
                customActions = listOf(CustomAccessibilityAction("削除") { onDelete(); true })
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fullName, style = Wa.mincho(28.sp, bold = true), color = Wa.ink, modifier = Modifier.weight(1f))
            Text("›", style = Wa.gothic(20.sp, FontWeight.Bold), color = Wa.dashed)
        }
        Row(Modifier.fillMaxWidth().background(Wa.background, RoundedCornerShape(Wa.Metrics.cellCornerRadius))) {
            for ((title, value) in values) {
                Column(
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(title, style = Wa.gothic(11.sp), color = Wa.secondaryText)
                    Text(
                        "$value",
                        style = Wa.mincho(20.sp, bold = true),
                        color = if (title == "総格" || title == "総画数") Wa.accent else Wa.ink,
                    )
                }
            }
        }
        if (summary != null) Text(summary, style = Wa.gothic(12.sp), color = Wa.secondaryText)
    }
}

/** お気に入りがないときの画面: 点線の枠のハート、説明、「探しにいく」。 */
@Composable
private fun EmptyFavorites(errorMessage: String?, onGoSearch: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Wa.Metrics.screenPadding).padding(bottom = 60.dp).testTag("favorites.empty"),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(120.dp).drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = Wa.dashed,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(Wa.Metrics.cornerRadius.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(WaIcons.Heart, contentDescription = null, tint = Wa.dashed, modifier = Modifier.size(48.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("お気に入りはまだありません", style = Wa.mincho(22.sp, bold = true).copy(letterSpacing = 1.sp), color = Wa.ink)
            Text(
                errorMessage ?: "「探す」の候補の一覧で、\nハートを押すと保存できます。",
                style = Wa.gothic(13.sp).copy(lineHeight = 21.sp),
                color = Wa.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onGoSearch)
                .drawBehind {
                    drawRoundRect(
                        Wa.accent, cornerRadius = CornerRadius(Wa.Metrics.cornerRadius.toPx()), style = Stroke(1.dp.toPx()),
                    )
                }
                .padding(horizontal = 28.dp)
                .testTag("favorites.goSearch"),
            contentAlignment = Alignment.Center,
        ) {
            Text("探しにいく", style = Wa.gothic(15.sp, FontWeight.Bold).copy(letterSpacing = 2.sp), color = Wa.accent)
        }
        Spacer(Modifier.size(0.dp))
    }
}
