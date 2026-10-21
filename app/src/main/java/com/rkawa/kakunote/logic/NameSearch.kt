package com.rkawa.kakunote.logic

/** 位置ごとの文字の種類。raw は共通テストケースやお気に入りの JSON での名前。 */
enum class ScriptFilter(val raw: String) {
    KANJI("kanji"),
    HIRAGANA("hiragana"),
    KATAKANA("katakana"),
    ANY("any");

    fun accepts(script: Script): Boolean = when (this) {
        ANY -> true
        KANJI -> script == Script.KANJI
        HIRAGANA -> script == Script.HIRAGANA
        KATAKANA -> script == Script.KATAKANA
    }

    companion object {
        fun of(raw: String): ScriptFilter? = entries.firstOrNull { it.raw == raw }
    }
}

/** 名の 1 つの位置の条件。 */
sealed class PositionCondition {
    /** この字にする */
    data class Fixed(val char: String) : PositionCondition()

    /** n 画の字にする */
    data class Strokes(val strokes: Int, val script: ScriptFilter) : PositionCondition()

    /** 何画でもよい */
    data class Free(val script: ScriptFilter) : PositionCondition()
}

/** 検索の条件（仕様書 search.md「入力」）。 */
data class SearchQuery(
    /** 姓。null なら姓なし（名前のみ検索） */
    val sei: String?,
    /** 名の位置 1〜L の条件（L は 1〜6） */
    val positions: List<PositionCondition>,
    /** 格ごとに許す値の集合。すべてを満たす名だけを候補にする */
    val conditions: Map<KakuKind, Set<Int>>,
    /** 避けたい字。固定でない位置の候補から外す（仕様書 search.md「避けたい字」） */
    val avoid: List<String> = emptyList(),
    /**
     * 使いたい字。どれか 1 つ以上が名のどこかに入っている候補だけを残す（仕様書 search.md「使いたい字」）。
     * 避けたい字にも入っていたら、使いたい字を優先する
     */
    val use: List<String> = emptyList(),
    /** 名前全体の読み（ひらがな。カタカナはひらがなに直して比べる）。null や空なら絞り込まない（仕様書 search.md「読み」） */
    val reading: String? = null,
    /** 読みの頭の部分も使う（例: 心「こころ」を「こ」とも読む。仕様書 search.md「読みの頭の部分」） */
    val readingPrefix: Boolean = false,
)

sealed class SearchError : Exception() {
    /** 文字数が 1〜6 でない */
    data object InvalidLength : SearchError() { private fun readResolve(): Any = InvalidLength }

    /** 格の条件がない、または値が空 */
    data object NoCondition : SearchError() { private fun readResolve(): Any = NoCondition }

    /** 条件にできない格（天格、姓なしの人格・総格・外格） */
    data class InvalidCondition(val kind: KakuKind) : SearchError()

    /** 姓や固定の字の入力文字列が前処理の規則に合わない */
    data class InvalidInput(val error: InputError) : SearchError()

    /** 固定の字が 1 文字でない（position は 1 から数える） */
    data class FixedNotSingleChar(val position: Int) : SearchError()

    /** 画数がわからない字がある */
    data class UnknownStrokes(val chars: List<String>) : SearchError()

    /** 候補の総数が NameSearch.MAX_TOTAL を超える（条件が広すぎる） */
    data object TooManyResults : SearchError() { private fun readResolve(): Any = TooManyResults }

    /** 読みが、ひらがな（カタカナ・「ー」）1〜30 文字でない */
    data object InvalidReading : SearchError() { private fun readResolve(): Any = InvalidReading }
}

/** 名前の候補の検索（仕様書 search.md）。候補は全部を作らず、page で必要な範囲だけを作る。 */
class NameSearch(query: SearchQuery, data: KanjiData, school: KakuSchool) {

    /** 条件を満たす内訳と、その内訳の候補の数。 */
    data class Breakdown(val strokes: List<Int>, val count: Long)

    /** 候補 1 件: 名と、その各字の画数（内訳）。 */
    data class Candidate(val name: String, val strokes: List<Int>)

    val total: Long

    /** 名の文字数 */
    val length: Int

    /** 姓の各字の画数。姓なしなら null */
    val seiStrokes: List<Int>?

    /**
     * 条件を満たす内訳の画数を 1 本の配列に並べたもの（内訳ごとに length 個ずつ）。
     * 内訳は 10 万を超えることがあるので、内訳ごとに配列を作らない
     */
    private val breakdownStrokes: IntArray

    /** 内訳ごとの候補の数 */
    private val breakdownCounts: LongArray

    /** 読みがあるときの数え方（内訳を全部持たずに数える）。null なら内訳を breakdownStrokes に持つ */
    private val readingSearch: ReadingSearch?

    /** 位置ごと: 画数 → その画数の候補の字（仕様の順番） */
    private val candidates: List<Map<Int, List<String>>>

    /** 使いたい字。空なら条件なし */
    private val used: Set<String>

    /** 読みでの絞り込みの表。null なら読みの条件なし */
    private val readingTable: ReadingTable?

    /** 位置ごと: 画数 → candidates と同じ順の、字の読みのキー（ReadingTable.key） */
    private val readingKeys: List<Map<Int, List<Int>>>

    /** 条件を満たす内訳（辞書式の小さい順）。テストや表示で使う */
    val breakdowns: List<Breakdown>
        get() = readingSearch?.allBreakdowns()
            ?: breakdownCounts.indices.map { i ->
                Breakdown(breakdownStrokes.copyOfRange(i * length, (i + 1) * length).toList(), breakdownCounts[i])
            }

    init {
        if (query.positions.size !in 1..MAX_LENGTH) throw SearchError.InvalidLength
        if (query.conditions.isEmpty() || query.conditions.values.any { it.isEmpty() }) throw SearchError.NoCondition
        val allowed = if (query.sei == null) {
            setOf(KakuKind.CHIKAKU)
        } else {
            setOf(KakuKind.JINKAKU, KakuKind.CHIKAKU, KakuKind.SOUKAKU, KakuKind.GAIKAKU)
        }
        KakuKind.entries.firstOrNull { query.conditions[it] != null && it !in allowed }?.let {
            throw SearchError.InvalidCondition(it)
        }

        val seiStrokes = query.sei?.let { strokes(characters(it), data) }

        // 避けたい字・使いたい字。前処理してから比べる（Kotlin の String はコードポイントの並びで比べる）。
        // 両方に入っていたら使いたい字を優先する
        val used = query.use.flatMap { characters(it) }.toSet()
        val avoided = query.avoid.flatMap { characters(it) }.toSet() - used
        fun autoCandidates(script: ScriptFilter, initial: Boolean): List<CharInfo> {
            val chars = data.autoCandidates(script, initial)
            return if (avoided.isEmpty()) chars else chars.filter { it.char !in avoided }
        }

        // 読み。どの部分も担えない字は、はじめから候補から外す（結果は変わらない）
        val readingTable = query.reading?.let { Reading.normalize(it) }?.let {
            ReadingTable(it, usesPrefixes = query.readingPrefix)
        }

        // 位置ごとの候補の字
        val candidates = mutableListOf<Map<Int, List<String>>>()
        val readingKeys = mutableListOf<Map<Int, List<Int>>>()
        query.positions.forEachIndexed { index, position ->
            var infos: List<CharInfo> = when (position) {
                is PositionCondition.Fixed -> {
                    val chars = characters(position.char)
                    if (chars.size != 1) throw SearchError.FixedNotSingleChar(index + 1)
                    strokes(chars, data)
                    listOf(data.info(chars[0])!!)
                }
                is PositionCondition.Strokes ->
                    autoCandidates(position.script, initial = index == 0).filter { it.strokes == position.strokes }
                is PositionCondition.Free -> autoCandidates(position.script, initial = index == 0)
            }
            val keys = mutableListOf<Int>()
            if (readingTable != null) {
                infos = infos.filter { info ->
                    val key = readingTable.key(info, index, data) ?: return@filter false
                    keys.add(key)
                    true
                }
            }
            val group = LinkedHashMap<Int, MutableList<String>>()
            val keyGroup = LinkedHashMap<Int, MutableList<Int>>()
            infos.forEachIndexed { k, info ->
                group.getOrPut(info.strokes) { mutableListOf() }.add(info.char)
                if (readingTable != null) keyGroup.getOrPut(info.strokes) { mutableListOf() }.add(keys[k])
            }
            candidates.add(group)
            readingKeys.add(keyGroup)
        }
        this.candidates = candidates
        this.used = used
        this.readingTable = readingTable
        this.readingKeys = readingKeys

        // 条件を満たす内訳を、辞書式の小さい順に求める。
        // 条件を満たせないとわかった途中の組み合わせは打ち切る（仕様書 search.md「内訳の列挙の打ち切り」）
        val strokeValues = candidates.map { it.keys.sorted().toIntArray() }
        // 位置ごと・画数ごとの、候補の字の数と、そのうち使いたい字の数
        val counts = candidates.indices.map { i -> strokeValues[i].map { candidates[i][it]!!.size.toLong() } }
        val usedCounts = candidates.indices.map { i ->
            strokeValues[i].map { v -> if (used.isEmpty()) 0L else candidates[i][v]!!.count { it in used }.toLong() }
        }
        val length = strokeValues.size
        val breakdownStrokes = IntArrayBuilder()
        val breakdownCounts = LongArrayBuilder()
        var total = 0L
        var readingSearch: ReadingSearch? = null
        if (strokeValues.all { it.isNotEmpty() }) {
            val bounds = ConditionBounds(
                kakuValues = { kakuValues(it, seiStrokes, school) },
                strokeValues = strokeValues,
                conditions = query.conditions,
            )
            // 深さごとの「途中までの格の値の増え方」。全部の位置を決めると、各格の値そのものになる
            val partials = Array(length + 1) { IntArray(bounds.itemCount) }
            val current = IntArray(length)

            // 内訳の途中までの「全部の組み合わせの数」と「使いたい字を 1 つも含まない組み合わせの数」。
            // 画数ごとの字の数は多くても 400 ほどなので、6 文字でも 64 ビットに収まる（あふれたら例外にする）
            fun visit(index: Int, full: Long, without: Long) {
                if (index == length) {
                    val count = if (used.isEmpty()) full else full - without
                    // 使いたい字を含む候補がない内訳は残さない
                    if (count <= 0) return
                    // 内訳 1 つの件数だけで上限を超える場合も、ここで止める
                    if (count > MAX_TOTAL) throw SearchError.TooManyResults
                    breakdownStrokes.addAll(current)
                    breakdownCounts.add(count)
                    // 上限を超えたら、残りをたどらずにすぐやめる
                    total += count
                    if (total > MAX_TOTAL) throw SearchError.TooManyResults
                    return
                }
                val partial = partials[index]
                for ((k, value) in strokeValues[index].withIndex()) {
                    if (!bounds.extend(partial, index, value, partials[index + 1])) continue
                    current[index] = value
                    visit(
                        index + 1,
                        Math.multiplyExact(full, counts[index][k]),
                        Math.multiplyExact(without, counts[index][k] - usedCounts[index][k]),
                    )
                }
            }
            if (readingTable != null) {
                // 読みがあるときは、内訳が数百万になることがあるので、内訳を持たずに数える
                val search = ReadingSearch(
                    table = readingTable, bounds = bounds, strokeValues = strokeValues,
                    classes = (0 until length).map { i ->
                        strokeValues[i].map { value ->
                            ReadingClass.group(
                                keys = readingKeys[i][value]!!,
                                used = candidates[i][value]!!.map { it in used },
                            )
                        }
                    },
                    keepsBase = (0 until length).map { i ->
                        readingKeys.drop(i + 1).any { group -> group.values.any { ReadingTable.ODORIJI in it } }
                    },
                    needsUse = used.isNotEmpty(),
                )
                if (search.total > MAX_TOTAL) throw SearchError.TooManyResults
                total = search.total
                readingSearch = search
            } else {
                visit(0, 1, 1)
            }
        }
        this.length = length
        this.seiStrokes = seiStrokes
        this.breakdownStrokes = breakdownStrokes.build()
        this.breakdownCounts = breakdownCounts.build()
        this.readingSearch = readingSearch
        this.total = total
    }

    /** 先頭から offset 件目（0 始まり）から、最大 limit 件の候補を返す。 */
    fun page(offset: Long, limit: Int): List<String> = candidates(offset, limit).map { it.name }

    /** page と同じ範囲の候補を、各字の画数と一緒に返す。 */
    fun candidates(offset: Long, limit: Int): List<Candidate> {
        if (offset < 0 || limit <= 0) return emptyList()
        val result = mutableListOf<Candidate>()
        if (readingSearch != null && readingTable != null) {
            // 読みがあるとき: その件数目の内訳と、内訳の中の番号を直接求め、内訳の中は順に作る
            var index = offset
            while (result.size < limit && index < total) {
                val (strokes, inner, count) = readingSearch.locate(index)
                val lists = (0 until length).map { candidates[it][strokes[it]]!! }
                val counter = ReadingCounter(
                    keys = strokes.indices.map { readingKeys[it][strokes[it]]!! },
                    used = lists.map { list -> list.map { it in used } },
                    table = readingTable,
                    needsUse = used.isNotEmpty(),
                )
                var j = inner
                while (j < count && result.size < limit) {
                    val picks = counter.nth(j)
                    result.add(Candidate(lists.indices.joinToString("") { lists[it][picks[it]] }, strokes))
                    j += 1
                    index += 1
                }
            }
            return result
        }
        var index = offset
        for (i in breakdownCounts.indices) {
            val count = breakdownCounts[i]
            if (index >= count) {
                index -= count
                continue
            }
            val strokes = breakdownStrokes.copyOfRange(i * length, (i + 1) * length).toList()
            val lists = (0 until length).map { candidates[it][strokes[it]]!! }
            val flags = if (used.isEmpty()) emptyList() else lists.map { list -> list.map { it in used } }
            while (index < count && result.size < limit) {
                val name = if (used.isEmpty()) combination(lists, index) else combination(lists, flags, index)
                result.add(Candidate(name, strokes))
                index += 1
            }
            if (result.size >= limit) break
            index = 0
        }
        return result
    }

    companion object {
        /** 名の文字数の上限 */
        const val MAX_LENGTH = 6

        /** 候補の総数の上限（2^53 - 1）。JSON や多くの言語で整数を正確に扱える範囲に合わせる */
        const val MAX_TOTAL = 9_007_199_254_740_991L

        /** 名の各位置の画数から、条件にできる格の値を求める。姓なしなら地格（総画数）だけ。 */
        private fun kakuValues(mei: List<Int>, sei: List<Int>?, school: KakuSchool): Map<KakuKind, Int> {
            if (sei == null) return mapOf(KakuKind.CHIKAKU to school.nameOnlyTotal(mei))
            val kaku = school.kaku(sei, mei)
            return KakuKind.entries.associateWith { kaku.value(it) }
        }

        /** 位置 1 がいちばんゆっくり変わる順番で、n 番目（0 始まり）の組み合わせ。 */
        private fun combination(lists: List<List<String>>, at: Long): String {
            var n = at
            val chars = Array(lists.size) { "" }
            for (i in lists.indices.reversed()) {
                chars[i] = lists[i][(n % lists[i].size).toInt()]
                n /= lists[i].size
            }
            return chars.joinToString("")
        }

        /**
         * 使いたい字の条件を満たす組み合わせのうち、n 番目（0 始まり）。
         * 位置 1 から順に、「その字を選んだとき、残りの位置で条件を満たす組み合わせの数」を数えて字を決める。
         */
        private fun combination(lists: List<List<String>>, usedFlags: List<List<Boolean>>, at: Long): String {
            // suffixFull[i]・suffixWithout[i]: 位置 i 以降の全部の組み合わせの数と、使いたい字を含まない組み合わせの数
            val length = lists.size
            val suffixFull = LongArray(length + 1) { 1 }
            val suffixWithout = LongArray(length + 1) { 1 }
            for (i in (0 until length).reversed()) {
                suffixFull[i] = suffixFull[i + 1] * lists[i].size
                suffixWithout[i] = suffixWithout[i + 1] * usedFlags[i].count { !it }
            }
            var n = at
            var satisfied = false
            val chars = StringBuilder()
            for (i in 0 until length) {
                for ((k, char) in lists[i].withIndex()) {
                    val now = satisfied || usedFlags[i][k]
                    val rest = if (now) suffixFull[i + 1] else suffixFull[i + 1] - suffixWithout[i + 1]
                    if (n < rest) {
                        chars.append(char)
                        satisfied = now
                        break
                    }
                    n -= rest
                }
            }
            return chars.toString()
        }

        private fun characters(text: String): List<String> =
            try {
                TextPreprocessor.characters(text)
            } catch (error: InputError) {
                throw SearchError.InvalidInput(error)
            }

        private fun strokes(chars: List<String>, data: KanjiData): List<Int> =
            try {
                data.strokes(chars)
            } catch (error: UnknownStrokeError) {
                throw SearchError.UnknownStrokes(error.chars)
            }
    }
}

/**
 * 打ち切りの判定。姓と文字数を決めると、各格は名の各位置の画数の 1 次式になる（KakuSchool の約束）。
 * すべての位置が最小の画数のときの値と、位置ごとの係数（画数を 1 増やしたときの変化）を求めておき、
 * 決めていない位置の最小・最大から、各格が取りうる範囲を出す。
 * 全部の位置を決めると範囲は 1 点になるので、最後の判定はそのまま「条件を満たすか」になる。
 */
internal class ConditionBounds(
    kakuValues: (List<Int>) -> Map<KakuKind, Int>,
    strokeValues: List<IntArray>,
    conditions: Map<KakuKind, Set<Int>>,
) {
    private class Item(
        val base: Int,
        val coefficients: IntArray,
        /** suffixLow[i]・suffixHigh[i]: 位置 i 以降を自由に選んだときの、値の増え方の最小・最大 */
        val suffixLow: IntArray,
        val suffixHigh: IntArray,
        val allowed: IntArray,
    )

    private val mins: IntArray = IntArray(strokeValues.size) { strokeValues[it].first() }
    private val items: List<Item>
    val itemCount: Int get() = items.size

    init {
        val maxs = IntArray(strokeValues.size) { strokeValues[it].last() }
        val base = kakuValues(mins.toList())
        items = conditions.entries.sortedBy { it.key.raw }.map { (kind, allowed) ->
            val coefficients = IntArray(mins.size) { index ->
                val bumped = mins.copyOf()
                bumped[index] += 1
                kakuValues(bumped.toList())[kind]!! - base[kind]!!
            }
            val suffixLow = IntArray(mins.size + 1)
            val suffixHigh = IntArray(mins.size + 1)
            for (index in mins.indices.reversed()) {
                val span = coefficients[index] * (maxs[index] - mins[index])
                suffixLow[index] = suffixLow[index + 1] + minOf(0, span)
                suffixHigh[index] = suffixHigh[index + 1] + maxOf(0, span)
            }
            Item(base[kind]!!, coefficients, suffixLow, suffixHigh, allowed.sorted().toIntArray())
        }
    }

    /** 位置 index の画数を value にしたときの途中の値を next に書き、条件を満たす見込みがあれば true。 */
    fun extend(partial: IntArray, index: Int, value: Int, next: IntArray): Boolean {
        for ((i, item) in items.withIndex()) {
            val sum = partial[i] + item.coefficients[index] * (value - mins[index])
            next[i] = sum
            val low = item.base + sum + item.suffixLow[index + 1]
            val high = item.base + sum + item.suffixHigh[index + 1]
            // allowed は小さい順なので、low 以上の最初の値が high 以下かを二分探索で調べる
            var lower = 0
            var upper = item.allowed.size
            while (lower < upper) {
                val middle = (lower + upper) / 2
                if (item.allowed[middle] < low) lower = middle + 1 else upper = middle
            }
            if (lower == item.allowed.size || item.allowed[lower] > high) return false
        }
        return true
    }
}

/**
 * 読みがあるときの数え方。内訳（画数の組）を全部は作らず、
 * 「位置・読みの割り振りの状態・途中までの格の値 → この先で条件に合う組み合わせの数」を覚えておく。
 * 総数はこれで求め、n 件目は位置 1 から順に画数を決めて直接求める（仕様書 search.md「数え方（実装の例）」）。
 * 途中の数は 64 ビットをあふれうるので、飽和させて数える（saturatingAdd）。
 */
internal class ReadingSearch(
    private val table: ReadingTable,
    private val bounds: ConditionBounds,
    private val strokeValues: List<IntArray>,
    /** classes[i][k]: 位置 i・画数 strokeValues[i][k] の字を、同じように数えられるものにまとめたもの */
    private val classes: List<List<List<ReadingClass>>>,
    private val keepsBase: List<Boolean>,
    needsUse: Boolean,
) {
    val total: Long

    /** 位置 1 の前の状態 */
    private val start = ReadingTable.Path.start(needsUse)
    private val memo = HashMap<Key, Long>()

    private data class Key(val index: Int, val path: ReadingTable.Path, val partial: List<Int>)

    init {
        val length = strokeValues.size
        fun count(index: Int, path: ReadingTable.Path, partial: IntArray): Long {
            if (index == length) return if (table.accepts(path)) 1 else 0
            val key = Key(index, path, partial.toList())
            memo[key]?.let { return it }
            var total = 0L
            val next = partial.copyOf()
            for ((k, value) in strokeValues[index].withIndex()) {
                if (!bounds.extend(partial, index, value, next)) continue
                for (item in classes[index][k]) {
                    val moved = table.step(
                        path, item.key, item.used, remaining = length - index - 1, keepBase = keepsBase[index],
                    ) ?: continue
                    total = saturatingAdd(total, saturatingMultiply(item.count, count(index + 1, moved, next)))
                }
            }
            memo[key] = total
            return total
        }
        total = count(0, start, IntArray(bounds.itemCount))
    }

    /** 位置 index 以降で条件に合う組み合わせの数（覚えた値）。 */
    private fun suffix(index: Int, path: ReadingTable.Path, partial: IntArray): Long =
        if (index == strokeValues.size) {
            if (table.accepts(path)) 1 else 0
        } else {
            memo[Key(index, path, partial.toList())] ?: 0
        }

    /** 位置 index の画数を k 番目にしたときの、ここまでの状態の分布と途中の格の値。条件を満たせなければ null。 */
    private fun extend(
        paths: Map<ReadingTable.Path, Long>, partial: IntArray, index: Int, k: Int,
    ): Pair<Map<ReadingTable.Path, Long>, IntArray>? {
        val next = partial.copyOf()
        if (!bounds.extend(partial, index, strokeValues[index][k], next)) return null
        val moved = LinkedHashMap<ReadingTable.Path, Long>()
        for ((path, ways) in paths) {
            for (item in classes[index][k]) {
                val step = table.step(
                    path, item.key, item.used,
                    remaining = strokeValues.size - index - 1, keepBase = keepsBase[index],
                ) ?: continue
                moved[step] = saturatingAdd(moved[step] ?: 0L, saturatingMultiply(ways, item.count))
            }
        }
        return if (moved.isEmpty()) null else moved to next
    }

    private fun count(paths: Map<ReadingTable.Path, Long>, partial: IntArray, from: Int): Long =
        paths.entries.fold(0L) { sum, (path, ways) ->
            saturatingAdd(sum, saturatingMultiply(ways, suffix(from, path, partial)))
        }

    /** 全体の n 件目（0 始まり）が入っている内訳と、内訳の中での番号、その内訳の件数。 */
    fun locate(n: Long): Triple<List<Int>, Long, Long> {
        var rest = n
        var paths: Map<ReadingTable.Path, Long> = mapOf(start to 1L)
        var partial = IntArray(bounds.itemCount)
        val strokes = mutableListOf<Int>()
        var last = 0L
        for (index in strokeValues.indices) {
            for (k in strokeValues[index].indices) {
                val (moved, next) = extend(paths, partial, index, k) ?: continue
                val c = count(moved, next, index + 1)
                if (rest < c) {
                    strokes.add(strokeValues[index][k])
                    paths = moved
                    partial = next
                    last = c
                    break
                }
                rest -= c
            }
        }
        return Triple(strokes, rest, last)
    }

    /** 条件に合う候補がある内訳を、辞書式の小さい順に全部（テスト用。件数の小さい検索で使う）。 */
    fun allBreakdowns(): List<NameSearch.Breakdown> {
        val result = mutableListOf<NameSearch.Breakdown>()
        val strokes = mutableListOf<Int>()
        fun walk(index: Int, paths: Map<ReadingTable.Path, Long>, partial: IntArray) {
            for (k in strokeValues[index].indices) {
                val (moved, next) = extend(paths, partial, index, k) ?: continue
                val c = count(moved, next, index + 1)
                if (c <= 0) continue
                strokes.add(strokeValues[index][k])
                if (index + 1 == strokeValues.size) {
                    result.add(NameSearch.Breakdown(strokes.toList(), c))
                } else {
                    walk(index + 1, moved, next)
                }
                strokes.removeAt(strokes.lastIndex)
            }
        }
        walk(0, mapOf(start to 1L), IntArray(bounds.itemCount))
        return result
    }
}

/** 伸びる Int の配列（内訳の画数を、箱に入れずに持つ）。 */
private class IntArrayBuilder {
    private var array = IntArray(64)
    private var size = 0

    fun addAll(values: IntArray) {
        if (size + values.size > array.size) array = array.copyOf(maxOf(array.size * 2, size + values.size))
        values.copyInto(array, size)
        size += values.size
    }

    fun build(): IntArray = array.copyOf(size)
}

/** 伸びる Long の配列（内訳ごとの候補の数）。 */
private class LongArrayBuilder {
    private var array = LongArray(64)
    private var size = 0

    fun add(value: Long) {
        if (size == array.size) array = array.copyOf(array.size * 2)
        array[size++] = value
    }

    fun build(): LongArray = array.copyOf(size)
}
