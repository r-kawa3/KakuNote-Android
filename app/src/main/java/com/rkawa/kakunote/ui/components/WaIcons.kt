package com.rkawa.kakunote.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * アプリで使うアイコン。アイコンのライブラリを足さずに、ここで線を描く
 * （ライセンスの表示を増やさないため）。24 × 24 の枠に、線で描く。
 */
object WaIcons {

    /** 探す（虫めがね） */
    val Search: ImageVector by lazy {
        outline("search") {
            circle(10.5f, 10.5f, 6.5f)
            moveTo(15.2f, 15.2f)
            lineTo(20.5f, 20.5f)
        }
    }

    /** 調べる（閉じた本） */
    val Book: ImageVector by lazy {
        outline("book") {
            moveTo(5.5f, 4f)
            lineTo(18.5f, 4f)
            lineTo(18.5f, 20f)
            lineTo(7f, 20f)
            curveTo(6.2f, 20f, 5.5f, 19.3f, 5.5f, 18.5f)
            close()
            moveTo(5.5f, 17f)
            curveTo(5.5f, 16.2f, 6.2f, 15.5f, 7f, 15.5f)
            lineTo(18.5f, 15.5f)
            moveTo(9.5f, 8f)
            lineTo(14.5f, 8f)
        }
    }

    /** お気に入り（ハートの線） */
    val Heart: ImageVector by lazy { outline("heart") { heart() } }

    /** 保存済みのお気に入り（塗りつぶしたハート） */
    val HeartFilled: ImageVector by lazy {
        ImageVector.Builder("heartFilled", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) { heart() }
        }.build()
    }

    /** 設定（歯車） */
    val Gear: ImageVector by lazy {
        outline("gear") {
            // 8 つの歯。外側（半径 9）と内側（半径 6.8）を交互に結ぶ
            val teeth = 8
            for (i in 0 until teeth * 4) {
                val angle = Math.PI * 2 * (i - 0.5) / (teeth * 4)
                val radius = if (i % 4 < 2) 9.0 else 6.8
                val x = (12 + radius * cos(angle)).toFloat()
                val y = (12 + radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3f)
        }
    }

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    private fun PathBuilder.heart() {
        moveTo(12f, 20f)
        curveTo(12f, 20f, 3.5f, 14.6f, 3.5f, 9f)
        curveTo(3.5f, 6.2f, 5.6f, 4.3f, 8f, 4.3f)
        curveTo(9.7f, 4.3f, 11.1f, 5.2f, 12f, 6.6f)
        curveTo(12.9f, 5.2f, 14.3f, 4.3f, 16f, 4.3f)
        curveTo(18.4f, 4.3f, 20.5f, 6.2f, 20.5f, 9f)
        curveTo(20.5f, 14.6f, 12f, 20f, 12f, 20f)
        close()
    }
}
