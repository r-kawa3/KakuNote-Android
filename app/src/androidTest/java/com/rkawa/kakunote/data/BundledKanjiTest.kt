package com.rkawa.kakunote.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 端末で、同梱の漢字 DB をコピーして読み込めることを確かめる。 */
@RunWith(AndroidJUnit4::class)
class BundledKanjiTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun readsBundledDataMatchingTestCaseVersion() {
        val data = BundledKanji.read(context)
        // 共通テストケースは、テストの APK の assets に入れている（app/build.gradle.kts）
        val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
        val search = JSONObject(testAssets.open("search.json").bufferedReader().readText())
        assertEquals(
            search.getJSONObject("data_version").getString("kanjidic2_database_version"),
            data.meta["kanjidic2_database_version"],
        )
        assertEquals(9, data.info("海")?.strokes)
        // 互換漢字の 海（U+FA45）は、正準等価でも別の字
        assertEquals(10, data.info("海")?.strokes)
        assertEquals(3, data.info("々")?.strokes)
        assertEquals(false, data.readings.isEmpty())
    }

    @Test
    fun copiesAgainWhenStampDiffers() {
        val file = BundledKanji.copiedDatabase(context)
        // 古い版のコピーが残っている状態を作る（中身を壊し、印を別の時刻にする）
        file.writeText("古いデータ")
        File(file.parentFile, "${file.name}.stamp").writeText("0")
        BundledKanji.copiedDatabase(context)
        assertEquals(9, BundledKanji.read(context).info("海")?.strokes)
    }

    @Test
    fun keepsCopyWhenStampMatches() {
        val file = BundledKanji.copiedDatabase(context)
        val modified = file.lastModified()
        assertEquals(file, BundledKanji.copiedDatabase(context))
        assertEquals(modified, file.lastModified())
    }
}
