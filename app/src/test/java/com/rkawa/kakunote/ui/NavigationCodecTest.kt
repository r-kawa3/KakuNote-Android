package com.rkawa.kakunote.ui

import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.PositionCondition
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.ui.result.ResultListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 開いていた画面の履歴を、アプリを開き直したときのために保存して戻す。 */
class NavigationCodecTest {

    private val query = SearchQuery(
        sei = "田中", positions = listOf(PositionCondition.Fixed("陽"), PositionCondition.Free(ScriptFilter.KANJI)),
        conditions = mapOf(KakuKind.SOUKAKU to setOf(24)), reading = "はると",
    )

    @Test
    fun roundTrip() {
        val stack = listOf(
            SearchRoute.Results(ResultListState(query, firstIndex = 37, firstOffset = 12)),
            SearchRoute.Detail(DetailTarget("田中", "陽斗")),
        )
        val restored = NavigationCodec.decodeSearch(NavigationCodec.encodeSearch(stack))
        assertEquals(2, restored.size)
        val results = restored[0] as SearchRoute.Results
        assertEquals(query, results.state.query)
        assertEquals(37, results.state.listState.firstVisibleItemIndex)
        assertEquals(12, results.state.listState.firstVisibleItemScrollOffset)
        assertEquals(DetailTarget("田中", "陽斗"), (restored[1] as SearchRoute.Detail).target)
    }

    @Test
    fun nameOnlyTarget() {
        assertEquals(DetailTarget(null, "陽菜"), NavigationCodec.decodeTarget(NavigationCodec.encodeTarget(DetailTarget(null, "陽菜"))))
        assertNull(NavigationCodec.decodeTarget("こわれた"))
    }

    @Test
    fun brokenEntryDropsTheRest() {
        val good = NavigationCodec.encodeSearch(listOf(SearchRoute.Results(ResultListState(query))))
        val restored = NavigationCodec.decodeSearch(good + "こわれた" + NavigationCodec.encodeSearch(listOf(SearchRoute.Detail(DetailTarget(null, "一")))))
        assertEquals(1, restored.size)
        assertEquals(emptyList<SearchRoute>(), NavigationCodec.decodeSearch(null))
    }
}
