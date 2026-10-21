package com.rkawa.kakunote.logic

/**
 * 読みでの絞り込み（仕様書 search.md「読み」）。
 *
 * 読みは、ひらがなのコードポイントの並びで扱う。名の読み r を、位置ごとの空でない連続した部分に分けて、
 * どれか 1 つの分け方で全部の位置の字がその部分を担えれば、その組み合わせを残す。
 */
object Reading {

    /** 読みの長さの上限（「どこまで割り振れるか」を 64 ビットの集合で持つため） */
    const val MAX_LENGTH = 30

    /** 「ー」 */
    const val LONG_VOWEL = 0x30FC

    /** 入力を、比べる形（ひらがなのコードポイントの並び）にする。前後の空白を除いて空なら null（絞り込まない）。 */
    fun normalize(text: String): List<Int>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val reading = trimmed.codePoints().toArray().map { hiragana(it) }
        if (reading.size > MAX_LENGTH || !reading.all { it in 0x3041..0x309F || it == LONG_VOWEL }) {
            throw SearchError.InvalidReading
        }
        return reading
    }

    /** カタカナ（ァ〜ヶ、ヽヾ）をひらがなに直す。「ー」やひらがなのないカタカナはそのまま。 */
    fun hiragana(code: Int): Int =
        if (code in 0x30A1..0x30F6 || code == 0x30FD || code == 0x30FE) code - 0x60 else code

    /** 読みの頭の部分で、手前で区切らない小さい字 */
    val smallKana: Set<Int> = "ぁぃぅぇぉっゃゅょゎゕゖ".codePoints().toArray().toSet()

    /** 読みの頭の部分（先頭の 1 文字以上）。小さい字の手前では区切らない。読みそのものも含む。 */
    fun prefixes(of: List<Int>): List<List<Int>> =
        (1..maxOf(of.size, 1)).mapNotNull { k ->
            if (k <= of.size && (k == of.size || of[k] !in smallKana)) of.subList(0, k).toList() else null
        }

    /** 連濁: 1 文字目の替え方（か〜こ → が〜ご、さ〜そ → ざ〜ぞ、た〜と → だ〜ど、は〜ほ → ば〜ぼ・ぱ〜ぽ） */
    val rendaku: Map<Int, List<Int>> = buildMap {
        val plain = "かきくけこさしすせそたちつてと".codePoints().toArray()
        val voiced = "がぎぐげござじずぜぞだぢづでど".codePoints().toArray()
        plain.indices.forEach { put(plain[it], listOf(voiced[it])) }
        val h = "はひふへほ".codePoints().toArray()
        val b = "ばびぶべぼ".codePoints().toArray()
        val p = "ぱぴぷぺぽ".codePoints().toArray()
        h.indices.forEach { put(h[it], listOf(b[it], p[it])) }
    }
}

/**
 * 1 回の検索での、読みの割り振りの表。
 *
 * 字の読みの集合（連濁なし）に番号（setID）を付け、「読みの s 文字目から始めて、どこまで担えるか」を
 * 64 ビットの集合で持つ。同じ読みの集合の字は、同じように割り振れるので、まとめて扱える。
 */
class ReadingTable(
    /** 名の読み（ひらがなのコードポイント） */
    val reading: List<Int>,
    /** 漢字の読みの頭の部分も使うか */
    val usesPrefixes: Boolean = false,
) {
    /** spans[setID * 2 + (連濁ありなら 1)][s]: 読みの s 文字目から始めたときに、担い終われる位置の集合 */
    private val spans = mutableListOf<LongArray>()
    private val setIDs = HashMap<Set<List<Int>>, Int>()

    /** 位置 position（0 始まり）の字 info のキー。々 は ODORIJI。どこも担えない字は null（候補から外す）。 */
    fun key(info: CharInfo, position: Int, data: KanjiData): Int? {
        val scalar = info.codePoint
        if (scalar == ODORIJI_CHAR) {
            return if (position == 0) null else ODORIJI
        }
        val readings: Set<List<Int>> = if (info.script == Script.KANJI) {
            val own = (data.readings[scalar] ?: emptySet()).map { it.codePoints().toArray().toList() }
            (if (usesPrefixes) own.flatMap { Reading.prefixes(it) } else own).toSet()
        } else {
            setOf(listOf(Reading.hiragana(scalar)))
        }
        val id = setID(readings)
        // 連濁は、位置 2 以降の漢字だけ
        val key = id * 2 + if (position > 0 && info.script == Script.KANJI) 1 else 0
        return if (spans[key].any { it != 0L }) key else null
    }

    private fun setID(readings: Set<List<Int>>): Int {
        setIDs[readings]?.let { return it }
        val id = setIDs.size
        setIDs[readings] = id
        val voiced = readings.flatMap { y ->
            val replacements = y.firstOrNull()?.let { Reading.rendaku[it] } ?: return@flatMap emptyList()
            replacements.map { listOf(it) + y.drop(1) }
        }.toSet() + readings
        spans.add(makeSpans(readings))
        spans.add(makeSpans(voiced))
        return id
    }

    private fun makeSpans(readings: Set<List<Int>>): LongArray {
        val m = reading.size
        return LongArray(m) { start ->
            var ends = 0L
            for (y in readings) {
                if (y.isNotEmpty() && start + y.size <= m && reading.subList(start, start + y.size) == y) {
                    ends = ends or (1L shl (start + y.size))
                }
            }
            ends
        }
    }

    /**
     * 位置ごとに数えていくときの状態: ここまでで割り振り終われる位置の集合、使いたい字がもう入っているか、
     * 直前の 々 でない字の読みの setID（覚えなくてよいときは -1）。
     */
    data class Path(val ends: Long, val satisfied: Boolean, val base: Int) {
        companion object {
            fun start(needsUse: Boolean) = Path(ends = 1L, satisfied = !needsUse, base = -1)
        }
    }

    /**
     * 位置 position（0 始まり）に、キー key の字を置いたあとの状態。割り振れなくなれば null。
     * remaining はそのあとに残る位置の数（残りの位置にも 1 文字以上ずつ要るので、割り振り終わる位置を絞る）。
     * keepBase が false なら、直前の字の読みを覚えない（この先に 々 がないとき）。
     */
    fun step(path: Path, key: Int, used: Boolean, remaining: Int, keepBase: Boolean): Path? {
        val base: Int
        val spanKey: Int
        if (key == ODORIJI) {
            if (path.base < 0) return null
            base = path.base
            // 々 は漢字なので、連濁を認める（位置 1 の 々 は、はじめから外している）
            spanKey = path.base * 2 + 1
        } else {
            base = key shr 1
            spanKey = key
        }
        val limit = reading.size - remaining
        if (limit <= 0) return null
        val ends = advance(path.ends, spanKey) and ((2L shl limit) - 1)
        if (ends == 0L) return null
        return Path(ends, path.satisfied || used, if (keepBase) base else -1)
    }

    /** 最後の位置まで置いたあと、条件に合っているか。 */
    fun accepts(path: Path): Boolean = path.satisfied && (path.ends and (1L shl reading.size)) != 0L

    /** 割り振れる位置の集合 S から、キー key の字を置いたあとの集合。 */
    private fun advance(state: Long, key: Int): Long {
        var next = 0L
        var rest = state
        val table = spans[key]
        while (rest != 0L) {
            val start = rest.countTrailingZeroBits()
            rest = rest and (rest - 1)
            if (start < table.size) next = next or table[start]
        }
        return next
    }

    companion object {
        /** 々 を表すキー（直前の字の読みを使う） */
        const val ODORIJI = -1
        private const val ODORIJI_CHAR = 0x3005
    }
}

/** 位置ごとの字を、同じように数えられるもの（読みのキーと、使いたい字かどうかが同じ）にまとめる。 */
data class ReadingClass(val key: Int, val used: Boolean, val count: Long) {
    companion object {
        fun group(keys: List<Int>, used: List<Boolean>): List<ReadingClass> {
            val counts = LinkedHashMap<Pair<Int, Boolean>, Long>()
            keys.forEachIndexed { i, key ->
                val id = key to used[i]
                counts[id] = (counts[id] ?: 0L) + 1
            }
            return counts.map { (id, count) -> ReadingClass(id.first, id.second, count) }
        }
    }
}

/**
 * 1 つの内訳の中で、読み（と使いたい字）の条件に合う組み合わせの n 番目を求める。
 * 「この先で条件に合う組み合わせの数」を状態ごとに覚えておく（仕様書 search.md「数え方（実装の例）」）。
 */
class ReadingCounter(
    /** 位置ごとの字のキー（ReadingTable.key） */
    private val keys: List<List<Int>>,
    /** 位置ごとの、使いたい字かどうか */
    private val used: List<List<Boolean>>,
    private val table: ReadingTable,
    private val needsUse: Boolean,
) {
    /** 位置 i より後ろに 々 があるか（なければ、直前の字の読みを覚えなくてよい） */
    private val keepsBase = keys.indices.map { i -> keys.drop(i + 1).any { ReadingTable.ODORIJI in it } }
    private val classes = keys.indices.map { ReadingClass.group(keys[it], used[it]) }
    private val memo = HashMap<Memo, Long>()

    private data class Memo(val index: Int, val path: ReadingTable.Path)

    /** 条件に合う組み合わせの数。 */
    fun total(): Long = count(0, ReadingTable.Path.start(needsUse))

    /** 条件に合う組み合わせのうち n 番目（0 始まり）の、位置ごとの字の番号。 */
    fun nth(n: Long): List<Int> {
        var rest = n
        var path = ReadingTable.Path.start(needsUse)
        val picks = mutableListOf<Int>()
        for (i in keys.indices) {
            for ((k, key) in keys[i].withIndex()) {
                val next = step(i, path, key, used[i][k]) ?: continue
                val c = count(i + 1, next)
                if (rest < c) {
                    picks.add(k)
                    path = next
                    break
                }
                rest -= c
            }
        }
        return picks
    }

    private fun step(index: Int, path: ReadingTable.Path, key: Int, used: Boolean): ReadingTable.Path? =
        table.step(path, key, used, remaining = keys.size - index - 1, keepBase = keepsBase[index])

    private fun count(index: Int, path: ReadingTable.Path): Long {
        if (index == keys.size) return if (table.accepts(path)) 1 else 0
        val memoKey = Memo(index, path)
        memo[memoKey]?.let { return it }
        var total = 0L
        for (item in classes[index]) {
            val next = step(index, path, item.key, item.used) ?: continue
            total = saturatingAdd(total, saturatingMultiply(item.count, count(index + 1, next)))
        }
        memo[memoKey] = total
        return total
    }
}

/**
 * 数の上限。読みのある数え方では、途中の数が 64 ビットをあふれうるので、ここで止める（飽和させる）。
 * 足し算とかけ算だけで、比べる相手はいつも NameSearch.MAX_TOTAL 以下なので、止めても結果は変わらない。
 */
internal fun saturatingAdd(a: Long, b: Long): Long = if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b

internal fun saturatingMultiply(a: Long, b: Long): Long =
    if (a != 0L && b > Long.MAX_VALUE / a) Long.MAX_VALUE else a * b
