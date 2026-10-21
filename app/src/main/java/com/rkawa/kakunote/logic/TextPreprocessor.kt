package com.rkawa.kakunote.logic

import java.text.Normalizer

/** 入力文字列が前処理の規則に合わないときのエラー。 */
sealed class InputError : Exception() {
    data object Empty : InputError() { private fun readResolve(): Any = Empty }
    data object SpaceInside : InputError() { private fun readResolve(): Any = SpaceInside }
    data object DanglingSoundMark : InputError() { private fun readResolve(): Any = DanglingSoundMark }
    data class InvalidSoundMark(val base: String) : InputError()
}

/**
 * 姓・名の入力文字列を、画数を数える前に 1 文字ずつに分ける（仕様書 strokes.md「入力文字列の前処理」）。
 *
 * Unicode の正規化（NFC・NFKC）は全体にはかけない。互換漢字（例: 﨑）が統合漢字（崎）に
 * 置き換わり、画数や「名前に使えるか」が変わってしまうため。
 * 1 文字は Unicode のコードポイント 1 つ（Kotlin の Char ではない。サロゲートペアも 1 文字）。
 * Kotlin の String は正準等価な字を同じとみなさないので、字の比較やキーには String をそのまま使える。
 */
object TextPreprocessor {

    private val spaces = setOf(0x20, 0x3000, 0x09)
    private val combiningSoundMarks = setOf(0x3099, 0x309A)

    fun characters(text: String): List<String> {
        // 前後の空白を取り除く
        var scalars = text.codePoints().toArray().toList()
        scalars = scalars.dropWhile { it in spaces }.dropLastWhile { it in spaces }

        // 1. 半角カタカナを全角にする（その部分だけ NFKC。半角の濁点は結合用の濁点になる）
        val widened = mutableListOf<Int>()
        var index = 0
        while (index < scalars.size) {
            if (!isHalfwidthKatakana(scalars[index])) {
                widened.add(scalars[index])
                index += 1
                continue
            }
            var end = index
            while (end < scalars.size && isHalfwidthKatakana(scalars[end])) end += 1
            val segment = string(scalars.subList(index, end))
            Normalizer.normalize(segment, Normalizer.Form.NFKC).codePoints().forEach { widened.add(it) }
            index = end
        }

        // 2. 結合用の濁点・半濁点を直前の字と合成する
        val result = mutableListOf<String>()
        for (scalar in widened) {
            if (scalar in combiningSoundMarks) {
                if (result.isEmpty()) throw InputError.DanglingSoundMark
                val base = result.removeAt(result.lastIndex)
                // NFC はこの 2 つのコードポイントだけにかける。合成できるのは、かなに濁点・半濁点を付けたときだけ
                val composed = Normalizer.normalize(base + string(listOf(scalar)), Normalizer.Form.NFC)
                if (composed.codePointCount(0, composed.length) != 1 || composed.codePointAt(0) !in 0x3041..0x30FF) {
                    throw InputError.InvalidSoundMark(base)
                }
                result.add(composed)
            } else {
                result.add(string(listOf(scalar)))
            }
        }

        // 3. 途中の空白はエラー
        if (result.any { it.codePointCount(0, it.length) == 1 && it.codePointAt(0) in spaces }) {
            throw InputError.SpaceInside
        }
        if (result.isEmpty()) throw InputError.Empty
        return result
    }

    private fun isHalfwidthKatakana(scalar: Int): Boolean = scalar in 0xFF61..0xFF9F

    private fun string(codePoints: List<Int>): String =
        buildString { codePoints.forEach { appendCodePoint(it) } }
}

/** コードポイント 1 つの文字列。 */
internal fun codePointString(codePoint: Int): String = String(Character.toChars(codePoint))
