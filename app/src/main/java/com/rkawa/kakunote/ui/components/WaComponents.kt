package com.rkawa.kakunote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.ui.theme.Wa

/** 見出しの横に置く、朱の四角に白抜きの漢字 1 字の印。 */
@Composable
fun SealMark(character: String) {
    Box(
        Modifier
            .size(Wa.Metrics.sealSize)
            .background(Wa.seal, RoundedCornerShape(3.dp))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(character, style = Wa.mincho(16.sp, bold = true), color = Wa.onAccent)
    }
}

/**
 * 画面の上部の見出し: 朱の印 + 明朝体の題名、右に「設定」の丸いボタン。
 * seal は印の漢字 1 字（探す「名」、調べる「画」、お気に入り「好」）。
 */
@Composable
fun WaHeader(seal: String, title: String, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = Wa.Metrics.minTapSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SealMark(seal)
        Spacer(Modifier.size(10.dp))
        Text(
            title,
            style = Wa.mincho(26.sp, bold = true).copy(letterSpacing = 1.5.sp),
            color = Wa.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        SettingsCircleButton(onOpenSettings)
    }
}

/** 設定画面を開く丸いボタン（歯車）。 */
@Composable
fun SettingsCircleButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Wa.Metrics.minTapSize)
            .clip(CircleShape)
            .background(Wa.surface, CircleShape)
            .border(1.dp, Wa.border, CircleShape),
    ) {
        Icon(WaIcons.Gear, contentDescription = "設定", tint = Wa.ink, modifier = Modifier.size(22.dp))
    }
}

/** カードの面: 面の色、細い枠線、角丸。 */
fun Modifier.waCard(cornerRadius: Dp = Wa.Metrics.cornerRadius): Modifier =
    this
        .background(Wa.surface, RoundedCornerShape(cornerRadius))
        .border(1.dp, Wa.border, RoundedCornerShape(cornerRadius))

/** 小さな見出し（「文字数」「名前の形」など）。 */
@Composable
fun WaSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Wa.gothic(13.sp, FontWeight.Bold).copy(letterSpacing = 1.sp),
        color = Wa.ink,
        modifier = modifier,
    )
}

/** チェックボックス。押すたびにオン・オフが替わる。色は置く面に合わせて渡す。 */
@Composable
fun WaCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Wa.ink,
) {
    Row(
        modifier
            .defaultMinSize(minHeight = Wa.Metrics.minTapSize)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .border(1.5.dp, color, RoundedCornerShape(3.dp))
                .background(if (checked) color else Color.Transparent, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text("✓", color = if (color == Wa.ink) Wa.surface else Wa.accent, style = Wa.gothic(12.sp, FontWeight.Bold))
            }
        }
        Text(title, style = Wa.gothic(13.sp, FontWeight.Bold), color = color)
    }
}
