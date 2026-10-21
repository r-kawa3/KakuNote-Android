package com.rkawa.kakunote.logic

/** 五格（仕様書 kaku.md）。 */
data class Kaku(
    val tenkaku: Int,
    val jinkaku: Int,
    val chikaku: Int,
    val soukaku: Int,
    val gaikaku: Int,
) {
    fun value(of: KakuKind): Int = when (of) {
        KakuKind.TENKAKU -> tenkaku
        KakuKind.JINKAKU -> jinkaku
        KakuKind.CHIKAKU -> chikaku
        KakuKind.SOUKAKU -> soukaku
        KakuKind.GAIKAKU -> gaikaku
    }
}

/** 格の種類。raw は共通テストケースやお気に入りの JSON での名前。 */
enum class KakuKind(val raw: String) {
    TENKAKU("tenkaku"),
    JINKAKU("jinkaku"),
    CHIKAKU("chikaku"),
    SOUKAKU("soukaku"),
    GAIKAKU("gaikaku");

    companion object {
        fun of(raw: String): KakuKind? = entries.firstOrNull { it.raw == raw }
    }
}

/**
 * 流派。五格の計算方法を入れ替えられるようにする（最初の版は新字体式だけ）。
 *
 * 約束: 姓と名の文字数を決めたとき、各格は名の各位置の画数の 1 次式であること
 * （検索の打ち切りが、この性質を使って格の取りうる範囲を求める）。
 */
interface KakuSchool {
    /** 姓と名の各字の画数から五格を求める。sei・mei はどちらも 1 文字以上。 */
    fun kaku(sei: List<Int>, mei: List<Int>): Kaku

    /** 姓なしのときの総画数（＝ 地格）。 */
    fun nameOnlyTotal(mei: List<Int>): Int
}

/** 新字体式: 霊数なし。一字姓・一字名のときは、外格にその字の画数を足す。 */
object ShinjitaiSchool : KakuSchool {

    override fun kaku(sei: List<Int>, mei: List<Int>): Kaku {
        require(sei.isNotEmpty() && mei.isNotEmpty()) { "姓と名の両方が必要です" }
        val tenkaku = sei.sum()
        val chikaku = mei.sum()
        val jinkaku = sei.last() + mei.first()
        val soukaku = tenkaku + chikaku
        var gaikaku = soukaku - jinkaku
        if (sei.size == 1) gaikaku += sei[0]
        if (mei.size == 1) gaikaku += mei[0]
        return Kaku(tenkaku, jinkaku, chikaku, soukaku, gaikaku)
    }

    override fun nameOnlyTotal(mei: List<Int>): Int = mei.sum()
}

/** 名についての警告（仕様書 strokes.md「名前に使える字」）。 */
sealed class NameWarning {
    /** 名前に使えない字（position は 1 から数える） */
    data class NotNameChar(val char: String, val position: Int) : NameWarning()

    /** 名の 1 文字目に置けない字 */
    data class NotInitial(val char: String) : NameWarning()
}
