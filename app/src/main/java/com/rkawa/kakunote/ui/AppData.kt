package com.rkawa.kakunote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rkawa.kakunote.ui.favorites.FavoritesModel
import com.rkawa.kakunote.ui.settings.AppSettings

/**
 * 画面が共有する、端末の中に保存するもの（お気に入り・設定）。ViewModel に持つので、
 * 画面が作り直されても開き直さない。アプリの画面を閉じたら、お気に入りの DB を閉じる。
 */
class AppData(application: Application) : AndroidViewModel(application) {

    val favorites: FavoritesModel = FavoritesModel.open(application)
    val settings: AppSettings = AppSettings.open(application)

    override fun onCleared() {
        favorites.close()
    }
}
