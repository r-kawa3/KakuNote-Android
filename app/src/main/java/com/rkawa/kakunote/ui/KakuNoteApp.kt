package com.rkawa.kakunote.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.favorites.FavoritesScreen
import com.rkawa.kakunote.ui.lookup.LookupScreen
import com.rkawa.kakunote.ui.search.SearchScreen
import com.rkawa.kakunote.ui.settings.SettingsScreen
import com.rkawa.kakunote.ui.theme.Wa

/** 下のタブ。 */
enum class AppTab(val label: String, val icon: ImageVector) {
    SEARCH("探す", WaIcons.Search),
    LOOKUP("調べる", WaIcons.Book),
    FAVORITES("お気に入り", WaIcons.Heart),
}

/**
 * アプリの骨組み: 下のタブ（探す・調べる・お気に入り）と、どの画面からも開ける設定画面。
 * タブは状態だけで切り替える（画面を進める・戻る仕組みは、候補の一覧などを作るチケットで足す）。
 */
@Composable
fun KakuNoteApp(data: KanjiData) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.SEARCH) }
    var showsSettings by rememberSaveable { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()

    if (showsSettings) {
        SettingsScreen(onClose = { showsSettings = false })
        return
    }

    Scaffold(
        containerColor = Wa.background,
        contentColor = Wa.ink,
        bottomBar = {
            NavigationBar(containerColor = Wa.surfaceMuted) {
                for (tab in AppTab.entries) {
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(tab.label, style = Wa.gothic(12.sp)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Wa.accent,
                            selectedTextColor = Wa.accent,
                            unselectedIconColor = Wa.secondaryText,
                            unselectedTextColor = Wa.secondaryText,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // キーボードが出ているときも、画面の下の方までスクロールで届くようにする
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
        val openSettings = { showsSettings = true }
        // タブを切り替えても、各タブの入力やスクロールの位置を残す
        stateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                AppTab.SEARCH -> SearchScreen(data, openSettings, modifier)
                AppTab.LOOKUP -> LookupScreen(data, openSettings, modifier)
                AppTab.FAVORITES -> FavoritesScreen(data, openSettings, modifier)
            }
        }
    }
}


