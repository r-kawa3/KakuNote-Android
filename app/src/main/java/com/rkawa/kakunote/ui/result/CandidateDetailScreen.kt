package com.rkawa.kakunote.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaTopBar
import com.rkawa.kakunote.ui.lookup.LookupResultView
import com.rkawa.kakunote.ui.lookup.LookupState
import com.rkawa.kakunote.ui.theme.Wa

/** 候補の詳細: 「調べる」と同じ表示（各字の画数・警告・五格）を出す。 */
@Composable
fun CandidateDetailScreen(
    sei: String?,
    mei: String,
    data: KanjiData,
    school: KakuSchool,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember(sei, mei) { LookupState.of(sei ?: "", mei, data, school) }
    Column(modifier.fillMaxSize()) {
        WaTopBar(listOfNotNull(sei, mei).joinToString(" "), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
        ) {
            when (state) {
                is LookupState.Shown -> LookupResultView(state.result)
                is LookupState.Failed -> Text(state.message, style = Wa.gothic(), color = Wa.seal)
                LookupState.Empty -> Unit
            }
        }
    }
}
