# EndraRTA v1 仕様

## タスク情報
- 対象: Fabric Minecraft 26.1.2 mod
- 使用Skill: jp-coding-rules
- 実装範囲: v1 段階実装
- 実行前提: Java 25

## 範囲
- 練習/競技モードを設定で切り替える。
- v1ではタイマー、HUD、村スポーン、鉱石焼き不要、エンダーアイ破損防止、基本レーダー、クイックリセットを実装する。
- v2以降のワールドプレビュー、マルチインスタンス管理は設定項目と仕様上の予約に留める。

## 制約
- 競技モードでは補助/チート扱いになり得る機能を既定OFFにする。
- 設定JSONを読めるようにする。
- IDEのclient依存解決問題を避けるため、設定画面はMod Menu APIに直接依存せずキー割当から開く。
- シングルプレイRTAを主対象にし、クライアントHUDの構造物レーダーは統合サーバー情報が読める場合に表示する。

## 受け入れ条件
- 新規ワールドで練習モード時に最寄り村へ初回移動する。
- 鉄、金、銅の原石ドロップが対応するインゴットへ置換される。
- エンダーアイが飛翔終了時に壊れず、アイテムとして落ちる。
- RTA/IGTが開始し、ネザー入場、ネザー要塞発見、エンド要塞発見、エンド入場、ドラゴン撃破をスプリット記録する。
- ドラゴン撃破時にタイマーを停止する。
- HUDにRTA/IGT、最新スプリット、バイオーム、ライトレベル、座標換算、残りエンドクリスタル数、要塞レーダーを表示する。
- バイオーム名は日本語表記と標準英語表記を3秒ごとに交互表示する。
- F3デバッグ画面を開いている間は、円グラフ表示中でもEndrRTA HUDを描画しない。
- クイックリセットキーで確認画面を表示し、シングルプレイワールドから切断できる。
- 設定キーでEndrRTA設定画面を開ける。

## 対象外
- v1ではロード前ワールドプレビューを実装しない。
- v1では複数ワールドの並列生成や外部プロセス制御を実装しない。
- 公認競技での使用可否は保証しない。

## 2026-06-06 Java 25 toolchain 変更タスク

### タスク情報
- 対象: Gradle Java ビルド設定
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- Gradle の Java toolchain を Java 25 に明示する。
- Java 25 toolchain の自動取得用 resolver を設定する。
- `--release 25` によるコンパイル互換性チェックを維持する。
- 既存の Fabric/Mixin の Java 25 要件と整合させる。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- Minecraft/Fabric の依存バージョンは変更しない。
- IDE 固有設定やユーザー環境変数はリポジトリ設定として固定しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- Gradle がプロジェクトのコンパイル用 JDK として Java 25 toolchain を要求する。
- Java 25 が未検出の場合は configured resolver から toolchain を取得できる。
- Java 25 がインストールされた環境では `gradlew.bat build --warning-mode all` を実行できる。
- Java 25 未導入の環境では不足 JDK が原因だと判別できる。

### 対象外
- ローカル端末への JDK 25 インストール。
- Java 25 非対応 JDK でのビルド成功保証。

### 検証メモ
- `gradlew.bat -q javaToolchains`: Java 25 toolchain として Eclipse Temurin JDK 25 (25.0.2+10-LTS) を検出。
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。

## 2026-06-06 日本語化タスク

### タスク情報
- 対象: mod のユーザー向け表示文言
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- mod メタデータの説明文を日本語化する。
- HUD、設定画面、クイックリセット確認、キー設定名を日本語で表示する。
- キー設定名は Minecraft の言語ファイル経由で表示できるようにする。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- mod ID、パッケージ名、内部設定キー、ログ名は変更しない。
- RTA/IGT、HUD など一般的な略語は読みやすさを優先して必要最小限だけ残す。
- ゲーム内機能や設定の意味は変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- mod 一覧の説明文が日本語で読める。
- 操作設定のキー名が日本語で表示される。
- HUD の英語ラベルが日本語ラベルに置き換わっている。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- mod 名 `EndrRTA` のブランド名変更。
- Minecraft 本体や Fabric API が表示する既存文言の翻訳変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- ビルド後の `fabric.mod.json` に日本語説明文と `version=2.0.5` が反映されていることを確認。
- `ja_jp.json` と `en_us.json` にキー設定名の日本語文言が含まれていることを確認。

## 2026-06-06 見た目改善タスク

### タスク情報
- 対象: HUD と EndrRTA 設定画面
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- HUD に濃淡背景、枠線、アクセントバー、状態別テキスト色を追加する。
- HUD の表示行を「タイマー」「最新」「座標」などの読みやすいラベルに整理する。
- 設定画面に半透明パネル、アクセントバー、説明文を追加して見た目を整える。
- 設定画面の一部ラベルを短く自然な日本語に調整する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- Minecraft/Fabric の描画 API 範囲内で実装し、新規描画ライブラリは追加しない。
- mod 機能や設定保存形式は変更しない。
- 低解像度でもボタンが操作しにくくならないよう、既存の2列構成を維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- HUD が単色矩形ではなく、枠線とアクセント色で情報を区別できる。
- 競技モードやベッド危険表示など注意が必要な行が警告色で表示される。
- 設定画面にパネル背景と説明文が表示され、項目が読みやすい。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- アイコン画像やフォントの追加。
- 設定項目の増減や挙動変更。
- Minecraft 本体 UI のテーマ変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `GuiGraphicsExtractor` の `fillGradient` と `outline` を使用し、追加ライブラリなしで実装。
- `mod_version=2.0.6` が反映されていることを確認。

## 2026-06-06 ポーズ画面シードリセットボタン追加タスク

### タスク情報
- 対象: ESC で開くポーズ画面
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- シングルプレイのポーズ画面に `シードリセット` ボタンを追加する。
- ボタン押下時は既存のクイックリセット処理を再利用し、確認後に現在のワールドを終了する。
- 確認画面でキャンセルした場合はポーズ画面へ戻す。
- クライアント専用 Mixin 設定を分け、専用言語キーを追加する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ワールド自動生成や外部プロセス制御は実装しない。
- マルチプレイではシードリセットの意味が曖昧なため、ボタンはシングルプレイ時のみ表示する。
- 既存のキーバインドによるクイックリセット挙動は維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- シングルプレイ中に ESC を押すと `シードリセット` ボタンが表示される。
- ボタン押下後、確認で承認すると現在のワールドを終了してリセット処理を開始する。
- 確認でキャンセルするとポーズ画面に戻る。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 新規シードのワールドを自動作成して即入場する処理。
- マルチプレイ用の切断ボタン追加。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `endrrta-2.0.7.jar` に `endrrta.client.mixins.json` と `PauseScreenMixin.class` が含まれていることを確認。
- ビルド後の `fabric.mod.json` に `endrrta.client.mixins.json` と `version=2.0.7` が反映されていることを確認。

## 2026-06-06 設定画面カテゴリ分割タスク

### タスク情報
- 対象: EndrRTA 設定画面
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- 設定画面を `基本`、`練習補助`、`HUD`、`リセット` のカテゴリに分ける。
- 上部にカテゴリ切替ボタンを配置し、選択中カテゴリの設定項目だけを表示する。
- 既存の設定項目と保存挙動は維持する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 設定JSONの構造は変更しない。
- 新しい設定項目は追加しない。
- 低解像度でも操作しやすいよう、表示項目数をカテゴリごとに抑える。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面上部にカテゴリ切替が表示される。
- `基本` にはモード、HUD表示、自動スプリットがまとまっている。
- `練習補助` には村移動、鉱石自動精錬、エンダーアイ保護、レーダー、ベッド爆破支援がまとまっている。
- `HUD` には座標換算、バイオーム、明るさ、クリスタル数がまとまっている。
- `リセット` にはリセット確認がまとまっている。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 設定値の型や保存形式の変更。
- 数値設定用 UI の追加。
- Mod Menu API への直接依存。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.8` が反映されていることを確認。

## 2026-06-06 診断修正タスク

### タスク情報
- 対象: IDE 診断に表示された Java/Mixin 警告・エラー
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- HUD、設定画面、データ生成、サーバー状態、設定読み込みの nullable 注釈警告を解消する。
- Mixin の target / shadow / accessor マッピング未解決診断を解消する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 実行時の Mixin リマップを壊す `remap = false` は、実対象がリマップ不要でない限り使わない。
- 競技/練習モードや既存 HUD 表示内容の仕様は変えない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 添付診断の対象ファイルで nullable 由来の警告が再発しにくいコード構造になっている。
- Mixin annotation processor が対象メソッド/フィールドを解決できる設定またはコードになっている。
- Java 25 環境では Gradle ビルドを実行できる状態である。

### 対象外
- Minecraft 本体や Fabric API 側の JDK 25 依存警告を JDK 24 以下で解消すること。
- ゲーム内挙動や UI レイアウトの仕様変更。

### 検証メモ
- `gradlew.bat dependencies --configuration compileClasspath --warning-mode all`: 成功。
- `gradlew.bat clean build --warning-mode all`: Java 25 toolchain 設定後に成功。警告なし。

## 2026-06-07 直接新規ワールドリセットタスク

### タスク情報
- 対象: クイックリセットとポーズ画面のシードリセット
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- リセット承認後にタイトル画面へ戻らず、現在のワールドを終了して新しい通常ワールドを直接作成する。
- 新規ワールドはサバイバル、通常難易度、ランダムシード、通常ワールド生成で作成する。
- ロード中は Minecraft 標準の `地形情報を読み込み中...` 画面を表示する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ワールド生成設定画面や外部インスタンス管理は追加しない。
- マルチプレイではリセット処理を開始しない既存条件を維持する。
- 設定JSONの構造は変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- クイックリセットまたは `シードリセット` ボタンを承認すると、タイトル画面ではなく新しいワールドのロードへ進む。
- 作成されるワールド名が既存ワールドと衝突しにくい。
- キャンセル時は元のキャンセル先画面へ戻る。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- ワールド候補の事前プレビュー。
- 複数ワールドの並列生成。
- 既存ワールドの削除や自動整理。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `endrrta-2.0.9.jar` に `SeedResetWorldStarter.class` と `QuickResetHandler.class` が含まれていることを確認。
- ビルド後の `fabric.mod.json` に `version=2.0.9` が反映されていることを確認。

## 2026-06-07 HUD再デザインとワールド切替リセット修正タスク

### タスク情報
- 対象: EndrRTA HUD とサーバー側ラン状態
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- HUD をヘッダー、タイマー、詳細行に分け、情報の優先順位が分かる見た目へ調整する。
- 強い黄色枠を控え、影、控えめな枠線、上部アクセント、ラベル/値の色分けを使う。
- バイオーム表示は `minecraft:` 名前空間と `_` をそのまま出さず、読みやすい表示にする。
- ワールド終了時に RTA/IGT、スプリット、レーダー、村スポーン済み状態をリセットする。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- Minecraft/Fabric の既存描画 API の範囲で実装し、新規ライブラリは追加しない。
- HUD の表示項目や設定項目の意味は変更しない。
- マルチプレイで統合サーバー状態が読めない場合は HUD のラン状態を表示しない既存方針を維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- スクリーンショットのような太い黄色外枠が目立ちすぎないデザインになる。
- タイマー、最新スプリット、座標、バイオーム、明るさ、レーダーが読み分けやすい。
- ワールドを変更した後、前ワールドのタイマー/スプリット/レーダー情報が残らない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- HUD のドラッグ移動や位置設定。
- アイコン画像、独自フォント、アニメーションの追加。
- 既存ワールドデータの削除や整理。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.10` が反映されていることを確認。
- サーバー停止イベントで `EndrRTAServerState.clear()` を呼び、ワールド切替時にラン状態を破棄する実装にした。

## 2026-06-07 初期スポーン高さ補正タスク

### タスク情報
- 対象: 練習モードの `村へ初期移動`
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 村スポーン補正で、対象チャンクを生成/読み込みしてから地表Y座標を取得する。
- 取得したY座標がワールド下限付近の場合は岩盤付近へのテレポートを行わない。
- テレポート中止時はプレイヤーに日本語メッセージを表示する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 村検索の仕様、検索半径、設定JSON構造は変更しない。
- 通常のワールド生成やシードリセット処理は変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 新規ワールド開始時に、村スポーン補正で岩盤付近へ飛ばされない。
- 村位置の地表高さが安全に取得できない場合は、無理にテレポートしない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 完全に安全な村内スポーン地点探索。
- 村以外の初期スポーン補正。
- 既存のワールドや設定ファイルの移行。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.11` が反映されていることを確認。
- 村位置チャンクを `ChunkStatus.FULL` で読み込んでから Heightmap を参照する実装にした。

## 2026-06-07 円グラフ操作補助タスク

### タスク情報
- 対象: F3 円グラフ/プロファイラを使う練習補助
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- 円グラフを専用キー1つで開閉できるようにする。
- 円グラフの階層を1つ戻るキーと root まで戻るキーを追加する。
- 円グラフ表示中は HUD に簡単な操作ヒントを表示する。
- 練習補助として扱い、競技モードでは操作補助を無効にする。
- 設定画面に円グラフ補助の ON/OFF を追加する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- Minecraft 本体のプロファイラ結果や項目名は変更しない。
- 数字キーで項目へ入る基本操作は本体機能を使う。
- 公認競技での使用可否は保証しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 専用キーで F3 を押さずに円グラフを開閉できる。
- 専用キーで円グラフ階層を1つ戻せる。
- 専用キーで円グラフ階層を root へ戻せる。
- 競技モードでは補助キーが円グラフ操作を行わない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 特定構造物の自動探索や自動判定。
- プロファイラ項目の自動選択。
- 円グラフ自体の描画変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.12` が反映されていることを確認。
- `PieChartAssistHandler` で円グラフ開閉、1階層戻る、root復帰を実装した。

## 2026-06-07 円グラフクリック選択タスク

### タスク情報
- 対象: F3 円グラフ/プロファイラの項目選択
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 円グラフ補助の表示中にカーソルを出し、リスト行を左クリックで選択できるようにする。
- 右クリックで1階層戻れるようにする。
- Esc または円グラフ開閉キーで円グラフ補助を閉じられるようにする。
- 数字キーによる既存選択も補助スクリーン中は維持する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- Minecraft 本体の円グラフ描画やプロファイラ結果は変更しない。
- クリック選択は練習モードかつ円グラフ補助ONのときだけ有効にする。
- 補助スクリーンはゲームを停止しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 円グラフの `[1] entities` などの行をクリックすると、その項目へ入れる。
- 右クリックで1階層戻れる。
- Esc または開閉キーで円グラフ補助を閉じられる。
- 競技モードではクリック選択補助が有効にならない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- プロファイラ項目の自動選択。
- 円グラフの独自描画化。
- 構造物探索の自動化。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.13` が反映されていることを確認。
- `PieChartAssistScreen` でカーソル表示中の左クリック選択、右クリック戻り、Esc/開閉キー終了を実装した。

## 2026-06-07 走行中円グラフ選択タスク

### タスク情報
- 対象: 円グラフ補助の操作方式
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 円グラフ補助で専用スクリーンを開かず、通常のゲーム操作中に選択できるようにする。
- マウスホイールで円グラフ項目を選び、左クリックまたは中クリックで選択する。
- 右クリックで1階層戻れるようにする。
- HUD に現在選択中の項目と操作ヒントを表示する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- WASD移動やダッシュを止める画面を開かない。
- 円グラフ補助ONかつ練習モードのときだけマウス操作を補助に使う。
- 円グラフ補助中のクリック/ホイールは通常の攻撃、使用、ホットバー切替より補助操作を優先する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 円グラフ表示中も走りながらホイールで項目を選べる。
- 左クリックまたは中クリックで現在選択中の項目へ入れる。
- 右クリックで1階層戻れる。
- 補助スクリーンが開かず、ゲームが停止しない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- カーソルクリックによる行選択。
- プロファイラ項目の自動選択。
- 円グラフ描画そのものの差し替え。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.14` が反映されていることを確認。
- `MouseHandlerMixin` で円グラフ表示中のホイール選択、左/中クリック決定、右クリック戻りを実装した。
- 走行を止める `PieChartAssistScreen` は削除した。

## 2026-06-07 モブ補助設定とHUD再設計タスク

### タスク情報
- 対象: 練習補助設定、Entityドロップ、EndrRTA HUD
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- ブレイズがブレイズロッドを最低1個ドロップする設定を追加する。
- エンダーマンがエンダーパールを最低1個ドロップする設定を追加する。
- エンダーマンがブロックを持たない設定を追加する。
- F3デバッグ表示中はEndrRTA HUDを非表示にする。
- HUDをタイマー重視の横長パネルへ大幅に再設計する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 追加補助は練習モードでのみ有効にする。
- 既存のLootテーブル本体やMinecraft本体の表示は変更しない。
- F3円グラフ補助の操作は維持する。
- 設定JSONは既存形式にboolean項目を追加するだけにする。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定ONの練習モードで、ブレイズ討伐時にブレイズロッドが最低1個出る。
- 設定ONの練習モードで、エンダーマン討伐時にエンダーパールが最低1個出る。
- 設定ONの練習モードで、エンダーマンがブロックを保持しない。
- F3デバッグ表示中はEndrRTA HUDが重ならない。
- HUDの見た目が現行の縦リスト型から明確に変わる。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- モブのスポーン率やAI全般の変更。
- Lootingなどエンチャント計算の再実装。
- HUD位置のドラッグ移動。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.15` が反映されていることを確認。
- `EntityDropTransformer` でブレイズロッド/エンダーパールの最低1個ドロップを保証する実装にした。
- `EnderManMixin` でエンダーマンのブロック保持を抑止する実装にした。
- HUDはRTA/IGTカードと2列情報グリッドへ再設計し、F3デバッグ表示中は非表示にした。

## 2026-06-07 シードリセット用ワールド設定タスク

### タスク情報
- 対象: シードリセット時の新規ワールド作成設定と設定画面カテゴリ
- 使用Skill: jp-coding-rules, ja-copy-quality
- 変更区分: PATCH

### 範囲
- シードリセットで作る新規ワールドに、構造物生成、ボーナスチェスト、チート許可、ハードコアの設定を追加する。
- 追加設定を `EndrRTAConfig` に保存する。
- 設定画面カテゴリを `基本`、`初期補助`、`ドロップ`、`エンダー`、`HUD`、`円グラフ`、`リセット`、`ワールド` に細分化する。
- カテゴリ数増加に合わせ、設定画面のタブを2段表示にする。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ワールドプリセットは通常ワールドのまま維持する。
- シードは引き続きランダム生成にする。
- 設定JSONは既存形式にboolean項目を追加するだけにする。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面の `ワールド` でシードリセット用ワールド設定を切り替えられる。
- シードリセット時に構造物生成ON/OFF、ボーナスチェストON/OFF、チート許可ON/OFF、ハードコアON/OFFが反映される。
- 設定画面カテゴリがより細かく分かれている。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- ワールドタイプやプリセットの切替。
- 固定シード入力UI。
- データパック選択やゲームルール編集。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.16` が反映されていることを確認。
- `SeedResetWorldStarter` で構造物生成、ボーナスチェスト、チート許可、ハードコア設定を新規ワールド作成へ反映する実装にした。
- 設定画面カテゴリを8項目に細分化し、2段タブで切り替えられるようにした。

## 2026-06-07 干草まとめ破壊とエンダーアイ自動回収タスク

### タスク情報
- 対象: 練習補助、エンダーアイ保護処理
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 干草の俵を1つ壊したとき、周囲にある干草の俵もまとめて破壊する。
- まとめ破壊は設定でON/OFFできるようにする。
- エンダーアイ保護ON時、飛翔終了後にアイテムとしてその場へ落とさず、投げたプレイヤーへ自動回収する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- まとめ破壊は練習モードでのみ有効にする。
- 干草の俵以外のブロックは対象にしない。
- まとめ破壊の範囲は近距離に限定し、過剰なブロック更新を避ける。
- エンダーアイの自動回収は投げたプレイヤーを推定して行う。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定ONの練習モードで干草の俵を壊すと、周囲の干草の俵も破壊される。
- 設定OFFまたは競技モードではまとめ破壊されない。
- エンダーアイ保護ONの練習モードで、飛翔終了後のエンダーアイがその場に落ちずプレイヤーへ戻る。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 木、葉、作物、鉱石など干草の俵以外の一括破壊。
- エンダーアイの軌道や要塞探索ロジックの変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.17` が反映されていることを確認。
- `HayBaleClusterBreaker` で `Blocks.HAY_BLOCK` の周囲一括破壊を実装した。
- `EyeOfEnderMixin` で飛翔終了後のエンダーアイをドロップではなくプレイヤーのインベントリへ戻す実装にした。

## 2026-06-07 HUDコンパクト化タスク

### タスク情報
- 対象: EndrRTA HUD
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- HUDの横幅を抑え、画面左上を大きく塞がない縦型レイアウトへ変更する。
- 縦型でも長くなりすぎないよう、HUD高さを画面高さの半分以内に制限する。
- 表示できない追加情報は最後に省略行としてまとめる。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 表示情報の意味や設定項目は変更しない。
- F3デバッグ表示中のHUD非表示挙動は維持する。
- 新しい画像や外部ライブラリは追加しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- HUDが横長カードではなく、幅を抑えた縦型表示になる。
- HUDの高さが画面の半分を超えない。
- 重要なRTA/IGTと主要情報が読み取れる。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- HUD位置のドラッグ移動。
- HUDサイズのゲーム内スライダー設定。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.18` が反映されていることを確認。
- HUD幅を `224px` に抑え、タイマーを縦型カード内にまとめる実装にした。
- HUD高さを `graphics.guiHeight() / 2` 以内に制限し、入りきらない行は省略行で表示する実装にした。

## 2026-06-07 レーダー更新と円グラフホイール長押しタスク

### タスク情報
- 対象: HUDレーダー表示、円グラフ補助の開閉操作
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- レーダー距離をHUD描画時のプレイヤー現在位置から再計算し、リアルタイムに変化するようにする。
- エンド要塞/ネザー要塞は発見扱いになるまで `????` と表示する。
- 円グラフ補助の開閉を `P` キーではなく、マウスホイールボタン長押しに変更する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- レーダーの構造物検索範囲や自動スプリット距離設定は変更しない。
- 円グラフ表示中のホイール選択、左クリック決定、右クリック戻りは維持する。
- ホイールボタン長押しはゲーム画面中かつ練習モードのときだけ有効にする。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 要塞までの距離が移動に合わせて更新される。
- 発見扱いになる前の要塞表示が `????` になる。
- `P` キーでは円グラフを開閉しない。
- ホイールボタン長押しで円グラフを開閉できる。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- レーダーの完全な視認判定。
- 円グラフの描画そのものの変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.19` が反映されていることを確認。
- HUDのレーダー距離は保存済み距離ではなく、現在のプレイヤー位置から描画時に再計算する実装にした。
- エンド要塞/ネザー要塞は対応スプリット記録前に `????` と表示する実装にした。
- `P` キーの円グラフ開閉登録を外し、ホイールボタン長押しで開閉する実装にした。

## 2026-06-07 未実装プレビュー設定削除タスク

### タスク情報
- 対象: EndrRTA 設定画面、設定データ
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 未実装で効果のない旧プレビュー設定を設定画面から削除する。
- `EndrRTAConfig` から未使用の旧プレビュー設定フィールドを削除する。
- リセットカテゴリの説明文を実態に合わせて修正する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- クイックリセットや新規ワールド作成の挙動は変更しない。
- 既存の設定JSONに古い旧プレビュー設定キーが残っていても、読み込みを壊さない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面に旧プレビュー設定が表示されない。
- コード上に旧プレビュー設定フィールドの参照が残らない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- 実際のワールドプレビュー機能の実装。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.20` が反映されていることを確認。
- 設定画面から旧プレビュー設定を削除した。
- `src` 配下に旧プレビュー設定の参照が残っていないことを確認。

## 2026-06-07 HUD背景透明度スライダー追加タスク

### タスク情報
- 対象: HUD描画、設定画面、設定データ
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- HUDパネル背景の透明度を `config/endrrta.json` に保存できる設定として追加する。
- 設定画面のHUDカテゴリに、0%から100%まで変更できるスライダーを追加する。
- HUDパネルの背景グラデーションに設定値を反映する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- HUDの位置、幅、高さ、表示項目は変更しない。
- 既存設定ファイルに透明度設定がない場合は、現在の見た目に近い既定値を使う。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- HUDカテゴリに `HUD背景透明度` スライダーが表示される。
- スライダー変更後に保存すると、HUD背景の透明度が変わる。
- 設定値が範囲外でも0%から100%に丸められる。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- HUDカードや行背景の個別透明度設定。
- HUDのドラッグ移動やサイズ変更。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.23` が反映されていることを確認。
- HUDカテゴリに `HUD背景透明度` スライダーを追加した。
- `hudBackgroundOpacity` は読み込み時と保存時に0%から100%へ丸める実装にした。

## 2026-06-07 ビルド後mods自動配置とHUD透明度軽量化タスク

### タスク情報
- 対象: Gradleビルド後のローカルMinecraft mods配置、HUD透明度描画
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- `build` 完了後に通常Jarを `C:\Users\sakip\AppData\Roaming\.minecraft\mods` へコピーする。
- コピー前に同ディレクトリ内の `endrrta-*.jar` を削除し、旧バージョンが残らないようにする。
- `sources.jar` はmodsへ配置しない。
- HUD背景透明度の色計算をキャッシュし、透明度0%では背景と影の描画をスキップする。
- 修正に合わせて `mod_version` を PATCH 更新する。
- 構造タグ修正後の最終配置版は `2.0.35` とする。

### 制約
- EndrRTA以外のmod Jarは削除しない。
- ビルド成果物の生成場所は変更しない。
- HUDの表示項目、位置、サイズは変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- `gradlew.bat build` または `gradlew.bat clean build --warning-mode all` 後に最新の `endrrta-<version>.jar` がmodsへ配置される。
- mods内の古い `endrrta-*.jar` は削除される。
- `endrrta-<version>-sources.jar` はmodsへ配置されない。
- HUD透明度描画で毎フレームの色計算が発生し続けない。
- 透明度0%ではHUDパネル背景と影を描画しない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 対象外
- Minecraft本体や他modの負荷対策。
- HUD全体の再設計。

## 2026-06-07 mod表示名変更タスク

### タスク情報
- 対象: mod表示名、ゲーム内表示名、生成Jar名、mods自動配置
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- modの表示名を `EndraRTA` に変更する。
- HUD、設定画面、キー設定表示、通知、ログ、クイックリセット用ワールド名の表示を `EndraRTA` に変更する。
- 生成Jar名を `endrarta-<version>.jar` にする。
- mods自動配置時に旧 `endrrta-*.jar` と新 `endrarta-*.jar` の両方を削除してから最新版をコピーする。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 既存設定やリソース参照を壊さないため、mod ID `endrrta`、Javaパッケージ、設定ファイル名は維持する。
- EndraRTA以外のmod Jarは削除しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- `fabric.mod.json` の `name` が `EndraRTA` になっている。
- ゲーム内に表示される主要な旧名表記が `EndraRTA` へ置き換わっている。
- `gradlew.bat clean build --warning-mode all` 後に `endrarta-<version>.jar` がmodsへ配置される。
- mods内に旧 `endrrta-*.jar` が残らない。

## 2026-06-07 追加練習補助とHUD修正タスク

### タスク情報
- 対象: ボートスタック、アイテム拾得拒否、ピグリン要塞タイプHUD、村スポーン補正、F3表示中HUD制御
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 設定ON時、ボート/チェスト付きボート/竹の筏を最大64個までスタックできるようにする。
- 設定ON時、`config/endrrta.json` の `ignoredPickupItems` に列挙したアイテムを拾わないようにする。
- ピグリン要塞タイプをHUDに表示し、設定画面から表示/非表示を切り替えられるようにする。
- 村スポーン補正をサーバーtick待ちだけでなく、プレイヤー参加直後にも実行する。
- F3デバッグ画面またはF3プロファイラ円グラフ表示中はEndraRTA HUDを描画しない。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- mod ID、設定ファイル名、Javaパッケージ名は変更しない。
- アイテム拾得拒否は練習モードかつ設定ONのときだけ有効にする。
- ピグリン要塞タイプは近傍検索できた最寄り候補を表示する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面で `ボートスタック` を切り替えられる。
- 設定画面で `指定アイテム拾得拒否` を切り替えられる。
- 設定画面で `ピグリン要塞タイプ` を切り替えられる。
- ネザーでHUDにピグリン要塞タイプが表示される。
- F3+Pで円グラフを出してもEndraRTA HUDが表示されない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.26` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.26.jar` が配置されていることを確認。
- ピグリン要塞タイプ用の構造タグJSONがJSONとして読み込めることを確認。

## 2026-06-07 ベッドスタック追加タスク

### タスク情報
- 対象: 練習補助設定、ItemStack最大スタック数
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 設定ON時、全色ベッドを最大64個までスタックできるようにする。
- 設定画面の初期補助カテゴリに `ベッドスタック` を追加する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ベッドスタックは練習モードでのみ有効にする。
- ボートスタックの既存挙動は維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面で `ベッドスタック` を切り替えられる。
- 設定ONの練習モードでベッドの最大スタック数が64になる。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.27` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.27.jar` が配置されていることを確認。

## 2026-06-07 MouseHandlerMixin IDE診断修正タスク

### タスク情報
- 対象: `MouseHandlerMixin`
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- `@Shadow minecraft` を削除し、`Minecraft.getInstance()` を使う形に変更する。
- `@Inject` 対象をIDEが解決しやすいintermediary名へ変更する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 円グラフ補助のホイール選択、左クリック決定、右クリック戻りの挙動は変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- `MouseHandlerMixin` の `@Shadow field` 診断が出ない。
- `MouseHandlerMixin` の `onScroll` / `onButton` ターゲット名診断が出ない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.28` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.28.jar` が配置されていることを確認。

## 2026-06-07 PieChartAssistHandlerパッケージ移動タスク

### タスク情報
- 対象: `MouseHandlerMixin` と円グラフ補助ハンドラ
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- IDEが `chihalu.endrrta.client.pie` を解決できない診断を避けるため、`PieChartAssistHandler` を `client.mixin` パッケージへ移動する。
- `EndrRTAClient` と `EndrRTAHud` のimportを移動先へ更新する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 円グラフ補助の挙動は変更しない。
- Mixin JSONの登録対象は変更しない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- `MouseHandlerMixin` から `PieChartAssistHandler` を参照できる。
- 旧 `client.pie` パッケージ参照が残らない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.29` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.29.jar` が配置されていることを確認。
- `src` 配下に旧 `client.pie` パッケージ参照が残っていないことを確認。

## 2026-06-07 ItemStackMixin bootstrapクラッシュ修正タスク

### タスク情報
- 対象: `ItemStackMixin`、設定読み込み状態
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 本番起動時に `ItemStackMixin` の `getMaxStackSize` 注入先が見つからずクラッシュする問題を修正する。
- `@Inject` 対象を実行環境で解決できるintermediary名へ変更する。
- Minecraft bootstrap中に設定やアイテム定数へ触れないよう、設定読み込み済みフラグを追加する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ボート/ベッドスタックの設定挙動は維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 起動ログに `ItemStackMixin` の `getMaxStackSize` ターゲット不一致が出ない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.30` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.30.jar` が配置されていることを確認。
- `ItemStackMixin` の注入先が `method_7914` になっていることを確認。

## 2026-06-07 ItemStackMixin削除とFabric API化タスク

### タスク情報
- 対象: ボート/ベッドスタック処理、起動bootstrapクラッシュ
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 起動時に `ItemStackMixin` がターゲット名不一致でクラッシュする問題を根本回避する。
- `ItemStackMixin` を削除し、Mixin設定からも外す。
- ボート/ベッドの最大スタック数変更を `DefaultItemComponentEvents.MODIFY` に置き換える。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ボート/ベッドスタックは起動時の設定値を標準コンポーネントへ反映する。
- 設定変更後の反映にはMinecraft再起動が必要。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 生成Jarに `ItemStackMixin.class` が含まれない。
- `endrrta.mixins.json` に `ItemStackMixin` が含まれない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.31` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.31.jar` が配置されていることを確認。
- 生成Jarに `ItemStackMixin.class` が含まれず、`PracticeStackComponents.class` が含まれることを確認。

## 2026-06-07 ボート/ベッドスタック削除タスク

### タスク情報
- 対象: ボート/ベッドスタック設定と処理
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- ボート/ベッドの最大スタック数変更機能を削除する。
- 設定項目 `stackBoats` / `stackBeds` と設定画面の `ボートスタック` / `ベッドスタック` を削除する。
- `PracticeStackComponents` の登録とクラス本体を削除する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ほかの練習補助、HUD、拾得拒否、村スポーン、円グラフ補助の挙動は変更しない。
- 既存の `config/endrrta.json` に古いキーが残っていても起動を妨げない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 設定画面に `ボートスタック` / `ベッドスタック` が表示されない。
- `src` 配下に `stackBoats` / `stackBeds` / `PracticeStackComponents` の参照が残らない。
- 生成Jarに `PracticeStackComponents.class` が含まれない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.32` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.32.jar` が配置されていることを確認。
- `src` 配下に `stackBoats` / `stackBeds` / `PracticeStackComponents` / `ボートスタック` / `ベッドスタック` の参照が残っていないことを確認。
- 生成Jarに `PracticeStackComponents.class` と `ItemStackMixin.class` が含まれないことを確認。

## 2026-06-07 MouseHandlerMixin起動クラッシュ修正タスク

### タスク情報
- 対象: `MouseHandlerMixin`、起動時Mixin変換
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- `MouseHandlerMixin` が起動時に `method_1598` を解決できずクラッシュする問題を修正する。
- `MouseHandler` の実メソッド名に合わせ、注入先を `onScroll` / `onButton` に変更する。
- `PieChartAssistHandler` をMixin専用パッケージ外へ移動し、通常クラスの直接参照クラッシュを回避する。
- 存在しない種類別ピグリン要塞構造IDを参照しないよう、タグを `minecraft:bastion_remnant` へ集約する。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- 円グラフ補助のホイール選択、クリック決定、右クリック戻りの挙動は変更しない。
- ボート/ベッドスタック削除後の状態は維持する。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- 起動ログに `MouseHandlerMixin` の `method_1598` ターゲット不一致が出ない。
- 起動ログに `PieChartAssistHandler is in a defined mixin package` が出ない。
- 起動ログに `Unbound values in registry` が出ない。
- `gradlew.bat clean build --warning-mode all` が成功する。
- 最新JarがMinecraftのmodsフォルダへ配置される。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.35` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.35.jar` が配置されていることを確認。
- 生成Jarに `MouseHandlerMixin.class` と `client/pie/PieChartAssistHandler.class` が含まれることを確認。
- 生成Jarに古い種類別ピグリン要塞タグと `PracticeStackComponents.class` / `ItemStackMixin.class` が含まれないことを確認。
- `runClient` で初期化、リソース読み込み、ワールド参加、村スポーン補正まで到達することを確認。
- `runClient` の最新ログで `MouseHandlerMixin` / `PieChartAssistHandler` / `Unbound values in registry` の起動クラッシュが出ていないことを確認。

## 2026-06-07 レーダーHUDと円グラフ維持タスク

### タスク情報
- 対象: HUDレーダー表示、ピグリン要塞タイプ表示、F3円グラフ表示
- 使用Skill: jp-coding-rules
- 変更区分: PATCH

### 範囲
- 要塞レーダーのHUD値を方角表示から `X/Z/距離` 表示へ変更する。
- オーバーワールドではエンド要塞だけ、ネザーではネザー要塞とピグリン要塞タイプだけを表示する。
- ディメンション移動時に非対象ディメンションのレーダー結果を消す。
- ピグリン要塞タイプを生成済み構造ピース名から `住居` / `橋` / `ホグリン小屋` / `宝物部屋` に推定して表示する。
- F3を閉じても、円グラフ表示フラグがONなら円グラフを表示し続ける。
- 修正に合わせて `mod_version` を PATCH 更新する。

### 制約
- ピグリン要塞タイプは構造ピースが取得できる生成済み範囲で推定する。未取得時は `不明` とする。
- 競技モードやレーダーOFF時は既存通り補助表示を出さない。
- 外部の有料 API、サーバー、シークレットは使わない。

### 受け入れ条件
- エンド要塞/ネザー要塞HUDにX座標、Z座標、距離が表示される。
- ネザーではエンド要塞HUDが表示されない。
- オーバーワールドではネザー要塞HUDが表示されない。
- ネザーでピグリン要塞が見つかった場合、HUDに `ピグリン要塞 住居/橋/ホグリン小屋/宝物部屋/不明` の形で表示される。
- F3を閉じても円グラフが閉じない。
- `gradlew.bat clean build --warning-mode all` が成功する。

### 検証メモ
- `gradlew.bat clean build --warning-mode all`: 成功。警告なし。
- `mod_version=2.0.36` が反映されていることを確認。
- `C:\Users\sakip\AppData\Roaming\.minecraft\mods` に `endrarta-2.0.36.jar` が配置されていることを確認。
- 生成Jarに `DebugScreenOverlayMixin.class` と `bastion_remnant.json` が含まれることを確認。
- 旧種類別ピグリン要塞タグ、旧MouseHandler注入名、ボート/ベッドスタック関連クラスが残っていないことを確認。
- `runClient` で初期化、リソース読み込み、ワールド参加、村スポーン補正まで到達することを確認。
- 新しいクラッシュレポートが生成されないことを確認。
