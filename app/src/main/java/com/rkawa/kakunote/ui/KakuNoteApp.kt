package com.rkawa.kakunote.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.ShinjitaiSchool
import com.rkawa.kakunote.ui.result.CandidateDetailScreen
import com.rkawa.kakunote.ui.result.ResultListState
import com.rkawa.kakunote.ui.result.ResultScreen
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.components.waContentWidth
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
 * タブは状態だけで切り替える。進んだ先（「探す」の一覧・詳細、「お気に入り」の詳細）は AppNavigation に履歴として持つ
 * （タブを切り替えても、画面が作り直されても残る）。戻るのは、上の「‹ 戻る」と端末の戻る操作。
 */
@Composable
fun KakuNoteApp(data: KanjiData, navigation: AppNavigation = viewModel(), appData: AppData = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.SEARCH) }
    var showsSettings by rememberSaveable { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    var searchStack by navigation::searchStack
    val searchHolder = rememberSaveableStateHolder()
    val school: KakuSchool = ShinjitaiSchool
    // お気に入りは「探す」の一覧と「お気に入り」タブで共有する
    val favorites = appData.favorites
    // 設定（登録した姓など）は全部の画面で共有する
    val settings = appData.settings
    var favoriteDetail by navigation::favoriteDetail

    if (showsSettings) {
        SettingsScreen(settings, data, onClose = { showsSettings = false })
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
                        onClick = {
                            // 選んでいる「探す」タブをもう一度押すと、条件の入力に戻る
                            if (tab == selectedTab && tab == AppTab.SEARCH) searchStack = emptyList()
                            if (tab == selectedTab && tab == AppTab.FAVORITES) favoriteDetail = null
                            selectedTab = tab
                        },
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
        // キーボードが出ているときも、画面の下の方までスクロールで届くようにする。広い画面では中身を真ん中に置く
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().waContentWidth()
        val openSettings = { showsSettings = true }
        // タブを切り替えても、各タブの入力やスクロールの位置を残す
        val popSearch = { searchStack = searchStack.dropLast(1) }
        BackHandler(enabled = selectedTab == AppTab.SEARCH && searchStack.isNotEmpty(), onBack = popSearch)
        BackHandler(enabled = selectedTab == AppTab.FAVORITES && favoriteDetail != null) { favoriteDetail = null }
        stateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                AppTab.SEARCH -> when (val route = searchStack.lastOrNull()) {
                    // 一覧から戻ったときも条件の入力が残るように、入力の画面の状態は別に保存する
                    null -> searchHolder.SaveableStateProvider("form") {
                        SearchScreen(
                            settings = settings.values,
                            data = data,
                            onSearch = { searchStack = listOf(SearchRoute.Results(ResultListState(it))) },
                            onOpenSettings = openSettings,
                            modifier = modifier,
                        )
                    }
                    is SearchRoute.Results -> key(route) {
                        ResultScreen(
                            state = route.state, data = data, school = school, favorites = favorites, onBack = popSearch,
                            onSelect = { row ->
                                searchStack = searchStack + SearchRoute.Detail(DetailTarget(route.state.query.sei, row.name))
                            },
                            modifier = modifier,
                        )
                    }
                    is SearchRoute.Detail -> CandidateDetailScreen(route.target.sei, route.target.mei, data, school, popSearch, modifier)
                }
                AppTab.LOOKUP -> LookupScreen(data, settings.registeredSei, openSettings, modifier)
                AppTab.FAVORITES -> when (val detail = favoriteDetail) {
                    null -> FavoritesScreen(
                        favorites = favorites, data = data, school = school, onOpenSettings = openSettings,
                        onSelect = { favoriteDetail = DetailTarget(it.sei, it.mei) },
                        onGoSearch = { selectedTab = AppTab.SEARCH },
                        modifier = modifier,
                    )
                    else -> CandidateDetailScreen(detail.sei, detail.mei, data, school, { favoriteDetail = null }, modifier)
                }
            }
        }
    }
}


