package com.rkawa.kakunote.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkawa.kakunote.data.Attribution
import com.rkawa.kakunote.data.CharList
import com.rkawa.kakunote.data.SeiCheck
import com.rkawa.kakunote.logic.KanjiData
import com.rkawa.kakunote.ui.components.WaTopBar
import com.rkawa.kakunote.ui.components.waContentWidth
import com.rkawa.kakunote.ui.components.waCard
import com.rkawa.kakunote.ui.theme.Wa

/**
 * 設定画面: 姓の登録、使いたい字、避けたい字、読みの頭の部分、「このアプリについて」。
 * 「閉じる」と端末の戻る操作で閉じる。「このアプリについて」は設定画面の中で進む。
 */
@Composable
fun SettingsScreen(settings: AppSettings, data: KanjiData, onClose: () -> Unit) {
    var showsAbout by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (showsAbout) showsAbout = false else onClose() }
    Column(Modifier.fillMaxSize().background(Wa.background).safeDrawingPadding().waContentWidth()) {
        if (showsAbout) {
            AboutScreen(data, onBack = { showsAbout = false })
        } else {
            SettingsContent(settings, data, onClose, onOpenAbout = { showsAbout = true })
        }
    }
}

@Composable
private fun SettingsContent(settings: AppSettings, data: KanjiData, onClose: () -> Unit, onOpenAbout: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = Wa.Metrics.screenPadding, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "設定",
                style = Wa.mincho(22.sp, bold = true),
                color = Wa.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onClose, modifier = Modifier.testTag("settings.close")) {
                Text("閉じる", color = Wa.accent, style = Wa.gothic(16.sp, FontWeight.Bold))
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section("姓", footer = "登録した姓は「探す」と「調べる」で使います。空にすると、名前だけで探します。") {
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("姓", style = Wa.gothic(15.sp, FontWeight.Bold), color = Wa.ink, modifier = Modifier.width(38.dp))
                    val sei = settings.values.sei
                    BasicTextField(
                        value = sei,
                        onValueChange = settings::setSei,
                        singleLine = true,
                        textStyle = Wa.mincho(20.sp).copy(color = Wa.ink, textAlign = TextAlign.End),
                        cursorBrush = SolidColor(Wa.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f).testTag("settings.sei").semantics { contentDescription = "姓" },
                        decorationBox = { inner ->
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.CenterEnd) {
                                if (sei.isEmpty()) Text("例：田中", style = Wa.mincho(20.sp), color = Wa.placeholder)
                                inner()
                            }
                        },
                    )
                }
                settings.registeredSei?.let { registered ->
                    HorizontalDivider(color = Wa.divider)
                    val check = remember(registered, data) { SeiCheck.of(registered, data) }
                    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (check) {
                            is SeiCheck.Tenkaku -> {
                                Text("天格", style = Wa.gothic(15.sp), color = Wa.ink)
                                Spacer(Modifier.weight(1f))
                                Text("${check.value}画", style = Wa.mincho(18.sp, bold = true), color = Wa.accent, modifier = Modifier.testTag("settings.tenkaku"))
                            }
                            is SeiCheck.Problem -> Text(check.message, style = Wa.gothic(13.sp), color = Wa.seal, modifier = Modifier.testTag("settings.seiError"))
                        }
                    }
                }
            }
            CharListSection(
                settings, data, CharList.USE, "使いたい字", "例：陽", "settings.use",
                footer = "どれか 1 字以上が名前のどこかに入っている候補だけを出します。名前に使える字だけを登録できます。避けたい字にも入っている字は、使いたい字を優先します。",
            )
            CharListSection(
                settings, data, CharList.AVOID, "避けたい字", "例：乙", "settings.avoid",
                footer = "自由・画数指定の位置の候補から外します。自分で固定した字は外しません。漢字・ひらがな・カタカナを、何文字でもまとめて入れられます。",
            )
            Section(
                "読み",
                footer = "「探す」で読みを入れたとき、字の読みの先頭の部分も認めます（例：心「こころ」→「こ」で、心春を「こはる」）。小さい字（ゃ・ゅ・ょ など）の手前では区切りません。候補が大きく増えます。",
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .toggleable(
                            value = settings.values.readingPrefix,
                            role = Role.Switch,
                            onValueChange = settings::setReadingPrefix,
                        )
                        .padding(horizontal = 16.dp)
                        .testTag("settings.readingPrefix"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("読みの頭の部分も使う", style = Wa.gothic(16.sp), color = Wa.ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.values.readingPrefix,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Wa.accent, checkedThumbColor = Wa.onAccent,
                            uncheckedTrackColor = Wa.surfaceMuted, uncheckedThumbColor = Wa.secondaryText, uncheckedBorderColor = Wa.border,
                        ),
                    )
                }
            }
            settings.errorMessage?.let { Text(it, style = Wa.gothic(13.sp), color = Wa.seal) }
            Section(null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(onClick = onOpenAbout)
                        .padding(horizontal = 16.dp)
                        .testTag("settings.about"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("このアプリについて", style = Wa.gothic(16.sp), color = Wa.ink, modifier = Modifier.weight(1f))
                    Text("›", style = Wa.gothic(20.sp, FontWeight.Bold), color = Wa.dashed)
                }
            }
        }
    }
}

/** 使いたい字・避けたい字の欄: 入力して「追加」、登録した字はチップで並び、押すと外れる。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharListSection(
    settings: AppSettings,
    data: KanjiData,
    list: CharList,
    title: String,
    placeholder: String,
    tag: String,
    footer: String,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    fun add() {
        if (input.isBlank()) return
        message = settings.addChars(input, list, data)
        if (message == null) input = ""
    }
    Section(title, footer = footer) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = Wa.mincho(20.sp).copy(color = Wa.ink),
                cursorBrush = SolidColor(Wa.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add(); focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).testTag("$tag.input").semantics { contentDescription = title },
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box {
                        if (input.isEmpty()) Text(placeholder, style = Wa.mincho(20.sp), color = Wa.placeholder)
                        inner()
                    }
                },
            )
            TextButton(onClick = ::add, enabled = input.isNotBlank(), modifier = Modifier.testTag("$tag.add")) {
                Text("追加", style = Wa.gothic(15.sp, FontWeight.Bold), color = if (input.isNotBlank()) Wa.accent else Wa.placeholder)
            }
        }
        message?.let {
            Text(it, style = Wa.gothic(13.sp), color = Wa.seal, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).testTag("$tag.error"))
        }
        val chars = settings.values.chars(list)
        if (chars.isNotEmpty()) {
            HorizontalDivider(color = Wa.divider)
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (char in chars) {
                    Row(
                        Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .background(Wa.surfaceMuted, CircleShape)
                            .clickable(role = Role.Button) { settings.removeChar(char, list) }
                            .padding(horizontal = 12.dp)
                            .testTag("$tag.remove.$char")
                            .semantics(mergeDescendants = true) { contentDescription = "「$char」を外す" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(char, style = Wa.mincho(18.sp, bold = true), color = Wa.ink)
                        Text("×", style = Wa.gothic(13.sp, FontWeight.Bold), color = Wa.secondaryText)
                    }
                }
            }
        }
    }
}

/** 設定画面の 1 区切り: 小見出し、カード、下の説明。 */
@Composable
fun Section(title: String?, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        title?.let { Text(it, style = Wa.gothic(13.sp, FontWeight.Bold), color = Wa.secondaryText, modifier = Modifier.padding(horizontal = 4.dp)) }
        Column(Modifier.fillMaxWidth().waCard(), content = content)
        footer?.let { Text(it, style = Wa.gothic(12.sp).copy(lineHeight = 18.sp), color = Wa.secondaryText, modifier = Modifier.padding(horizontal = 4.dp)) }
    }
}

/** 「このアプリについて」: 漢字データの出典・ライセンス・バージョン。設定画面から開く。 */
@Composable
fun AboutScreen(data: KanjiData, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sources = remember(data) { Attribution.sources(data.meta) }
    Column(Modifier.fillMaxSize()) {
        WaTopBar("このアプリについて", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Wa.Metrics.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            for (source in sources) {
                Section(source.name) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(source.description, style = Wa.gothic(15.sp).copy(lineHeight = 23.sp), color = Wa.ink)
                        Row {
                            Text("データのバージョン", style = Wa.gothic(15.sp), color = Wa.ink, modifier = Modifier.weight(1f))
                            Text(source.version, style = Wa.gothic(15.sp), color = Wa.secondaryText, modifier = Modifier.testTag("about.${source.name}.version"))
                        }
                        Text(source.notice, style = Wa.gothic(13.sp).copy(lineHeight = 20.sp), color = Wa.secondaryText)
                        for (link in source.links) {
                            Text(
                                link.title,
                                style = Wa.gothic(15.sp),
                                color = Wa.accent,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp).clickable { uriHandler.openUri(link.url) }.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
