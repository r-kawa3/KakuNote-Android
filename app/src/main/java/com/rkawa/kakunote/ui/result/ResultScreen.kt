package com.rkawa.kakunote.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaTopBar
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa
import java.text.NumberFormat

/** 候補の一覧: 上に条件のチップと件数、その下に候補のカード。下までスクロールすると続きを作る。 */
@Composable
fun ResultScreen(
    state: ResultListState,
    data: KanjiData,
    school: KakuSchool,
    onBack: () -> Unit,
    onSelect: (ResultRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state) { state.start(data, school) }

    Column(modifier.fillMaxSize()) {
        WaTopBar("候補", onBack)
        when (val phase = state.phase) {
            ResultListState.Phase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Wa.accent, modifier = Modifier.testTag("result.loading"))
            }
            is ResultListState.Phase.Failed -> Unavailable("検索できませんでした", phase.message, "result.error")
            is ResultListState.Phase.Ready ->
                if (phase.total == 0L) {
                    Unavailable("該当する名前がありません", "条件を変えてみてください。", "result.empty")
                } else {
                    ResultList(state, phase.total, onSelect)
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultList(state: ResultListState, total: Long, onSelect: (ResultRow) -> Unit) {
    val listState = state.listState
    // 最後の行が見えてきたら、続きを作る
    LaunchedEffect(listState, state) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= listState.layoutInfo.totalItemsCount - 5 && state.hasMore) state.loadMore() }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("result.list"),
        contentPadding = PaddingValues(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                for (chip in state.chips) ConditionChip(chip)
            }
        }
        item {
            Text(
                "${NumberFormat.getIntegerInstance().format(total)}件",
                style = Wa.gothic(13.sp, FontWeight.Bold),
                color = Wa.secondaryText,
                modifier = Modifier.testTag("result.total"),
            )
        }
        itemsIndexed(state.rows) { _, row -> CandidateRow(row) { onSelect(row) } }
    }
}

/** 条件のチップ。格の条件は藍で塗り、それ以外は枠だけ。 */
@Composable
private fun ConditionChip(chip: SearchSummary.Chip) {
    Box(
        Modifier
            .defaultMinSize(minHeight = 30.dp)
            .background(if (chip.isPrimary) Wa.accent else Wa.surface, CircleShape)
            .border(1.dp, if (chip.isPrimary) Color.Transparent else Wa.border, CircleShape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            chip.text,
            style = Wa.gothic(12.sp, if (chip.isPrimary) FontWeight.Bold else FontWeight.Normal),
            color = if (chip.isPrimary) Wa.onAccent else Wa.ink,
        )
    }
}

/** 候補の 1 行: 明朝の大きな名前と、画数の内訳（ハートはお気に入りのチケットで足す）。 */
@Composable
private fun CandidateRow(row: ResultRow, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .waCard()
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .testTag("result.row")
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(row.name, row.breakdown, row.kakuNote).joinToString("、")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.name, style = Wa.mincho(32.sp, bold = true), color = Wa.ink)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.breakdown, style = Wa.gothic(12.sp), color = Wa.secondaryText)
            row.kakuNote?.let { Text(it, style = Wa.gothic(12.sp, FontWeight.Bold), color = Wa.accent) }
        }
    }
}

/** 0 件・エラーの画面。 */
@Composable
private fun Unavailable(title: String, message: String, tag: String) {
    Column(
        Modifier.fillMaxSize().padding(Wa.Metrics.screenPadding).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = Wa.mincho(22.sp, bold = true), color = Wa.ink, textAlign = TextAlign.Center)
        Text(message, style = Wa.gothic(15.sp), color = Wa.secondaryText, textAlign = TextAlign.Center)
    }
}
