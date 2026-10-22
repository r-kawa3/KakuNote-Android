package com.rkawa.kakunote.data

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** お気に入りの保存（iOS の FavoriteStoreTests と同じ確かめ方）。 */
class FavoriteStoreTest {

    private lateinit var file: File
    private lateinit var sql: JdbcWritableSql
    private lateinit var store: FavoriteStore

    private val query = SearchQuery(
        sei = "田中",
        positions = listOf(PositionCondition.Fixed("陽"), PositionCondition.Strokes(3, ScriptFilter.ANY), PositionCondition.Free(ScriptFilter.HIRAGANA)),
        conditions = mapOf(KakuKind.SOUKAKU to setOf(24, 31), KakuKind.JINKAKU to setOf(15)),
        avoid = listOf("乙"),
        use = listOf("葵"),
        reading = "はると",
        readingPrefix = true,
    )

    @Before
    fun open() {
        file = File.createTempFile("favorites", ".sqlite")
        file.delete()
        sql = JdbcWritableSql(file.path)
        store = FavoriteStore(sql)
    }

    @After
    fun close() {
        sql.close()
        file.delete()
    }

    @Test
    fun addListsNewestFirst() {
        assertTrue(store.add(null, "陽菜", null, createdAt = 1_000))
        assertTrue(store.add("田中", "健太", query, createdAt = 2_000))
        val all = store.all()
        assertEquals(listOf("健太", "陽菜"), all.map { it.mei })
        assertEquals("田中", all[0].sei)
        assertNull(all[1].sei)
        assertEquals(query, all[0].query)
        assertNull(all[1].query)
        assertEquals(2_000L, all[0].createdAt)
    }

    @Test
    fun sameSeiAndMeiOnlyOnce() {
        assertTrue(store.add("田中", "健太", null))
        assertFalse(store.add("田中", "健太", query))
        // 姓が違えば別
        assertTrue(store.add(null, "健太", null))
        assertTrue(store.add("佐藤", "健太", null))
        assertEquals(3, store.all().size)
    }

    @Test
    fun canonicallyEquivalentCharsAreDistinct() {
        assertTrue(store.add(null, "海", null))
        assertTrue(store.add(null, "海", null))
        assertEquals(2, store.all().size)
    }

    @Test
    fun remove() {
        store.add("田中", "健太", null)
        store.add(null, "健太", null)
        store.remove(null, "健太")
        assertEquals(listOf("田中"), store.all().map { it.sei })
    }

    @Test
    fun keepsAfterReopen() {
        store.add("田中", "健太", query)
        sql.close()
        sql = JdbcWritableSql(file.path)
        val reopened = FavoriteStore(sql)
        assertEquals(query, reopened.all().single().query)
        var version = 0
        sql.query("PRAGMA user_version") { version = (it[0] as Number).toInt() }
        assertEquals(FavoriteStore.SCHEMA_VERSION, version)
    }

    @Test
    fun codecReadsOldJsonWithoutNewFields() {
        val old = """{"sei":null,"positions":[{"kind":"free","script":"kanji"}],"conditions":{"chikaku":[2]}}"""
        val decoded = SearchQueryCodec.decodeOrNull(old)!!
        assertEquals(emptyList<String>(), decoded.avoid)
        assertNull(decoded.reading)
        assertFalse(decoded.readingPrefix)
        assertEquals(mapOf(KakuKind.CHIKAKU to setOf(2)), decoded.conditions)
        assertNull(SearchQueryCodec.decodeOrNull("こわれた"))
    }
}
