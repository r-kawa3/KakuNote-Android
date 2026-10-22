package com.rkawa.kakunote.data

import com.rkawa.kakunote.logic.InputError
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.TextPreprocessor
import com.rkawa.kakunote.logic.UnknownStrokeError
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 設定で登録する字の並び。 */
enum class CharList {
    /** 避けたい字: すべての検索で、自動の候補から外す */
    AVOID,

    /** 使いたい字: すべての検索で、どれか 1 つ以上が名に入っている候補だけを出す */
    USE,
}

/** 設定画面で決める値（iOS の SettingsValues と同じ項目）。 */
data class SettingsValues(
    /** 登録した姓（入力のまま）。空なら登録なし */
    val sei: String = "",
    /** 避けたい字（登録した順） */
    val avoidChars: List<String> = emptyList(),
    /** 使いたい字（登録した順） */
    val useChars: List<String> = emptyList(),
    /** 読みの頭の部分も使う（初期値はオフ） */
    val readingPrefix: Boolean = false,
) {
    /** 登録した姓。前後の空白を除いて空なら null */
    val registeredSei: String? get() = sei.trim().ifEmpty { null }

    fun chars(list: CharList): List<String> = if (list == CharList.AVOID) avoidChars else useChars

    /**
     * 入力した字（何文字でもよい）を、避けたい字・使いたい字に足したもの。
     * 足せない字があれば、知らせる文言を返して何も足さない。使いたい字は、名前に使える字だけを足せる。
     * 同じ字は 2 度足さない（Kotlin の String は正準等価な字を別の字として比べる）。
     */
    fun addingChars(text: String, list: CharList, data: KanjiData): Pair<SettingsValues, String?> {
        val chars = try {
            TextPreprocessor.characters(text)
        } catch (_: InputError) {
            return this to "字だけを入れてください（途中の空白などは使えません）"
        }
        val unknown = chars.filter { data.info(it) == null }
        if (unknown.isNotEmpty()) {
            return this to unknown.joinToString("、") { "「$it」" } + "は登録できません。漢字・ひらがな・カタカナを入れてください"
        }
        if (list == CharList.USE) {
            val notName = chars.filter { data.info(it)?.nameOK != true }
            if (notName.isNotEmpty()) return this to notName.joinToString("、") { "「$it」" } + "は名前には使えない字です"
        }
        val added = (chars(list) + chars).distinct()
        return with(list, added) to null
    }

    fun removingChar(char: String, list: CharList): SettingsValues = with(list, chars(list) - char)

    private fun with(list: CharList, chars: List<String>) =
        if (list == CharList.AVOID) copy(avoidChars = chars) else copy(useChars = chars)
}

/** 設定のファイル（JSON）の読み書き。同梱の漢字 DB・お気に入りとは別のファイル。 */
class SettingsStore(private val file: File) {

    /** 保存した設定。ファイルがない・読めないときは初期値。項目を足しても古いファイルを読めるように、ない項目は初期値にする */
    fun load(): SettingsValues = try {
        val json = JSONObject(file.readText())
        fun strings(name: String) = json.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
        SettingsValues(
            sei = json.optString("sei", ""),
            avoidChars = strings("avoidChars"),
            useChars = strings("useChars"),
            readingPrefix = json.optBoolean("readingPrefix", false),
        )
    } catch (_: Exception) {
        SettingsValues()
    }

    /** 保存する。途中で止まっても壊れたファイルが残らないように、別名に書いてから置き換える */
    fun save(values: SettingsValues) {
        val json = JSONObject()
            .put("sei", values.sei)
            .put("avoidChars", JSONArray(values.avoidChars))
            .put("useChars", JSONArray(values.useChars))
            .put("readingPrefix", values.readingPrefix)
        file.parentFile?.mkdirs()
        val temporary = File(file.path + ".tmp")
        temporary.writeText(json.toString())
        if (!temporary.renameTo(file)) {
            temporary.delete()
            error("設定を保存できませんでした")
        }
    }

    companion object {
        const val FILE_NAME = "settings.json"
    }
}

/** 姓の天格。画数がわからない字などがあれば、表示する文言。 */
sealed class SeiCheck {
    data class Tenkaku(val value: Int) : SeiCheck()
    data class Problem(val message: String) : SeiCheck()

    companion object {
        fun of(sei: String, data: KanjiData): SeiCheck = try {
            Tenkaku(data.strokes(TextPreprocessor.characters(sei)).sum())
        } catch (error: UnknownStrokeError) {
            Problem("画数がわからない字があります: " + error.chars.joinToString("、") { "「$it」" })
        } catch (_: InputError) {
            Problem("姓に使えない文字（途中の空白など）があります")
        }
    }
}

/** 漢字データの出典とライセンス（「このアプリについて」に出す。文言は iOS の Attribution と同じ）。
 *
 * KANJIDIC2 のライセンスでは、メニューから開ける別の画面に、使っていることと出典を表示し、
 * ライセンスへのリンクを示すことが求められている。Unihan は同梱する DB に入れていないので、表示しない。
 * アプリを公開する前に、各配布元の最新の条件を確かめ直すこと。
 */
object Attribution {

    data class Link(val title: String, val url: String)

    data class Source(
        val name: String,
        val description: String,
        /** 同梱 DB に記録されているデータのバージョン */
        val version: String,
        val notice: String,
        val links: List<Link>,
    )

    fun sources(meta: Map<String, String>): List<Source> = listOf(
        Source(
            name = "KANJIDIC2",
            description = "漢字の画数・読み・常用漢字と人名用漢字の区別に、Electronic Dictionary Research and Development Group（EDRDG）の KANJIDIC2 を使っています。",
            version = meta["kanjidic2_database_version"] ?: "不明",
            notice = "KANJIDIC2 は EDRDG の財産で、同グループのライセンスに従って使っています。このアプリの漢字データは KANJIDIC2 をもとに作ったもので、同じ CC BY-SA 4.0 のもとで提供します。",
            links = listOf(
                Link("KANJIDIC プロジェクト", "https://www.edrdg.org/wiki/index.php/KANJIDIC_Project"),
                Link("EDRDG のライセンス", "https://www.edrdg.org/edrdg/licence.html"),
                Link("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"),
            ),
        ),
    )
}
