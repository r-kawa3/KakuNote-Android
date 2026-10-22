package com.rkawa.kakunote.ui.lookup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.logic.Kaku
import com.rkawa.kakunote.logic.LookupChar
import com.rkawa.kakunote.logic.LookupResult
import com.rkawa.kakunote.ui.components.WaSectionLabel
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/**
 * 「調べる」の結果: 警告 → 文字ごとの画数のマス → 総格（名だけなら総画数）の大きなカード →
 * 天格・人格・地格・外格の 2×2 のカード → 流派の注記。候補の詳細でも使う。
 */
@Composable
fun LookupResultView(result: LookupResult, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (result.warnings.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().waCard().padding(14.dp).testTag("lookup.warnings"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (warning in result.warnings) {
                    Text("⚠ " + LookupMessages.message(warning), style = Wa.gothic(15.sp), color = Wa.seal)
                }
            }
        }
        CharCells(result)
        val kaku = result.kaku
        val total = result.nameOnlyTotal
        if (kaku != null) {
            BigKakuCard("総格", kaku.soukaku, "姓名すべての合計", "lookup.kaku.soukaku")
            KakuGrid(kaku, LookupMessages.gaikakuNote(result.sei.size, result.mei.size))
        } else if (total != null) {
            BigKakuCard("総画数", total, "名の合計", "lookup.kaku.total")
        }
        Text(
            "画数の数え方は、流派によって異なる場合があります。",
            style = Wa.gothic(11.sp),
            color = Wa.secondaryText,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun CharCells(result: LookupResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WaSectionLabel("文字ごとの画数")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            for (item in result.sei) CharCell(item, isMei = false)
            if (result.sei.isNotEmpty() && result.mei.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(Wa.Metrics.cellSize)
                        .background(Wa.dashed.copy(alpha = 0.7f)),
                )
            }
            for (item in result.mei) CharCell(item, isMei = true)
        }
    }
}

/** 1 文字のマスと、その下の画数。名の字は藍の枠で目立たせる。 */
@Composable
private fun CharCell(item: LookupChar, isMei: Boolean) {
    val shape = RoundedCornerShape(Wa.Metrics.cellCornerRadius)
    val description = buildString {
        append("${if (isMei) "名" else "姓"}の字 ${item.char}、${item.strokes}画")
        if (item.altStrokes.isNotEmpty()) append("、別の数え方 " + item.altStrokes.joinToString("・") { "${it}画" })
    }
    Column(
        Modifier
            .defaultMinSize(minWidth = Wa.Metrics.cellSize)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(Wa.Metrics.cellSize)
                .background(Wa.surface, shape)
                .border(if (isMei) 2.dp else 1.dp, if (isMei) Wa.accent else Wa.border, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.char, style = Wa.mincho(34.sp, bold = true), color = Wa.ink)
        }
        Text("${item.strokes}画", style = Wa.gothic(13.sp, FontWeight.Bold), color = Wa.accent)
        if (item.altStrokes.isNotEmpty()) {
            Text(LookupMessages.altStrokes(item.altStrokes), style = Wa.gothic(11.sp), color = Wa.secondaryText)
        }
    }
}

/** 藍の大きなカード（総格、名だけなら総画数）。 */
@Composable
private fun BigKakuCard(title: String, value: Int, note: String, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Wa.accent, RoundedCornerShape(Wa.Metrics.largeCornerRadius))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Wa.gothic(13.sp, FontWeight.Bold).copy(letterSpacing = 2.sp), color = Wa.onAccent)
            Text(
                buildAnnotatedString {
                    withStyle(Wa.mincho(56.sp, bold = true).toSpanStyle()) { append("$value") }
                    withStyle(Wa.mincho(22.sp, bold = true).toSpanStyle().merge(SpanStyle(color = Wa.onAccentFaint))) { append("画") }
                },
                color = Wa.onAccent,
                modifier = Modifier.testTag(tag),
            )
        }
        Text(note, style = Wa.gothic(12.sp), color = Wa.onAccentFaint, modifier = Modifier.padding(bottom = 8.dp))
    }
}

/** 天格・人格・地格・外格を 2×2 に並べる。 */
@Composable
private fun KakuGrid(kaku: Kaku, gaikakuNote: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KakuCard("天格", "姓の合計", kaku.tenkaku, "lookup.kaku.tenkaku", Modifier.weight(1f))
            KakuCard("人格", "姓の末尾＋名の先頭", kaku.jinkaku, "lookup.kaku.jinkaku", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KakuCard("地格", "名の合計", kaku.chikaku, "lookup.kaku.chikaku", Modifier.weight(1f))
            KakuCard("外格", gaikakuNote, kaku.gaikaku, "lookup.kaku.gaikaku", Modifier.weight(1f))
        }
    }
}

@Composable
private fun KakuCard(title: String, note: String, value: Int, tag: String, modifier: Modifier) {
    Row(
        modifier.defaultMinSize(minHeight = 64.dp).waCard().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Wa.gothic(15.sp, FontWeight.Bold).copy(letterSpacing = 1.sp), color = Wa.ink)
            Text(note, style = Wa.gothic(11.sp), color = Wa.secondaryText, maxLines = 2)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(Wa.mincho(28.sp, bold = true).toSpanStyle()) { append("$value") }
                withStyle(Wa.mincho(14.sp).toSpanStyle().merge(SpanStyle(color = Wa.secondaryText))) { append("画") }
            },
            color = Wa.ink,
            modifier = Modifier.testTag(tag),
        )
    }
}
