package com.rkawa.kakunote.ui.result

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.SearchError
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.ui.search.SearchMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 候補の一覧の状態。検索（内訳の列挙）はバックグラウンドで 1 回だけ行い、
 * 候補はページごとに作って足していく（遅延生成）。詳細から戻っても、読み込んだ行とスクロールの位置は残る。
 */
class ResultListState(val query: SearchQuery) {

    sealed class Phase {
        data object Loading : Phase()
        data class Ready(val total: Long) : Phase()
        data class Failed(val message: String) : Phase()
    }

    var phase by mutableStateOf<Phase>(Phase.Loading)
        private set
    val rows = mutableStateListOf<ResultRow>()

    /** 一覧のスクロールの位置（詳細から戻っても同じ位置に出す） */
    val listState = LazyListState()

    /** 一覧の上に並べる条件のチップ */
    val chips: List<SearchSummary.Chip> = SearchSummary.chips(query)

    private var search: NameSearch? = null
    private var school: KakuSchool? = null

    /** 検索する（まだなら）。メインスレッドではしない。 */
    suspend fun start(data: KanjiData, school: KakuSchool) {
        if (search != null || phase is Phase.Failed) return
        this.school = school
        phase = try {
            val made = withContext(Dispatchers.Default) { NameSearch(query, data, school) }
            search = made
            loadMore()
            Phase.Ready(made.total)
        } catch (error: CancellationException) {
            throw error
        } catch (error: SearchError) {
            Phase.Failed(SearchMessages.message(error))
        }
    }

    /** 続きの 1 ページを作って足す。1 ページを作るのは軽いので、その場で行う。 */
    fun loadMore() {
        val search = search ?: return
        val school = school ?: return
        if (rows.size >= search.total) return
        val page = search.candidates(rows.size.toLong(), PAGE_SIZE).map {
            ResultRow.make(it, search.seiStrokes, query.conditions, school)
        }
        rows.addAll(page)
    }

    val hasMore: Boolean get() = search?.let { rows.size < it.total } ?: false

    companion object {
        /** 1 回に作る候補の数 */
        const val PAGE_SIZE = 50
    }
}
