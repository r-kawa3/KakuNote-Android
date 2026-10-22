package com.rkawa.kakunote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 和のデザインの定数（色・文字・寸法）。値は iOS の KakuNote/Design/WaTheme.swift と同じ。
 * 生成り色の背景、墨色の文字、藍色のアクセント、朱色は小さな差し色だけ。
 */
object Wa {

    /** 色の値（0xRRGGBB）。テストでコントラストを確かめるのに使う */
    object Hex {
        /** 背景（生成り） */
        const val BACKGROUND = 0xF3ECDC
        /** カードの面 */
        const val SURFACE = 0xFBF7EC
        /** くすんだ面（タブバーなど） */
        const val SURFACE_MUTED = 0xEDE5D1
        /** 枠線 */
        const val BORDER = 0xD8CDB6
        /** 区切り線 */
        const val DIVIDER = 0xE4DAC4
        /** 文字（墨） */
        const val INK = 0x2A2723
        /** 補足の文字 */
        const val SECONDARY_TEXT = 0x6B6455
        /** 入力欄の例文 */
        const val PLACEHOLDER = 0x7A7260
        /** 点線の枠 */
        const val DASHED = 0xA89C82
        /** アクセント（藍） */
        const val ACCENT = 0x1F4E6B
        /** 藍・朱の上の文字 */
        const val ON_ACCENT = 0xFBF7EC
        /** 藍の上の薄い文字 */
        const val ON_ACCENT_FAINT = 0xE6DFCB
        /** 差し色（朱）。見出し横の印、保存済みのハート */
        const val SEAL = 0xC4452C
    }

    val background = color(Hex.BACKGROUND)
    val surface = color(Hex.SURFACE)
    val surfaceMuted = color(Hex.SURFACE_MUTED)
    val border = color(Hex.BORDER)
    val divider = color(Hex.DIVIDER)
    val ink = color(Hex.INK)
    val secondaryText = color(Hex.SECONDARY_TEXT)
    val placeholder = color(Hex.PLACEHOLDER)
    val dashed = color(Hex.DASHED)
    val accent = color(Hex.ACCENT)
    val onAccent = color(Hex.ON_ACCENT)
    val onAccentFaint = color(Hex.ON_ACCENT_FAINT)
    val seal = color(Hex.SEAL)

    /**
     * 漢字と見出しの明朝体。端末に入っている明朝体（serif）を使い、ない機種ではゴシック体になる
     * （フォントはアプリに入れない。2026-10-22 にユーザーと決めた）。文字の大きさの設定に合わせて大きくなる
     */
    fun mincho(size: TextUnit, bold: Boolean = false): TextStyle =
        TextStyle(fontFamily = FontFamily.Serif, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = size)

    /** 本文のゴシック（端末の標準の文字） */
    fun gothic(size: TextUnit = 16.sp, weight: FontWeight = FontWeight.Normal): TextStyle =
        TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size)

    /** 寸法 */
    object Metrics {
        /** 画面の左右の余白 */
        val screenPadding = 22.dp
        /** カードの角丸 */
        val cornerRadius = 12.dp
        /** 大きなカードの角丸 */
        val largeCornerRadius = 16.dp
        /** 名前のマスの大きさと角丸 */
        val cellSize = 64.dp
        val cellCornerRadius = 8.dp
        /** 押せる部分の最小の大きさ */
        val minTapSize = 48.dp
        /** 下の大きなボタンの高さ */
        val primaryButtonHeight = 56.dp
        /** 見出し横の印（朱の四角）の大きさ */
        val sealSize = 28.dp
    }

    private fun color(hex: Int) = Color(0xFF000000.toInt() or hex)
}

/** アプリ全体に和の色を当てる。端末がダークモードでも、いつも明るい色で見せる。 */
@Composable
fun WaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Wa.accent,
            onPrimary = Wa.onAccent,
            secondary = Wa.accent,
            onSecondary = Wa.onAccent,
            tertiary = Wa.seal,
            onTertiary = Wa.onAccent,
            background = Wa.background,
            onBackground = Wa.ink,
            surface = Wa.surface,
            onSurface = Wa.ink,
            surfaceVariant = Wa.surfaceMuted,
            onSurfaceVariant = Wa.secondaryText,
            surfaceContainer = Wa.surfaceMuted,
            outline = Wa.border,
            outlineVariant = Wa.divider,
            error = Wa.seal,
            onError = Wa.onAccent,
        ),
        content = content,
    )
}
