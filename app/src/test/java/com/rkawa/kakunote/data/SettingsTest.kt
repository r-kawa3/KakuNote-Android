package com.rkawa.kakunote.data

import com.rkawa.kakunote.logic.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** 設定の保存・字の登録・姓の天格・出典（iOS の SettingsStoreTests・AttributionTests と同じ確かめ方）。 */
class SettingsTest {

    private val data = TestData.kanji

    private fun tempFile() = File.createTempFile("settings", ".json").apply { delete() }

    @Test
    fun saveAndLoad() {
        val file = tempFile()
        val store = SettingsStore(file)
        assertEquals(SettingsValues(), store.load())
        val values = SettingsValues(sei = "田中", avoidChars = listOf("乙"), useChars = listOf("陽"), readingPrefix = true)
        store.save(values)
        assertEquals(values, SettingsStore(file).load())
        file.delete()
    }

    @Test
    fun readsOldFileWithoutNewFields() {
        val file = tempFile()
        file.writeText("""{"sei":"林"}""")
        assertEquals(SettingsValues(sei = "林"), SettingsStore(file).load())
        file.writeText("こわれた")
        assertEquals(SettingsValues(), SettingsStore(file).load())
        file.delete()
    }

    @Test
    fun registeredSeiTrims() {
        assertNull(SettingsValues(sei = "　 ").registeredSei)
        assertEquals("田中", SettingsValues(sei = " 田中 ").registeredSei)
    }

    @Test
    fun addCharsAndRemove() {
        var (values, message) = SettingsValues().addingChars("乙一乙ｱ", CharList.AVOID, data)
        assertNull(message)
        assertEquals(listOf("乙", "一", "ア"), values.avoidChars)
        values = values.removingChar("一", CharList.AVOID)
        assertEquals(listOf("乙", "ア"), values.avoidChars)
    }

    @Test
    fun addCharsKeepsCanonicallyEquivalentCharsApart() {
        val (values, _) = SettingsValues().addingChars("海海", CharList.AVOID, data)
        assertEquals(listOf("海", "海"), values.avoidChars)
    }

    @Test
    fun addCharsRejectsUnknownAndNotNameChars() {
        val start = SettingsValues()
        assertEquals(start to "「A」は登録できません。漢字・ひらがな・カタカナを入れてください", start.addingChars("陽A", CharList.AVOID, data))
        assertEquals(start to "「澤」は名前には使えない字です", start.addingChars("澤", CharList.USE, data))
        assertEquals(start to "字だけを入れてください（途中の空白などは使えません）", start.addingChars("陽 葵", CharList.USE, data))
        // 避けたい字なら、名前に使えない字も登録できる
        assertEquals(listOf("澤"), start.addingChars("澤", CharList.AVOID, data).first.avoidChars)
    }

    @Test
    fun seiTenkaku() {
        assertEquals(SeiCheck.Tenkaku(9), SeiCheck.of("田中", data))
        assertEquals(SeiCheck.Problem("画数がわからない字があります: 「A」"), SeiCheck.of("田A", data))
        assertEquals(SeiCheck.Problem("姓に使えない文字（途中の空白など）があります"), SeiCheck.of("田 中", data))
    }

    @Test
    fun attributionShowsKanjidic2Only() {
        val sources = Attribution.sources(data.meta)
        assertEquals(listOf("KANJIDIC2"), sources.map { it.name })
        assertEquals(data.meta["kanjidic2_database_version"], sources[0].version)
        assertEquals("不明", Attribution.sources(emptyMap())[0].version)
        assertEquals(true, sources[0].links.any { it.url.contains("creativecommons.org/licenses/by-sa/4.0") })
    }
}
