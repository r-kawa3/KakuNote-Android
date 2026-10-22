package com.rkawa.kakunote.ui

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.rkawa.kakunote.data.SearchQueryCodec
import com.rkawa.kakunote.ui.result.ResultListState
import org.json.JSONObject

/** 「探す」タブの中で進んだ先の画面。 */
sealed class SearchRoute {
    /** 候補の一覧。一覧の状態（読み込んだ行・スクロールの位置）をここに持つので、詳細から戻っても残る */
    class Results(val state: ResultListState) : SearchRoute()

    class Detail(val target: DetailTarget) : SearchRoute()
}

/** 詳細を出す姓・名。 */
data class DetailTarget(val sei: String?, val mei: String)

/**
 * 進んだ先の画面（「探す」の一覧・詳細、「お気に入り」の詳細）の履歴。
 *
 * ViewModel に持つので、画面が作り直されても（回転、ダークモードの切り替え、文字の大きさの変更など）そのまま残り、
 * 一覧の検索もやり直さない。アプリが裏で終了させられたときのために、開いていた画面とスクロールの位置を
 * 保存しておき、開き直したときに作り直す（そのときは検索をやり直す）。
 */
class AppNavigation(handle: SavedStateHandle) : ViewModel() {

    var searchStack by mutableStateOf(NavigationCodec.decodeSearch(handle.get<Bundle>(KEY)?.getStringArrayList(SEARCH)))
    var favoriteDetail by mutableStateOf(handle.get<Bundle>(KEY)?.getString(FAVORITE)?.let(NavigationCodec::decodeTarget))

    init {
        handle.setSavedStateProvider(KEY) {
            Bundle().apply {
                putStringArrayList(SEARCH, ArrayList(NavigationCodec.encodeSearch(searchStack)))
                favoriteDetail?.let { putString(FAVORITE, NavigationCodec.encodeTarget(it)) }
            }
        }
    }

    private companion object {
        const val KEY = "navigation"
        const val SEARCH = "search"
        const val FAVORITE = "favorite"
    }
}

/** 進んだ先の画面の履歴を文字列（JSON）にする。Android に依存しないので単体テストできる。 */
object NavigationCodec {

    fun encodeSearch(stack: List<SearchRoute>): List<String> = stack.map { route ->
        when (route) {
            is SearchRoute.Results -> JSONObject()
                .put("type", "results")
                .put("query", SearchQueryCodec.encode(route.state.query))
                .put("index", route.state.listState.firstVisibleItemIndex)
                .put("offset", route.state.listState.firstVisibleItemScrollOffset)
            is SearchRoute.Detail -> JSONObject(encodeTarget(route.target)).put("type", "detail")
        }.toString()
    }

    /** 読めない行は飛ばす（その先の画面も、つながりが崩れるので捨てる） */
    fun decodeSearch(saved: List<String>?): List<SearchRoute> {
        val routes = mutableListOf<SearchRoute>()
        for (text in saved ?: emptyList()) {
            val route = try {
                val json = JSONObject(text)
                when (json.getString("type")) {
                    "results" -> {
                        val query = SearchQueryCodec.decodeOrNull(json.getString("query")) ?: break
                        SearchRoute.Results(ResultListState(query, json.optInt("index"), json.optInt("offset")))
                    }
                    "detail" -> SearchRoute.Detail(decodeTarget(text) ?: break)
                    else -> break
                }
            } catch (_: Exception) {
                break
            }
            routes.add(route)
        }
        return routes
    }

    fun encodeTarget(target: DetailTarget): String =
        JSONObject().put("sei", target.sei ?: JSONObject.NULL).put("mei", target.mei).toString()

    fun decodeTarget(text: String): DetailTarget? = try {
        val json = JSONObject(text)
        DetailTarget(if (json.isNull("sei")) null else json.getString("sei"), json.getString("mei"))
    } catch (_: Exception) {
        null
    }
}
