package com.rkawa.kakunote.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rkawa.kakunote.data.CharList
import com.rkawa.kakunote.data.SettingsStore
import com.rkawa.kakunote.data.SettingsValues
import com.rkawa.kakunote.logic.KanjiData
import java.io.File

/** 画面が共有する設定。変えるとすぐに保存する（小さな JSON を 1 つ書くだけなので、その場で行う）。 */
class AppSettings(private val store: SettingsStore?) {

    var values by mutableStateOf(store?.load() ?: SettingsValues())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val registeredSei: String? get() = values.registeredSei

    fun setSei(sei: String) = update(values.copy(sei = sei))

    fun setReadingPrefix(on: Boolean) = update(values.copy(readingPrefix = on))

    /** 字を足す。足せない字があれば、知らせる文言を返す */
    fun addChars(text: String, list: CharList, data: KanjiData): String? {
        val (next, message) = values.addingChars(text, list, data)
        if (message == null) update(next)
        return message
    }

    fun removeChar(char: String, list: CharList) = update(values.removingChar(char, list))

    private fun update(next: SettingsValues) {
        values = next
        try {
            store?.save(next)
            errorMessage = null
        } catch (_: Exception) {
            errorMessage = "設定を保存できませんでした"
        }
    }

    companion object {
        /** アプリが使う設定（アプリの files の settings.json）。 */
        fun open(context: Context) = AppSettings(SettingsStore(File(context.filesDir, SettingsStore.FILE_NAME)))
    }
}
