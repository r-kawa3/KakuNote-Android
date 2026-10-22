package com.rkawa.kakunote

import androidx.test.platform.app.InstrumentationRegistry
import com.rkawa.kakunote.data.FavoriteStore
import com.rkawa.kakunote.data.SettingsStore
import org.junit.rules.ExternalResource
import java.io.File

/**
 * アプリを開く前に、端末の中に保存したもの（お気に入り・設定）を消す。テストどうしが影響しあわないように、
 * アプリを開くルールより先（order = 0）に置く。
 */
class ResetAppData : ExternalResource() {
    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(FavoriteStore.FILE_NAME)
        File(context.filesDir, SettingsStore.FILE_NAME).delete()
    }
}
