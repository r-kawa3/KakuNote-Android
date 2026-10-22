package com.rkawa.kakunote.ui.lookup

import com.rkawa.kakunote.logic.InputError
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.LookupError
import com.rkawa.kakunote.logic.LookupResult
import com.rkawa.kakunote.logic.NameLookup
import com.rkawa.kakunote.logic.NamePart
import com.rkawa.kakunote.logic.NameWarning

/** 「調べる」の画面に出すもの。 */
sealed class LookupState {
    /** 姓も名も空 */
    data object Empty : LookupState()

    data class Shown(val result: LookupResult) : LookupState()

    data class Failed(val message: String) : LookupState()

    companion object {
        /** 姓・名の入力から、画面に出すものを決める。計算は軽いので、入力のたびにその場で行う。 */
        fun of(sei: String, mei: String, data: KanjiData, school: KakuSchool): LookupState {
            if (sei.isBlank() && mei.isBlank()) return Empty
            return try {
                Shown(NameLookup.lookup(sei, mei, data, school))
            } catch (error: LookupError) {
                Failed(LookupMessages.message(error))
            }
        }
    }
}

/** 「調べる」の文言。iOS の LookupPresenter・LookupResultView と同じ。 */
object LookupMessages {

    fun message(error: LookupError): String = when (error) {
        is LookupError.UnknownStrokes ->
            "画数がわからない字があります: " + error.chars.joinToString("、") { "「$it」" }
        is LookupError.InvalidInput -> {
            val part = if (error.part == NamePart.SEI) "姓" else "名"
            when (error.error) {
                InputError.SpaceInside -> "${part}の途中に空白があります"
                InputError.DanglingSoundMark, is InputError.InvalidSoundMark -> "${part}の濁点・半濁点の付け方が正しくありません"
                InputError.Empty -> "${part}を入力してください"
            }
        }
    }

    fun message(warning: NameWarning): String = when (warning) {
        is NameWarning.NotNameChar -> "「${warning.char}」（名の${warning.position}文字目）は名前には使えません"
        is NameWarning.NotInitial -> "「${warning.char}」は名の1文字目には使えません"
    }

    /** 外格の式の説明。一字姓・一字名のときは、外格にその字の画数を足す（新字体式） */
    fun gaikakuNote(seiCount: Int, meiCount: Int): String = when {
        seiCount == 1 && meiCount == 1 -> "総格−人格＋姓と名の字"
        seiCount == 1 -> "総格−人格＋姓の字"
        meiCount == 1 -> "総格−人格＋名の字"
        else -> "総格−人格"
    }

    /** 別の数え方（例: 「別 10画・11画」） */
    fun altStrokes(strokes: List<Int>): String = "別 " + strokes.joinToString("・") { "${it}画" }
}
