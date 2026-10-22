package com.rkawa.kakunote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rkawa.kakunote.ui.theme.Wa

/**
 * 中身の幅を Wa.Metrics.maxContentWidth までにして、真ん中に置く。タブレットや折りたたみの広い画面
 * （横向きを含む）で、カードが横に伸びすぎないようにする。スマートフォンの幅では何も変わらない。
 */
fun Modifier.waContentWidth(): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = Wa.Metrics.maxContentWidth)

/** タブの 1 画面: 上に見出し、その下を縦にスクロールする。左右の余白は Wa.Metrics.screenPadding。 */
@Composable
fun WaTabScreen(
    seal: String,
    title: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        WaHeader(seal, title, onOpenSettings)
        content()
    }
}
