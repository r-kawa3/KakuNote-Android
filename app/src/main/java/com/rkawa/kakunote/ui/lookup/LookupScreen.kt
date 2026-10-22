package com.rkawa.kakunote.ui.lookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.ShinjitaiSchool
import com.rkawa.kakunote.ui.components.WaCheckbox
import com.rkawa.kakunote.ui.components.WaTabScreen
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/**
 * 「調べる」画面: 姓名を入れると、各字の画数と五格が出る。
 * 設定に姓があるときは「登録した姓を使う」（初期値オン）。オンのときは登録した姓が入って変えられず、オフで自由に入力できる。
 */
@Composable
fun LookupScreen(
    data: KanjiData,
    registeredSei: String?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    school: KakuSchool = ShinjitaiSchool,
) {
    var typedSei by rememberSaveable { mutableStateOf("") }
    var mei by rememberSaveable { mutableStateOf("") }
    var usesRegisteredSei by rememberSaveable { mutableStateOf(true) }
    val appliedSei = if (usesRegisteredSei) registeredSei else null
    val sei = appliedSei ?: typedSei
    val state = remember(sei, mei, data) { LookupState.of(sei, mei, data, school) }

    WaTabScreen(seal = "画", title = "画数を調べる", onOpenSettings = onOpenSettings, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (registeredSei != null) {
                WaCheckbox(
                    "登録した姓を使う", usesRegisteredSei, { usesRegisteredSei = it }, color = Wa.accent,
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("lookup.useSei"),
                )
            }
            Column(Modifier.fillMaxWidth().waCard()) {
                InputRow("姓", sei, { typedSei = it }, "例：田中", "lookup.sei", enabled = appliedSei == null)
                HorizontalDivider(color = Wa.divider)
                InputRow("名", mei, { mei = it }, "例：健太", "lookup.mei")
            }
            Text(
                "姓を空にすると、名の総画数だけを出します。",
                style = Wa.gothic(12.sp),
                color = Wa.secondaryText,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        when (state) {
            LookupState.Empty -> Unit
            is LookupState.Shown -> LookupResultView(state.result)
            is LookupState.Failed -> Text(
                "⚠ " + state.message,
                style = Wa.gothic(15.sp),
                color = Wa.seal,
                modifier = Modifier.fillMaxWidth().waCard().padding(14.dp).testTag("lookup.error"),
            )
        }
    }
}

@Composable
private fun InputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    tag: String,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = Wa.gothic(15.sp, FontWeight.Bold).copy(letterSpacing = 1.sp),
            color = Wa.ink,
            modifier = Modifier.width(38.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            textStyle = Wa.mincho(20.sp).copy(color = if (enabled) Wa.ink else Wa.ink.copy(alpha = 0.7f)),
            cursorBrush = SolidColor(Wa.accent),
            modifier = Modifier.weight(1f).testTag(tag).semantics { contentDescription = label },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = Wa.mincho(20.sp), color = Wa.placeholder)
                inner()
            },
        )
    }
}
