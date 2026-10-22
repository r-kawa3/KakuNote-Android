package com.rkawa.kakunote.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.logic.KakuSchool
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchError
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.logic.ShinjitaiSchool
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.components.WaSectionLabel
import com.rkawa.kakunote.ui.components.WaTabScreen
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat

/** 「この条件で探す」を押したあとの状態（候補の一覧はチケット 07 で作る。今は件数だけ出す） */
private sealed class SearchStatus {
    data object Idle : SearchStatus()
    data object Running : SearchStatus()
    data class Found(val total: Long) : SearchStatus()
    data class Failed(val message: String) : SearchStatus()
}

/**
 * 「探す」画面: 総画数（姓を使うなら総格）を一番上に大きく置き、名の文字数・名前の形（位置ごとの条件）を
 * 指定して、名前の候補を探す。「この条件で探す」ボタンは画面の下に固定する。
 * 姓・ほかの格・読みは、設定を作るチケット（09・10）で足す。
 */
@Composable
fun SearchScreen(
    data: KanjiData,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    school: KakuSchool = ShinjitaiSchool,
) {
    var form by rememberSaveable(stateSaver = SearchFormSaver) { mutableStateOf(SearchForm()) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingQuery by remember { mutableStateOf<SearchQuery?>(null) }
    var status by remember { mutableStateOf<SearchStatus>(SearchStatus.Idle) }
    val focusManager = LocalFocusManager.current

    // 検索はメインスレッドでしない（6 文字で条件が広いと時間がかかることがある）
    LaunchedEffect(pendingQuery) {
        val query = pendingQuery ?: return@LaunchedEffect
        status = SearchStatus.Running
        status = withContext(Dispatchers.Default) {
            try {
                SearchStatus.Found(NameSearch(query, data, school).total)
            } catch (error: SearchError) {
                SearchStatus.Failed(SearchMessages.message(error))
            }
        }
    }

    fun submit() {
        focusManager.clearFocus()
        when (val result = form.toQuery()) {
            is FormResult.Invalid -> {
                result.selectIndex?.let { form = form.copy(selectedIndex = it) }
                formError = result.message
                status = SearchStatus.Idle
                pendingQuery = null
            }
            is FormResult.Ready -> {
                formError = null
                pendingQuery = result.query
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        WaTabScreen(
            seal = "名", title = "名前を探す", onOpenSettings = onOpenSettings,
            modifier = Modifier.weight(1f),
        ) {
            TotalCard(
                title = "総画数",
                text = form.totalText,
                onTextChange = { form = form.copy(totalText = it) },
                onDone = { focusManager.clearFocus() },
            )
            LengthSection(form.length) { form = form.withLength(it) }
            NameShapeSection(
                form = form,
                onSelect = { form = form.copy(selectedIndex = it) },
                onChange = { change -> form = form.updateSelected(change) },
                onDone = { focusManager.clearFocus() },
            )
        }
        SubmitBar(
            formError = formError,
            status = status,
            onSubmit = ::submit,
        )
    }
}

// MARK: - 総画数（総格）のカード

@Composable
private fun TotalCard(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Wa.accent, RoundedCornerShape(Wa.Metrics.largeCornerRadius))
            .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = Wa.gothic(13.sp, FontWeight.Bold).copy(letterSpacing = 2.sp), color = Wa.onAccent)
            Spacer(Modifier.weight(1f))
            Text("複数は「,」で区切ります", style = Wa.gothic(11.sp), color = Wa.onAccentFaint)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        Wa.onAccent.copy(alpha = 0.4f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NumberField(
                value = text,
                onValueChange = onTextChange,
                placeholder = "20",
                textStyle = Wa.mincho(64.sp, bold = true).copy(color = Wa.onAccent),
                placeholderColor = Wa.onAccent.copy(alpha = 0.4f),
                cursor = Wa.onAccent,
                label = title,
                tag = "search.total",
                onDone = onDone,
                modifier = Modifier.weight(1f),
            )
            Text("画", style = Wa.mincho(24.sp), color = Wa.onAccentFaint, modifier = Modifier.padding(bottom = 14.dp))
        }
    }
}

/**
 * 数字の欄。数字専用のキーボード（かな変換を通らない）で、キーボードの「完了」で閉じる。
 * Android の数字キーボードには「,」と空白があるので、複数の値の区切りもそのまま入れられる
 * （iOS は数字キーボードに区切りと完了がないので、キーボードの上にボタンを足している）。
 */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    placeholderColor: Color,
    cursor: Color,
    label: String,
    tag: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(cursor),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .testTag(tag)
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(contentAlignment = if (textStyle.textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = placeholderColor))
                inner()
            }
        },
    )
}

// MARK: - 文字数

@Composable
private fun LengthSection(length: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            WaSectionLabel("文字数")
            Spacer(Modifier.weight(1f))
            Text("1〜${NameSearch.MAX_LENGTH}文字", style = Wa.gothic(11.sp), color = Wa.secondaryText)
        }
        Row(Modifier.fillMaxWidth().waCard().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", filled = false, label = "文字数を減らす", tag = "search.length.minus", enabled = length > 1) {
                onChange(length - 1)
            }
            Row(
                Modifier.weight(1f).testTag("search.length.value").semantics(mergeDescendants = true) {
                    contentDescription = "${length}文字"
                },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("$length", style = Wa.mincho(30.sp, bold = true), color = Wa.ink)
                Spacer(Modifier.width(4.dp))
                Text("文字", style = Wa.gothic(16.sp), color = Wa.secondaryText, modifier = Modifier.padding(bottom = 5.dp))
            }
            StepButton(
                "+", filled = true, label = "文字数を増やす", tag = "search.length.plus",
                enabled = length < NameSearch.MAX_LENGTH,
            ) { onChange(length + 1) }
        }
    }
}

@Composable
private fun StepButton(text: String, filled: Boolean, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .size(Wa.Metrics.minTapSize)
            .background((if (filled) Wa.accent else Wa.surfaceMuted).copy(alpha = if (enabled) 1f else 0.4f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(tag)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Wa.gothic(22.sp, FontWeight.Bold), color = if (filled) Wa.onAccent else Wa.ink)
    }
}

// MARK: - 名前の形

@Composable
private fun NameShapeSection(
    form: SearchForm,
    onSelect: (Int) -> Unit,
    onChange: ((PositionInput) -> PositionInput) -> Unit,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WaSectionLabel("名前の形")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (index in 0 until form.length) {
                NameCell(form.positions[index], index, isSelected = index == form.selectedIndex) { onSelect(index) }
            }
        }
        KindTabs(form.selected.kind) { kind -> onChange { it.copy(kind = kind) } }
        PositionDetail(form.selected, onChange, onDone)
    }
}

/** 名前の形の 1 マス。固定は実線の枠にその字、自由は点線の枠に「自由」、画数指定は「n画」。 */
@Composable
private fun NameCell(input: PositionInput, index: Int, isSelected: Boolean, onClick: () -> Unit) {
    val isFree = input.kind == PositionKind.FREE
    val shape = RoundedCornerShape(Wa.Metrics.cellCornerRadius)
    Column(
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("search.cell.${index + 1}")
            .semantics(mergeDescendants = true) {
                contentDescription = "${index + 1}文字目 ${input.cellText}"
                selected = isSelected
                stateDescription = if (isSelected) "選択中" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(Wa.Metrics.cellSize)
                .background(if (isFree) Color.Transparent else Wa.surface, shape)
                .then(
                    if (isFree && !isSelected) {
                        Modifier.drawBehind {
                            val stroke = 1.5.dp.toPx()
                            drawRoundRect(
                                color = Wa.dashed,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(Wa.Metrics.cellCornerRadius.toPx()),
                                style = Stroke(
                                    width = stroke,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                                ),
                            )
                        }
                    } else {
                        Modifier.border(if (isSelected) 2.dp else 1.5.dp, if (isSelected) Wa.accent else Wa.border, shape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                input.cellText,
                style = if (input.hasFixedChar) Wa.mincho(36.sp, bold = true) else Wa.gothic(15.sp).copy(letterSpacing = 1.sp),
                color = if (isFree) Wa.secondaryText else Wa.ink,
                maxLines = 1,
            )
        }
        Text(
            "${index + 1}文字目",
            style = Wa.gothic(11.sp, if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) Wa.accent else Wa.secondaryText,
        )
    }
}

/** 選んだマスの「自由／画数指定／固定」。選んでいるものに藍の下線を引く。 */
@Composable
private fun KindTabs(kind: PositionKind, onSelect: (PositionKind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        for (item in PositionKind.entries) {
            val isSelected = item == kind
            Column(
                Modifier
                    .defaultMinSize(minHeight = Wa.Metrics.minTapSize)
                    .clickable(role = Role.Tab, onClick = { onSelect(item) })
                    .testTag("search.kind.${item.id}")
                    .semantics { selected = isSelected },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    item.label,
                    style = Wa.gothic(15.sp, if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) Wa.accent else Wa.secondaryText,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                )
                Box(Modifier.height(2.dp).width(56.dp).background(if (isSelected) Wa.accent else Color.Transparent))
            }
        }
    }
}

/** 選んだマスの入力: 固定する字、画数、文字の種類。 */
@Composable
private fun PositionDetail(
    input: PositionInput,
    onChange: ((PositionInput) -> PositionInput) -> Unit,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (input.kind) {
            PositionKind.FIXED -> DetailRow("固定する字") {
                BasicTextField(
                    value = input.fixedText,
                    onValueChange = { text -> onChange { it.copy(fixedText = text) } },
                    singleLine = true,
                    textStyle = Wa.mincho(24.sp, bold = true).copy(color = Wa.ink, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(Wa.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = Modifier
                        .width(160.dp)
                        .testTag("search.fixed")
                        .semantics { contentDescription = "固定する字" },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (input.fixedText.isEmpty()) {
                                Text("例: 陽", style = Wa.mincho(24.sp, bold = true), color = Wa.placeholder)
                            }
                            inner()
                        }
                    },
                )
            }
            PositionKind.STROKES -> {
                DetailRow("画数") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        NumberField(
                            value = input.strokesText,
                            onValueChange = { text -> onChange { it.copy(strokesText = text) } },
                            placeholder = "例: 12",
                            textStyle = Wa.mincho(22.sp, bold = true).copy(color = Wa.ink, textAlign = TextAlign.End),
                            placeholderColor = Wa.placeholder,
                            cursor = Wa.accent,
                            label = "画数",
                            tag = "search.strokes",
                            onDone = onDone,
                            modifier = Modifier.width(120.dp),
                        )
                        Text("画", style = Wa.gothic(16.sp), color = Wa.secondaryText)
                    }
                }
                ScriptRow(input.script) { script -> onChange { it.copy(script = script) } }
            }
            PositionKind.FREE -> ScriptRow(input.script) { script -> onChange { it.copy(script = script) } }
        }
    }
}

@Composable
private fun ScriptRow(script: ScriptFilter, onSelect: (ScriptFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DetailRow("文字の種類") {
        Box {
            TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("search.script")) {
                Text(script.label + " ▾", style = Wa.gothic(16.sp), color = Wa.accent)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Wa.surface) {
                for (item in ScriptFilter.entries) {
                    DropdownMenuItem(
                        text = { Text(item.label, style = Wa.gothic(16.sp), color = Wa.ink) },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        },
                        modifier = Modifier.testTag("search.script.${item.raw}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(title: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).waCard(10.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Wa.gothic(15.sp), color = Wa.ink)
        Spacer(Modifier.weight(1f))
        content()
    }
}

// MARK: - 「この条件で探す」

@Composable
private fun SubmitBar(
    formError: String?,
    status: SearchStatus,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Wa.background)
            .padding(horizontal = Wa.Metrics.screenPadding)
            .padding(top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (formError != null) {
            Text(formError, style = Wa.gothic(13.sp), color = Wa.seal, modifier = Modifier.fillMaxWidth().testTag("search.formError"))
        }
        when (status) {
            SearchStatus.Idle -> Unit
            SearchStatus.Running -> Text("探しています…", style = Wa.gothic(13.sp), color = Wa.secondaryText)
            is SearchStatus.Found -> Text(
                "候補は ${NumberFormat.getIntegerInstance().format(status.total)}件 です（一覧は次のチケットで作ります）",
                style = Wa.gothic(13.sp, FontWeight.Bold),
                color = Wa.accent,
                modifier = Modifier.fillMaxWidth().testTag("search.resultCount"),
            )
            is SearchStatus.Failed -> Text(
                status.message,
                style = Wa.gothic(13.sp),
                color = Wa.seal,
                modifier = Modifier.fillMaxWidth().testTag("search.resultError"),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(Wa.Metrics.primaryButtonHeight)
                .background(Wa.accent, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onSubmit)
                .testTag("search.submit"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(WaIcons.Search, contentDescription = null, tint = Wa.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("この条件で探す", style = Wa.gothic(17.sp, FontWeight.Bold).copy(letterSpacing = 3.sp), color = Wa.onAccent)
        }
    }
}

/** 入力を、画面を回したり作り直したりしても残す（文字列と数の並びにして保存する） */
private val SearchFormSaver = Saver<SearchForm, List<Any>>(
    save = { form ->
        listOf<Any>(form.length, form.selectedIndex, form.totalText) +
            form.positions.flatMap { listOf(it.kind.name, it.script.name, it.strokesText, it.fixedText) }
    },
    restore = { list ->
        val positions = list.drop(3).chunked(4).map {
            PositionInput(
                kind = PositionKind.valueOf(it[0] as String),
                script = ScriptFilter.valueOf(it[1] as String),
                strokesText = it[2] as String,
                fixedText = it[3] as String,
            )
        }
        SearchForm(length = list[0] as Int, positions = positions, selectedIndex = list[1] as Int, totalText = list[2] as String)
    },
)
