package com.rkawa.kakunote.ui

import com.rkawa.kakunote.ui.theme.Wa
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** 和のデザインの色を確かめる。 */
class WaThemeTest {

    /** 文字と背景の組み合わせが読める明るさの差（WCAG のコントラスト比）を持つ。iOS と同じ組み合わせ。 */
    @Test
    fun textContrast() {
        val pairs = listOf(
            Triple("墨 / 生成り", Wa.Hex.INK to Wa.Hex.BACKGROUND, 4.5),
            Triple("墨 / カードの面", Wa.Hex.INK to Wa.Hex.SURFACE, 4.5),
            Triple("補足 / 生成り", Wa.Hex.SECONDARY_TEXT to Wa.Hex.BACKGROUND, 4.5),
            Triple("補足 / カードの面", Wa.Hex.SECONDARY_TEXT to Wa.Hex.SURFACE, 4.5),
            Triple("補足 / タブバー", Wa.Hex.SECONDARY_TEXT to Wa.Hex.SURFACE_MUTED, 4.5),
            Triple("藍の上の文字 / 藍", Wa.Hex.ON_ACCENT to Wa.Hex.ACCENT, 4.5),
            Triple("藍 / 生成り", Wa.Hex.ACCENT to Wa.Hex.BACKGROUND, 4.5),
            Triple("朱の上の文字 / 朱", Wa.Hex.ON_ACCENT to Wa.Hex.SEAL, 4.5),
            // 入力欄の例文は大きめの文字なので 3:1 でよい
            Triple("例文 / カードの面", Wa.Hex.PLACEHOLDER to Wa.Hex.SURFACE, 3.0),
        )
        for ((name, colors, minimum) in pairs) {
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$name: $ratio", ratio >= minimum)
        }
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(hex: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel((hex shr 16) and 0xFF) + 0.7152 * channel((hex shr 8) and 0xFF) + 0.0722 * channel(hex and 0xFF)
    }
}
