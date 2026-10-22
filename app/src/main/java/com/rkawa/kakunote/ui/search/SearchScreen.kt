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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.rkawa.kakunote.data.SeiCheck
import com.rkawa.kakunote.data.SettingsValues
import com.rkawa.kakunote.logic.KakuKind
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.logic.NameSearch
import com.rkawa.kakunote.logic.ScriptFilter
import com.rkawa.kakunote.logic.SearchQuery
import com.rkawa.kakunote.ui.components.WaCheckbox
import com.rkawa.kakunote.ui.components.WaIcons
import com.rkawa.kakunote.ui.components.WaSectionLabel
import com.rkawa.kakunote.ui.components.WaTabScreen
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/**
 * 「探す」画面: 総画数（姓を使うなら総格）を一番上に大きく置き、名の文字数・名前の形（位置ごとの条件）を
 * 指定して、名前の候補を探す。「この条件で探す」ボタンは画面の下に固定し、押すと onSearch で候補の一覧に進む。
 * 姓は設定画面で登録したもの（registeredSei）を使い、「姓を使う」をオフにすると名前だけで探す。
 * 避けたい字・使いたい字・読みの頭の部分は設定（settings）のものを使い、読みはこの画面の読みの欄で入れる。
 */
@Composable
fun SearchScreen(
    settings: SettingsValues,
    data: KanjiData,
    onSearch: (SearchQuery) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var form by rememberSaveable(stateSaver = SearchFormSaver) { mutableStateOf(SearchForm()) }
    /** 登録した姓を使うか（一番上のカードの「姓を使う」） */
    var usesSei by rememberSaveable { mutableStateOf(true) }
    var showsOtherKaku by rememberSaveable { mutableStateOf(false) }
    /** 名前全体の読み（ひらがな）。空なら読みで絞り込まない */
    var reading by rememberSaveable { mutableStateOf("") }
    val registeredSei = settings.registeredSei
    val focusManager = LocalFocusManager.current
    /** 検索に使う姓。登録がない・「姓を使う」がオフなら null */
    val sei = if (usesSei) registeredSei else null

    // 押す前に、探せる入力かを調べる。探せないときは「この条件で探す」を押せなくし、理由を出す
    val check = remember(form, sei, reading, settings, data) {
        form.check(data, sei, reading, settings.avoidChars, settings.useChars, settings.readingPrefix)
    }

    fun submit() {
        focusManager.clearFocus()
        reading = SearchForm.hiragana(reading)
        val ready = form.check(data, sei, reading, settings.avoidChars, settings.useChars, settings.readingPrefix)
        if (ready is FormResult.Ready) onSearch(ready.query)
    }

    Column(modifier.fillMaxSize()) {
        WaTabScreen(
            seal = "名", title = "名前を探す", onOpenSettings = onOpenSettings,
            modifier = Modifier.weight(1f),
        ) {
            TotalCard(
                title = if (sei != null) "総格" else "総画数",
                text = form.totalText,
                onTextChange = { form = form.copy(totalText = it) },
                onDone = { focusManager.clearFocus() },
            ) {
                if (registeredSei != null) {
                    SeiRow(registeredSei, usesSei, data) { usesSei = it }
                } else {
                    SeiPrompt(onOpenSettings)
                }
            }
            // カタカナは、入力を終えたとき（完了・この条件で探す）にひらがなに直す。打っている途中で直すと、
            // 日本語入力の変換を邪魔してしまうため
            ReadingField(
                reading,
                onChange = { reading = it },
                onDone = {
                    reading = SearchForm.hiragana(reading)
                    focusManager.clearFocus()
                },
            )
            if (sei != null) {
                OtherKakuSection(
                    expanded = showsOtherKaku,
                    onToggle = { showsOtherKaku = !showsOtherKaku },
                    texts = form.otherKakuTexts,
                    onChange = { kind, text -> form = form.copy(otherKakuTexts = form.otherKakuTexts + (kind to text)) },
                    onDone = { focusManager.clearFocus() },
                )
            }
            LengthSection(form.length) { form = form.withLength(it) }
            NameShapeSection(
                form = form,
                onSelect = { form = form.toggleSelection(it) },
                onChange = { change -> form = form.updateSelected(change) },
                onDone = { focusManager.clearFocus() },
            )
        }
        SubmitBar(
            problem = (check as? FormResult.Invalid)?.message,
            onSubmit = ::submit,
        )
    }
}

// MARK: - 総画数（総格）のカード

/** 一番上の藍のカード: 総画数（姓を使うなら総格）。値は 1 つだけで、真ん中に大きく出す。 */
@Composable
private fun TotalCard(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
    footer: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Wa.accent, RoundedCornerShape(Wa.Metrics.largeCornerRadius))
            .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = Wa.gothic(13.sp, FontWeight.Bold).copy(letterSpacing = 2.sp), color = Wa.onAccent)
        val style = Wa.mincho(64.sp, bold = true).copy(color = Wa.onAccent, textAlign = TextAlign.Center)
        BasicTextField(
            value = text,
            // 数字だけ、3 桁まで
            onValueChange = { onTextChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Wa.onAccent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        Wa.onAccent.copy(alpha = 0.4f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(bottom = 4.dp)
                .testTag("search.total")
                .semantics { contentDescription = title },
            // 数字はカードの真ん中、「画」は右の端に置く
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (text.isEmpty()) {
                        Text("20", style = style.copy(color = Wa.onAccentPlaceholder), modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                    // 「画」は飾り。入力した値と一緒に読み上げない
                    Text(
                        "画", style = Wa.mincho(24.sp), color = Wa.onAccentFaint,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 14.dp).clearAndSetSemantics { },
                    )
                }
            },
        )
        footer()
    }
}

/** 姓が未登録のときの行: 姓を登録すると総格で探せることと、設定画面へ進むボタン。 */
@Composable
private fun SeiPrompt(onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("姓を登録すると、総格で探せます", style = Wa.gothic(13.sp), color = Wa.onAccentFaint, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .defaultMinSize(minHeight = Wa.Metrics.minTapSize)
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .padding(horizontal = 4.dp)
                .testTag("search.setSei"),
            contentAlignment = Alignment.Center,
        ) {
            Text("姓を設定する ›", style = Wa.gothic(13.sp, FontWeight.Bold), color = Wa.onAccent)
        }
    }
}

/** 読みの欄。ひらがなで入れる（カタカナで入れても、入力を終えたときにひらがなに直す）。空なら読みで絞り込まない。 */
@Composable
private fun ReadingField(reading: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).waCard().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("読み", style = Wa.gothic(15.sp, FontWeight.Bold).copy(letterSpacing = 1.sp), color = Wa.ink)
        BasicTextField(
            value = reading,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Wa.mincho(18.sp).copy(color = Wa.ink, textAlign = TextAlign.End),
            cursorBrush = SolidColor(Wa.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.weight(1f).testTag("search.reading").semantics { contentDescription = "読み" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (reading.isEmpty()) Text("省略できます（例：はると）", style = Wa.mincho(18.sp), color = Wa.placeholder)
                    inner()
                }
            },
        )
    }
}

/** 登録した姓の行: 「姓「田中」・天格 9画」と「姓を使う」。 */
@Composable
private fun SeiRow(registered: String, usesSei: Boolean, data: KanjiData, onUsesSeiChange: (Boolean) -> Unit) {
    val check = remember(registered, data) { SeiCheck.of(registered, data) }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "姓「$registered」",
                    style = Wa.gothic(13.sp, FontWeight.Bold),
                    color = Wa.onAccent.copy(alpha = if (usesSei) 1f else 0.6f),
                    modifier = Modifier.testTag("search.sei"),
                )
                if (usesSei && check is SeiCheck.Tenkaku) {
                    Text("・天格", style = Wa.gothic(13.sp, FontWeight.Bold), color = Wa.onAccent)
                    Text("${check.value}画", style = Wa.mincho(17.sp, bold = true), color = Wa.onAccent, modifier = Modifier.testTag("search.tenkaku"))
                }
            }
            if (usesSei && check is SeiCheck.Problem) {
                Text(check.message, style = Wa.gothic(13.sp), color = Wa.onAccent, modifier = Modifier.testTag("search.tenkaku.error"))
            }
        }
        WaCheckbox("姓を使う", usesSei, onUsesSeiChange, color = Wa.onAccent, modifier = Modifier.testTag("search.useSei"))
    }
}

/** 姓ありのとき、折りたたみの中で人格・地格・外格を条件にする。 */
@Composable
private fun OtherKakuSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    texts: Map<KakuKind, String>,
    onChange: (KakuKind, String) -> Unit,
    onDone: () -> Unit,
) {
    val notes = mapOf(
        KakuKind.JINKAKU to ("人格" to "姓の末尾＋名の先頭"),
        KakuKind.CHIKAKU to ("地格" to "名の合計"),
        KakuKind.GAIKAKU to ("外格" to "総格−人格"),
    )
    Column(Modifier.fillMaxWidth().waCard()) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 16.dp)
                .testTag("search.otherKaku")
                .semantics { stateDescription = if (expanded) "開いている" else "閉じている" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ほかの格も条件にする（人格・地格・外格）", style = Wa.gothic(15.sp), color = Wa.ink, modifier = Modifier.weight(1f))
            Text(if (expanded) "︿" else "﹀", style = Wa.gothic(14.sp, FontWeight.Bold), color = Wa.accent)
        }
        if (expanded) {
            for ((index, kind) in SearchForm.OTHER_KAKU_KINDS.withIndex()) {
                val (title, note) = notes.getValue(kind)
                androidx.compose.material3.HorizontalDivider(color = Wa.divider, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text(title, style = Wa.gothic(15.sp, FontWeight.Bold), color = Wa.ink)
                        Text(note, style = Wa.gothic(11.sp), color = Wa.secondaryText)
                    }
                    NumberField(
                        value = texts[kind] ?: "",
                        onValueChange = { onChange(kind, it) },
                        placeholder = "例: 15",
                        textStyle = Wa.mincho(20.sp, bold = true).copy(color = Wa.ink, textAlign = TextAlign.End),
                        placeholderColor = Wa.placeholder,
                        cursor = Wa.accent,
                        label = title,
                        tag = "search.kaku.${kind.raw}",
                        onDone = onDone,
                        modifier = Modifier.weight(1f),
                    )
                    Text("画", style = Wa.gothic(16.sp), color = Wa.secondaryText)
                }
                if (index == SearchForm.OTHER_KAKU_KINDS.lastIndex) Spacer(Modifier.height(4.dp))
            }
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
        // マスを選んだときだけ、そのマスの条件（自由／画数指定／固定）の欄を出す
        val selected = form.selected
        if (selected == null) {
            Text("マスを押すと、その文字の条件を選べます", style = Wa.gothic(12.sp), color = Wa.secondaryText)
        } else {
            KindTabs(selected.kind) { kind -> onChange { it.copy(kind = kind) } }
            PositionDetail(selected, onChange, onDone)
        }
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
    problem: String?,
    onSubmit: () -> Unit,
) {
    val enabled = problem == null
    Column(
        Modifier
            .fillMaxWidth()
            .background(Wa.background)
            .padding(horizontal = Wa.Metrics.screenPadding)
            .padding(top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 押せないときは、その理由（足りない入力など）をボタンの上に出す
        if (problem != null) {
            Text(problem, style = Wa.gothic(13.sp), color = Wa.secondaryText, modifier = Modifier.fillMaxWidth().testTag("search.formError"))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(Wa.Metrics.primaryButtonHeight)
                .background(Wa.accent.copy(alpha = if (enabled) 1f else 0.35f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onSubmit)
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
        listOf<Any>(form.length, form.selectedIndex ?: -1, form.totalText) +
            SearchForm.OTHER_KAKU_KINDS.map { form.otherKakuTexts[it] ?: "" } +
            form.positions.flatMap { listOf(it.kind.name, it.script.name, it.strokesText, it.fixedText) }
    },
    restore = { list ->
        val others = SearchForm.OTHER_KAKU_KINDS.withIndex().associate { (i, kind) -> kind to (list[3 + i] as String) }
        val positions = list.drop(3 + SearchForm.OTHER_KAKU_KINDS.size).chunked(4).map {
            PositionInput(
                kind = PositionKind.valueOf(it[0] as String),
                script = ScriptFilter.valueOf(it[1] as String),
                strokesText = it[2] as String,
                fixedText = it[3] as String,
            )
        }
        SearchForm(
            length = list[0] as Int, positions = positions, selectedIndex = (list[1] as Int).takeIf { it >= 0 }, totalText = list[2] as String,
            otherKakuTexts = others,
        )
    },
)
