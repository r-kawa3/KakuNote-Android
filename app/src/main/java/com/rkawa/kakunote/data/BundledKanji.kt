package com.rkawa.kakunote.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rkawa.kakunote.logic.KanjiData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * アプリに同梱した漢字 DB（assets の kanji.sqlite）。起動後に 1 回だけ読み込み、アプリの中で 1 つだけ持つ。
 *
 * assets の DB は SQLite で直接開けないので、アプリの中のファイルにコピーしてから開く。
 * アプリを入れ直したり更新したりすると（DB を差し替えた版も含む）、コピーし直す。
 * DB を更新したときは、iOS のリポジトリの KakuNote/Resources/kanji.sqlite を
 * app/src/main/assets/kanji.sqlite にコピーする。
 */
object BundledKanji {

    /** 読み込みの状態。 */
    sealed class State {
        data object Loading : State()
        class Loaded(val data: KanjiData) : State()
        class Failed(val error: Throwable) : State()
    }

    private const val ASSET_NAME = "kanji.sqlite"

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = mutableState.asStateFlow()
    private val mutex = Mutex()

    /** 読み込む（まだなら）。メインスレッドでは読まない。失敗していたら、もう一度試す。 */
    suspend fun load(context: Context): State = mutex.withLock {
        (mutableState.value as? State.Loaded)?.let { return it }
        mutableState.value = State.Loading
        val result = try {
            State.Loaded(withContext(Dispatchers.IO) { read(context.applicationContext) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            State.Failed(error)
        }
        mutableState.value = result
        result
    }

    /** 同梱の DB をコピーして（要るときだけ）、読み込む。 */
    fun read(context: Context): KanjiData {
        val file = copiedDatabase(context)
        return AndroidSqlReader(file).use { KanjiData.load(it) }
    }

    /**
     * アプリの中にコピーした DB のファイル。アプリの入れ直し・更新の時刻を印のファイルに書いておき、
     * 時刻が変わっていたら（同梱の DB が変わったかもしれないので）コピーし直す。
     */
    internal fun copiedDatabase(context: Context): File {
        val dir = File(context.noBackupFilesDir, "kanji").apply { mkdirs() }
        val file = File(dir, ASSET_NAME)
        val stamp = File(dir, "$ASSET_NAME.stamp")
        val installed = installedTime(context).toString()
        if (file.exists() && stamp.exists() && stamp.readText() == installed) return file

        // 途中で止まっても壊れたファイルが残らないように、別名に書いてから置き換える
        val temporary = File(dir, "$ASSET_NAME.tmp")
        context.assets.open(ASSET_NAME).use { input -> temporary.outputStream().use { input.copyTo(it) } }
        if (!temporary.renameTo(file)) {
            temporary.delete()
            error("漢字のデータを置けませんでした")
        }
        stamp.writeText(installed)
        return file
    }

    private fun installedTime(context: Context): Long {
        val manager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(context.packageName, 0)
        }
        return info.lastUpdateTime
    }
}
