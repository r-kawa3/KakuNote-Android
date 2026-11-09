# KakuNote（Android）

名前の画数（五格）を調べたり、画数の条件に合う名前を探したりする Android アプリです。

[KakuNote（iOS）](https://github.com/r-kawa3/KakuNote) の Android 版です。漢字データベースと `testcases/` のテストケースは iOS 版と同じものです。

## できること

| タブ | 内容 |
|---|---|
| 調べる | 姓と名を入力すると、1 文字ずつの画数と五格（天格・人格・地格・外格・総格）を出す |
| 探す | 名の文字数・各文字の条件（この字にする／何画の字にする／自由）・格の条件などから、合う名前を決まった順番で一覧にする。読み・使いたい字・避けたい字でも絞り込める |
| お気に入り | 気に入った名前を保存する |

設定画面と、漢字データの出典を示す「このアプリについて」もあります。

## 技術

- Kotlin・Jetpack Compose（Material 3）、Android 10（API 29）以上
- 画面の状態: ViewModel・SavedStateHandle・rememberSaveable
- 画数の計算と名前の検索: `logic/`（Android の API を使わない Kotlin）
- 漢字データ: SQLite（`assets/kanji.sqlite`）。読み取り専用で開く
- お気に入り: 漢字データとは別のデータベースに保存
- テスト: JUnit（JVM のユニットテスト）、Compose の UI テスト

## ビルドとテスト

必要なもの: Android Studio（JDK 17 以上）

```bash
# ユニットテスト（JVM の上で動く）
./gradlew testDebugUnitTest

# 端末やエミュレータで動かすテスト（つないだ端末が要る）
./gradlew connectedDebugAndroidTest

# アプリを作る
./gradlew assembleDebug
```

## ディレクトリ構成

```
app/src/main/java/com/rkawa/kakunote/
  logic/        画数の計算・名前の検索など（Android から独立）
  data/         漢字データベースの読み込み・お気に入り・設定の保存
  ui/           画面（lookup・search・result・favorites・settings）と部品・テーマ
app/src/main/assets/kanji.sqlite   同梱する漢字データベース（iOS 版と同じもの）
app/src/test/         ユニットテスト
app/src/androidTest/  端末で動かすテスト
testcases/            iOS 版と Android 版で共通のテストケース
```

漢字データベースと `testcases/` は、iOS 版のリポジトリで作ったものをコピーしています。作り方は [KakuNote（iOS）の README](https://github.com/r-kawa3/KakuNote#漢字データベースを作り直す) を見てください。

## ライセンス

- **ソースコード**: [MIT License](LICENSE)
- **漢字データ**（`app/src/main/assets/kanji.sqlite`）: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)

漢字の画数・読み・常用漢字と人名用漢字の区別には、Electronic Dictionary Research and Development Group（[EDRDG](https://www.edrdg.org/)）の [KANJIDIC2](https://www.edrdg.org/wiki/index.php/KANJIDIC_Project) を使っています。KANJIDIC2 は EDRDG の財産で、[同グループのライセンス](https://www.edrdg.org/edrdg/licence.html)に従って使っています。このリポジトリの漢字データは KANJIDIC2 をもとに作ったもので、同じ CC BY-SA 4.0 のもとで提供します。
