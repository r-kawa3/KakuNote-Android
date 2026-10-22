package com.rkawa.kakunote.ui.favorites

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rkawa.kakunote.data.AndroidWritableSql
import com.rkawa.kakunote.data.Favorite
import com.rkawa.kakunote.data.FavoriteStore
import com.rkawa.kakunote.logic.SearchQuery

/**
 * 画面が共有するお気に入りの状態。「探す」の一覧と「お気に入り」タブの両方から使う。
 * 保存は小さな SQLite に 1 行書くだけなので、その場で行う。
 */
class FavoritesModel(private val store: FavoriteStore?, private val onClose: () -> Unit = {}) {

    var items by mutableStateOf<List<Favorite>>(emptyList())
        private set
    var errorMessage by mutableStateOf(if (store == null) "お気に入りを保存するファイルを開けませんでした" else null)
        private set

    /**
     * 保存済みの姓・名（Kotlin の String は正準等価な字を同じとみなさないので、そのまま比べてよい）。
     * 画面が変化に気づけるように、これも状態として持つ（ハートの色がすぐ変わる）
     */
    private var keys by mutableStateOf(emptySet<Pair<String, String>>())

    init {
        reload()
    }

    fun isFavorite(sei: String?, mei: String): Boolean = (sei ?: "") to mei in keys

    /** 保存済みなら削除し、まだなら保存する。 */
    fun toggle(sei: String?, mei: String, query: SearchQuery?) {
        val store = store ?: return
        try {
            if (isFavorite(sei, mei)) store.remove(sei, mei) else store.add(sei, mei, query)
        } catch (_: Exception) {
            errorMessage = "お気に入りを保存できませんでした"
        }
        reload()
    }

    fun remove(favorite: Favorite) {
        val store = store ?: return
        try {
            store.remove(favorite.sei, favorite.mei)
        } catch (_: Exception) {
            errorMessage = "お気に入りを削除できませんでした"
        }
        reload()
    }

    /** 保存の DB を閉じる（このあとは使わない） */
    fun close() = onClose()

    private fun reload() {
        val store = store ?: return
        try {
            items = store.all()
        } catch (_: Exception) {
            errorMessage = "お気に入りを読み込めませんでした"
        }
        keys = items.map { (it.sei ?: "") to it.mei }.toSet()
    }

    companion object {
        /** アプリが使うお気に入り（アプリの databases の favorites.sqlite。同梱の漢字 DB とは別）。 */
        fun open(context: Context): FavoritesModel {
            val sql = try {
                AndroidWritableSql(context.getDatabasePath(FavoriteStore.FILE_NAME).apply { parentFile?.mkdirs() })
            } catch (_: Exception) {
                null
            }
            val store = sql?.let {
                try {
                    FavoriteStore(it)
                } catch (_: Exception) {
                    null
                }
            }
            return FavoritesModel(store) { sql?.close() }
        }
    }
}
