# あいさつメッセージ生成システム

画面で名前を登録すると未処理のデータがDBに保存されます。バッチを実行すると、未処理のデータに「こんにちは、田中さん！」のようなメッセージが付き、画面の一覧に結果が出ます。

画面 → REST API → DB → Spring Batch → DB → 画面、という流れを1つのSpring Bootアプリで確認するためのサンプルです。

## このアプリで学べること

- Thymeleafで画面の骨組みを返し、データの取得と更新はブラウザの `fetch` からREST APIを呼ぶこと
- リクエストとレスポンスにDTOを使い、JPAのEntityをAPIの外に出さないこと
- Bean Validationで入力を検証し、エラーを共通のJSONで返すこと
- Spring Data JPAで登録と一覧取得をすること
- Spring BatchのJob、Step、Taskletで未処理データを更新すること
- バッチの終了状態を見てからAPIの成功・失敗を返すこと

## 採用技術とバージョン

2026年10月18日時点の公式情報で、Java 21に対応する安定版を選びました。

- 安定版の確認: [Spring Bootのドキュメント](https://docs.spring.io/spring-boot/maven-plugin/using.html) の現行安定版は Spring Boot 4.1.1
- 依存関係の確認: [Spring Boot 4.1.1のリリース](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.1) で Spring Batch 6.0.5 に更新されている
- バージョンの管理元: `spring-boot-starter-parent` 4.1.1 のBOM

| 技術 | バージョン | 役割 |
| --- | --- | --- |
| Java | 21 | 実行環境 |
| Spring Boot | 4.1.1 | アプリの土台。依存バージョンもここが管理する |
| Spring Framework | 7.0.9 | DIとSpring MVC |
| Spring Batch | 6.0.5 | Job / Step / Tasklet |
| Spring Data JPA / Hibernate ORM | Hibernate ORM 7.4.5.Final | DBアクセス |
| Bean Validation / Hibernate Validator | 9.1.3.Final | 名前の検証 |
| Thymeleaf | 3.1.5.RELEASE | 画面テンプレート |
| H2 | 2.4.240 | インメモリDB |
| Tomcat | 11.0.24 | 組み込みWebサーバ |
| Maven Wrapper | 3.9.11 | ビルド。Maven本体のインストールは不要 |

Spring Boot 4では、Spring MVCのスターター名が `spring-boot-starter-webmvc` です（以前の `spring-boot-starter-web` は非推奨）。バッチのメタデータテーブルをDBに作るため、`spring-boot-starter-batch-jdbc` を使います。Spring Batch 6は何も指定しないとメタデータを保存しない構成になるためです。ライブラリのバージョンは親POMに任せ、個別には指定していません。

## 前提条件

- JDK 21
- 起動はMaven Wrapperを使うため、Maven自体のインストールは不要です

## 起動

プロジェクトのフォルダで、次を実行します。

```powershell
.\mvnw.cmd spring-boot:run
```

起動ログに `Started GreetingApplication` と出たら準備完了です。停止するときは、そのターミナルで Ctrl+C を押します。

## テスト

```powershell
.\mvnw.cmd test
```

## 画面の使い方

ブラウザで [http://localhost:8080/](http://localhost:8080/) を開きます。

1. 名前に「田中」と入力し、「登録」を押します。一覧に状態「未処理」、メッセージ「未生成」、処理日時「—」で追加されます。
2. 「バッチ実行」を押します。処理件数1で完了し、一覧が「処理済み」、メッセージ「こんにちは、田中さん！」に変わり、処理日時が表示されます。
3. もう一度「バッチ実行」を押すと、処理件数0で完了します。処理済みのメッセージと処理日時は変わりません。
4. 「一覧更新」は、表示されている一覧をAPIから取り直します。

登録、バッチ実行、一覧更新の通信中は、その操作のボタンが無効になり、終わると戻ります。

## REST API

エラーのときは、成功時とは別に次の形を返します。スタックトレースは含めません。

```json
{ "message": "名前を入力してください。" }
```

### 名前を登録する

`POST /api/greetings`

リクエスト:

```json
{ "name": "田中" }
```

成功時は `201 Created` です。状態は `PENDING`、メッセージと処理日時は `null` です。

```json
{
  "id": 1,
  "name": "田中",
  "status": "PENDING",
  "message": null,
  "createdAt": "2026-10-18T21:10:00.123+09:00",
  "processedAt": null
}
```

名前は前後の空白を除いてから検証します。未入力、空白のみ、50文字超過、壊れたJSONは `400` です。50文字ちょうどは登録できます。

PowerShell:

```powershell
$body = '{"name":"田中"}'
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/greetings `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

### 一覧を取得する

`GET /api/greetings`

IDの昇順です。データがなければ `[]` を返します。

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/greetings
```

### バッチを実行する

`POST /api/batch/greetings`

Jobをその場で最後まで実行し、完了後に `200` を返します。`processedCount` は今回更新した件数です。対象が0件でも正常終了し、件数は0です。

```json
{
  "jobExecutionId": 1,
  "status": "COMPLETED",
  "processedCount": 1
}
```

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/batch/greetings
```

同じバッチが実行中の追加リクエストは `409` です。Jobが失敗したときは `500` と `message` を返し、成功レスポンスにはしません。

## 処理の流れと各層の役割

1. ブラウザが `GET /` を開く。`GreetingPageController` がThymeleafのHTMLを返す。
2. 画面のJavaScriptが `POST /api/greetings` を呼ぶ。`GreetingApiController` がDTOを受け取り、`GreetingService` が名前を検証して `GreetingRepository` 経由でH2へ保存する。状態は `PENDING`。
3. 「バッチ実行」が `POST /api/batch/greetings` を呼ぶ。`GreetingBatchLauncher` がSpring BatchのJobを同期実行する。
4. JobのStepが `GreetingTasklet` を1回実行する。Taskletは `GreetingService` を呼び、`PENDING` だけを「こんにちは、{name}さん！」にして `COMPLETED` と処理日時を保存する。
5. LauncherがJobの終了状態を確認し、`jobExecutionId`、`status`、今回の処理件数を返す。
6. 画面が `GET /api/greetings` で一覧を取り直し、状態を「未処理」「処理済み」と表示する。

| 層 | 役割 |
| --- | --- |
| 画面（Thymeleaf + JavaScript） | 入力、操作、結果表示。業務判断はしない |
| API | HTTPの入口。DTOとステータスコードを担当する |
| Service | 登録、一覧、未処理の更新。トランザクションの境界 |
| Repository | `greetings` テーブルへの読み書き |
| DB | 業務データとSpring Batchのメタデータ |
| Batch | Job / Step / Tasklet。起動のたびに未処理だけを処理する |

日時はすべて日本時間（`Asia/Tokyo`）です。APIの日時は `+09:00` 付きの文字列で、画面も同じタイムゾーンで表示します。

## 主要ファイル

| ファイル | 内容 |
| --- | --- |
| `src/main/java/com/example/greeting/web/GreetingPageController.java` | `GET /` で画面を返す |
| `src/main/resources/templates/index.html` | 画面のHTML |
| `src/main/resources/static/js/app.js` | `fetch` によるAPI呼び出し |
| `src/main/resources/static/css/style.css` | 画面の見た目 |
| `src/main/java/com/example/greeting/api/GreetingApiController.java` | 3つのREST API |
| `src/main/java/com/example/greeting/api/ApiExceptionHandler.java` | 共通のエラーJSON |
| `src/main/java/com/example/greeting/service/GreetingService.java` | 登録、一覧、未処理の更新 |
| `src/main/java/com/example/greeting/domain/Greeting.java` | 業務テーブルのEntity |
| `src/main/java/com/example/greeting/repository/GreetingRepository.java` | Spring Data JPA |
| `src/main/java/com/example/greeting/batch/GreetingBatchConfig.java` | JobとStep |
| `src/main/java/com/example/greeting/batch/GreetingTasklet.java` | 未処理レコードの更新 |
| `src/main/java/com/example/greeting/batch/GreetingBatchLauncher.java` | 同期実行、並行実行の防止、結果の判定 |
| `src/main/resources/application.properties` | H2、テーブル作成、起動時のJob無効化 |
| `src/test/java/com/example/greeting/GreetingFlowTest.java` | 登録とバッチの自動テスト |

## 制約

- H2はインメモリです。アプリを再起動すると、登録データもSpring Batchのメタデータも消えます。
- バッチは少量データ向けです。Taskletが未処理をまとめて読み、1件ずつ更新します。大量データ向けのチャンク処理はしていません。
- バッチ実行APIは学習用に同期実行です。HTTPリクエストはJobが終わるまで待ちます。件数が少ないこのサンプルではすぐに戻ります。本番の長いバッチをWebリクエストの中で同期実行する構成ではありません。
- 認証、クラウド、Docker、マイクロサービス、SPAフレームワークは含みません。
