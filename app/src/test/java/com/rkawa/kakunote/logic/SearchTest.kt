package com.rkawa.kakunote.logic

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 共通テストケース（testcases/search.json）で、名前の候補の検索を確かめる。 */
class SearchTest {

    private val data = TestData.kanji
    private val school: KakuSchool = ShinjitaiSchool

    /** 姓なしのケース（エラーのケースを含む）。 */
    @Test
    fun commonNameOnlyCases() {
        val cases = searchCases().filter { it.getJSONObject("input").isNull("sei") }
        assertFalse(cases.isEmpty())
        cases.forEach(::check)
    }

    /** 姓ありのケース（エラーのケースを含む）。 */
    @Test
    fun commonSeiCases() {
        val cases = searchCases().filter { !it.getJSONObject("input").isNull("sei") }
        assertFalse(cases.isEmpty())
        cases.forEach(::check)
    }

    @Test
    fun oneCharSeiGaikakuUsesSingleCharRule() {
        // 姓「林」（8 画）・1 文字・外格 20 → 外格 = 0 + 8 + n なので 12 画の字
        val search = search(SearchQuery("林", listOf(free(ScriptFilter.KANJI)), mapOf(KakuKind.GAIKAKU to setOf(20))))
        assertEquals(listOf(listOf(12)), search.breakdowns.map { it.strokes })
        assertTrue("翔" in search.page(0, search.total.toInt()))
    }

    @Test
    fun twoCharsOrder() {
        val search = search(SearchQuery(null, List(2) { free(ScriptFilter.KANJI) }, mapOf(KakuKind.CHIKAKU to setOf(2))))
        assertEquals(4L, search.total)
        assertEquals(listOf("一一", "一乙", "乙一", "乙乙"), search.page(0, 10))
        assertEquals(listOf("乙乙"), search.page(3, 10))
        assertEquals(emptyList<String>(), search.page(4, 10))
    }

    @Test
    fun pagesMatchFullListing() {
        val search = search(
            SearchQuery(null, listOf(free(ScriptFilter.KANJI), free(ScriptFilter.HIRAGANA)), mapOf(KakuKind.CHIKAKU to setOf(14))),
        )
        val everything = search.page(0, search.total.toInt())
        assertEquals(search.total, everything.size.toLong())
        assertEquals(search.total, everything.toSet().size.toLong())
        for (offset in listOf(0, 1, 37, everything.size - 3)) {
            assertEquals(everything.subList(offset, minOf(offset + 5, everything.size)), search.page(offset.toLong(), 5))
        }
    }

    @Test
    fun largeSearchIsLazy() {
        val start = System.nanoTime()
        val search = search(SearchQuery(null, List(4) { free(ScriptFilter.KANJI) }, mapOf(KakuKind.CHIKAKU to setOf(8))))
        assertEquals(50, search.page(0, 50).size)
        assertEquals(1, search.page(search.total - 1, 50).size)
        assertTrue(seconds(start) < 2.0)
    }

    @Test
    fun sixCharsLargeSearchIsFast() {
        // 打ち切りがないと、約 3 億通りの内訳をたどってしまう
        val start = System.nanoTime()
        val search = search(SearchQuery(null, List(6) { free(ScriptFilter.KANJI) }, mapOf(KakuKind.CHIKAKU to setOf(30))))
        assertTrue(seconds(start) < 3.0)
        assertEquals(listOf("一一一一一廳"), search.page(0, 1))
    }

    @Test
    fun tooManyResultsStopsEarly() {
        // 総画数 6〜200 のどれでもよい 6 文字（種類は any）は、総数が上限を超える
        val start = System.nanoTime()
        assertSearchError(
            SearchError.TooManyResults,
            SearchQuery(null, List(6) { free(ScriptFilter.ANY) }, mapOf(KakuKind.CHIKAKU to (6..200).toSet())),
        )
        assertTrue(seconds(start) < 2.0)
    }

    @Test
    fun lengthLimits() {
        for (length in listOf(0, 7)) {
            assertSearchError(
                SearchError.InvalidLength,
                SearchQuery(null, List(length) { free(ScriptFilter.KANJI) }, mapOf(KakuKind.CHIKAKU to setOf(7))),
            )
        }
    }

    @Test
    fun fixedCharMustBeSingle() {
        assertSearchError(
            SearchError.FixedNotSingleChar(1),
            SearchQuery(null, listOf(PositionCondition.Fixed("陽菜")), mapOf(KakuKind.CHIKAKU to setOf(23))),
        )
    }

    /** 避けたい字は、コードポイントの並びで比べる（正準等価な字は別の字）。 */
    @Test
    fun avoidComparesCodePoints() {
        val positions = listOf(free(ScriptFilter.KANJI))
        val conditions = mapOf(KakuKind.CHIKAKU to setOf(9))
        val plain = search(SearchQuery(null, positions, conditions))
        val compat = search(SearchQuery(null, positions, conditions, avoid = listOf("海")))
        val names = compat.page(0, compat.total.toInt())
        assertTrue("海" in names)
        assertFalse("海" in names)
        val plainNames = plain.page(0, plain.total.toInt())
        assertEquals(plain.total - (if ("海" in plainNames) 1 else 0), compat.total)
    }

    /** 使いたい字は、全部の候補から「使いたい字を含まないもの」を飛ばしたのと同じ。数・順番・ページ送りが合う。 */
    @Test
    fun useMatchesFilteredListing() {
        val positions = listOf(free(ScriptFilter.KANJI), free(ScriptFilter.HIRAGANA), free(ScriptFilter.KANJI))
        val conditions = mapOf(KakuKind.CHIKAKU to setOf(12))
        val plain = search(SearchQuery(null, positions, conditions))
        val use = listOf("一", "ぬ", "乃")
        val searched = search(SearchQuery(null, positions, conditions, avoid = listOf("乃"), use = use))
        val expected = plain.page(0, plain.total.toInt()).filter { name -> use.any { it in name } }
        assertEquals(expected.size.toLong(), searched.total)
        assertEquals(expected, searched.page(0, searched.total.toInt()))
        for (offset in listOf(0, 7, expected.size - 4)) {
            assertEquals(expected.subList(offset, minOf(offset + 6, expected.size)), searched.page(offset.toLong(), 6))
        }
        assertTrue(searched.breakdowns.all { it.count > 0 })
    }

    /**
     * 読みの絞り込みは、全部の候補から「読みが割り振れないもの」を飛ばしたのと同じ。数・順番・ページ送りが合う。
     * 割り振りは、ここでは素直な総当たり（仕様書 search.md「割り振り」）で確かめる。
     */
    @Test
    fun readingMatchesBruteForce() {
        for ((positions, reading, use) in listOf(
            Triple(listOf(free(ScriptFilter.KANJI), free(ScriptFilter.HIRAGANA)), "あいか", emptyList()),
            Triple(listOf(free(ScriptFilter.KANJI), free(ScriptFilter.KANJI)), "ゆうと", listOf("優")),
            Triple(listOf(PositionCondition.Fixed("人"), free(ScriptFilter.KANJI)), "ひとびと", emptyList()),
        )) {
            val conditions = mapOf(KakuKind.CHIKAKU to (2..40).toSet())
            val plain = search(SearchQuery(null, positions, conditions))
            val searched = search(SearchQuery(null, positions, conditions, use = use, reading = reading))
            val target = codePoints(reading)
            // 全部の候補は 900 万件ほどあるので、少しずつ取って絞る
            val expected = (0 until plain.total step 100_000).flatMap { offset ->
                plain.page(offset, 100_000).filter { name ->
                    (use.isEmpty() || use.any { it in name }) && canRead(name, target)
                }
            }
            assertTrue(reading, expected.isNotEmpty())
            assertEquals(reading, expected.size.toLong(), searched.total)
            assertEquals(reading, expected, searched.page(0, searched.total.toInt()))
            val middle = expected.size / 2
            assertEquals(reading, expected.subList(middle, minOf(middle + 3, expected.size)), searched.page(middle.toLong(), 3))
            assertTrue(reading, searched.breakdowns.all { it.count > 0 })
        }
    }

    /** 総当たりの割り振り: 名の各字に、読みの空でない連続した部分を順に割り振れるか。 */
    private fun canRead(name: String, reading: List<Int>): Boolean {
        val chars = codePoints(name)
        fun readings(i: Int): Set<List<Int>> {
            var j = i
            while (chars[j] == 0x3005) {
                if (j == 0) return emptySet()
                j -= 1
            }
            val c = chars[j]
            val info = data.chars.getValue(c)
            val own = if (info.script == Script.KANJI) {
                (data.readings[c] ?: emptySet()).map(::codePoints).toMutableSet()
            } else {
                mutableSetOf(listOf(Reading.hiragana(c)))
            }
            if (i > 0 && data.chars.getValue(chars[i]).script == Script.KANJI) {
                for (y in own.toList()) {
                    for (v in Reading.rendaku[y[0]] ?: emptyList()) own.add(listOf(v) + y.drop(1))
                }
            }
            return own
        }
        fun go(i: Int, start: Int): Boolean {
            if (i == chars.size) return start == reading.size
            return readings(i).any { y ->
                y.isNotEmpty() && start + y.size <= reading.size &&
                    reading.subList(start, start + y.size) == y && go(i + 1, start + y.size)
            }
        }
        return go(0, 0)
    }

    /** 読みの頭の部分: 小さい字の手前では区切らない。 */
    @Test
    fun readingPrefixes() {
        val shou = Reading.prefixes(codePoints("しょう")).map { p -> buildString { p.forEach { appendCodePoint(it) } } }
        assertEquals(listOf("しょ", "しょう"), shou)
        assertEquals(3, Reading.prefixes(codePoints("こころ")).size)
    }

    /** 読みがあれば、6 文字で格の条件がとても広くても（内訳が約 850 万）、速く数えてページを作れる。 */
    @Test
    fun readingSixCharsWideConditionIsFast() {
        val query = SearchQuery(
            null, List(6) { free(ScriptFilter.ANY) }, mapOf(KakuKind.CHIKAKU to (6..120).toSet()), reading = "はるかなそら",
        )
        val start = System.nanoTime()
        val search = search(query)
        assertEquals(4_191_688_453L, search.total)
        // 真ん中のページは、iOS 版と同じ（最初の 3 件）
        assertEquals(listOf("剝弄坂長粗村", "剝弄坂長粗良", "剝弄坂長組乱"), search.page(search.total / 2, 50).take(3))
        assertEquals(50, search.page(0, 50).size)
        assertEquals(2, search.page(search.total - 2, 50).size)
        assertTrue("${seconds(start)} 秒かかった", seconds(start) < 5.0)
    }

    /** 読みがあるときのページ送りは、内訳をまたいでも続いている（1 件ずつ取ったものと、まとめて取ったものが同じ）。 */
    @Test
    fun readingPagesAcrossBreakdowns() {
        val search = search(
            SearchQuery(
                "田中", listOf(free(ScriptFilter.KANJI), free(ScriptFilter.ANY)),
                mapOf(KakuKind.SOUKAKU to (10..60).toSet()), reading = "はるな",
            ),
        )
        val all = search.page(0, search.total.toInt())
        assertEquals(search.total, all.size.toLong())
        assertEquals(search.total, search.breakdowns.sumOf { it.count })
        for (offset in 0 until all.size step 37) {
            assertEquals(all.subList(offset, minOf(offset + 41, all.size)), search.page(offset.toLong(), 41))
        }
    }

    /** 読みの入力: カタカナはひらがなに直し、空なら絞り込まない。ひらがな以外はエラー。 */
    @Test
    fun readingNormalize() {
        assertEquals(3, Reading.normalize(" ハルト ")?.size)
        assertEquals(Reading.normalize("はると"), Reading.normalize("ハルト"))
        assertEquals(null, Reading.normalize("  "))
        assertEquals(listOf(0x308B, 0x30FC), Reading.normalize("るー"))
        for (text in listOf("haru", "は る", "あ".repeat(31))) {
            try {
                Reading.normalize(text)
                fail("$text がエラーにならない")
            } catch (_: SearchError.InvalidReading) {
            }
        }
    }

    // MARK: - 共通テストケースの読み込み

    private fun searchCases(): List<JSONObject> = SharedTestCases.load("search.json").getJSONArray("cases").objects()

    /** 1 ケースを確かめる。 */
    private fun check(testCase: JSONObject) {
        val id = testCase.optString("id", "?")
        val expected = testCase.getJSONObject("expected")
        val query = query(testCase.getJSONObject("input"))

        if (expected.has("error")) {
            try {
                search(query)
                fail("$id: エラーにならない")
            } catch (error: SearchError) {
                assertEquals("$id: $error", expected.getString("error"), errorType(error))
            }
            return
        }

        val search = search(query)
        assertEquals(id, expected.getLong("total"), search.total)
        assertEquals(id, expected.getInt("breakdown_count"), search.breakdowns.size)
        for (page in expected.getJSONArray("pages").objects()) {
            val offset = page.getLong("offset")
            val names = page.getJSONArray("names").strings()
            assertEquals("$id offset $offset", names.map(::codePoints), search.page(offset, page.getInt("limit")).map(::codePoints))
        }
    }

    private fun query(input: JSONObject): SearchQuery {
        val positions = input.getJSONArray("positions").objects().map { item ->
            val script = ScriptFilter.of(item.optString("script", "kanji")) ?: ScriptFilter.KANJI
            when (item.optString("kind")) {
                "fixed" -> PositionCondition.Fixed(item.optString("char"))
                "strokes" -> PositionCondition.Strokes(item.optInt("strokes"), script)
                else -> PositionCondition.Free(script)
            }
        }
        val conditionsJson = input.getJSONObject("conditions")
        val conditions = conditionsJson.keys().asSequence().associate { key ->
            (KakuKind.of(key) ?: error("格の名前が不正: $key")) to conditionsJson.getJSONArray(key).ints().toSet()
        }
        return SearchQuery(
            sei = if (input.isNull("sei")) null else input.getString("sei"),
            positions = positions,
            conditions = conditions,
            avoid = input.optJSONArray("avoid")?.strings() ?: emptyList(),
            use = input.optJSONArray("use")?.strings() ?: emptyList(),
            reading = if (input.isNull("reading")) null else input.optString("reading"),
            readingPrefix = input.optBoolean("reading_prefix", false),
        )
    }

    private fun errorType(error: SearchError): String = when (error) {
        is SearchError.InvalidInput, is SearchError.FixedNotSingleChar -> "invalid_input"
        is SearchError.UnknownStrokes -> "unknown_stroke"
        SearchError.TooManyResults -> "too_many"
        SearchError.InvalidReading -> "invalid_reading"
        SearchError.InvalidLength, SearchError.NoCondition, is SearchError.InvalidCondition -> "invalid_condition"
    }

    private fun search(query: SearchQuery) = NameSearch(query, data, school)

    private fun assertSearchError(expected: SearchError, query: SearchQuery) {
        try {
            search(query)
            fail("エラーにならない: $expected")
        } catch (error: SearchError) {
            assertEquals(expected, error)
        }
    }

    private fun free(script: ScriptFilter) = PositionCondition.Free(script)

    private fun seconds(start: Long) = (System.nanoTime() - start) / 1e9
}
