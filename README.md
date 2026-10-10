# Book Management API

Kotlin、Spring Boot、jOOQ、PostgreSQLで実装した書籍・著者管理用REST APIです。書籍と著者は多対多で関連付けられます。

## 実装機能

- 著者の登録、取得、全体更新
- 書籍の登録、取得、全体更新
- 1冊に対する複数著者の関連付け
- 指定著者に紐づく書籍一覧の取得
- Flywayによるスキーマ管理と、PostgreSQLスキーマからのjOOQ Kotlinコード生成

## 技術構成

| 技術 | バージョン |
|---|---|
| Java | 21 |
| Kotlin | 2.1.21 |
| Spring Boot | 3.5.16 |
| Gradle Wrapper | 9.4.1 |
| PostgreSQL | 16 |
| Flyway | 10（Dockerイメージ） |
| jOOQ | 3.19.35（Spring Bootの依存管理） |

## ディレクトリ構成

```text
src/
├── main/
│   ├── kotlin/com/example/bookmanagement/
│   │   ├── controller/  # HTTPリクエスト・レスポンスを扱うAPI層
│   │   ├── service/     # 業務ルールとトランザクションを扱う層
│   │   ├── repository/  # jOOQのDSLContextでDBアクセスを行う層
│   │   ├── dto/         # APIのリクエスト・レスポンス
│   │   ├── model/       # ドメインモデルと出版状態
│   │   ├── exception/   # APIエラーへの変換を含む例外処理
│   │   └── config/      # ClockやOpenAPIなどの設定
│   └── resources/
│       └── db/migration/ # FlywayのDBマイグレーション
├── test/kotlin/          # 単体テスト、Web層テスト、DB結合テスト
└── functionalTest/kotlin/ # HTTPからPostgreSQLまで通すfunctionalテスト
```

## 必要な実行環境

- JDK 21
- Docker Desktop（Docker Composeを利用可能な状態）
- インターネット接続（初回のGradle依存関係、Dockerイメージ、Javaツールチェーン取得時）

Gradle本体を別途インストールする必要はありません。リポジトリに含まれるGradle Wrapperを使用します。

## 環境構築と起動

リポジトリ直下で以下を実行します。

```bash
docker compose up -d postgres
docker compose run --rm flyway
./gradlew generateJooq
./gradlew bootRun
```

起動後、APIは `http://localhost:8080` で利用できます。
ブラウザでAPI仕様の確認とリクエスト実行を行う場合は、Swagger UI（ `http://localhost:8080/swagger-ui/index.html` ）
を開いてください。OpenAPI JSONは `http://localhost:8080/v3/api-docs` で取得できます。

停止する場合は以下を実行します。DBデータを残す場合は `down` のみ、DBデータも削除する場合は `down -v` を使用します。

```bash
docker compose down
```

### 接続設定

ローカル開発用の既定値は以下です。必要に応じて環境変数で置き換えられます。

| 環境変数 | 既定値 |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/book_management` |
| `DB_USERNAME` | `book_management` |
| `DB_PASSWORD` | `book_management` |
| `JOOQ_DB_URL` | `jdbc:postgresql://localhost:5432/book_management` |
| `JOOQ_DB_USERNAME` | `book_management` |
| `JOOQ_DB_PASSWORD` | `book_management` |

これらの既定値はローカル開発専用です。本番用の認証情報はリポジトリへ保存しません。

## FlywayとjOOQコード生成

Flywayのmigrationは `src/main/resources/db/migration` に配置します。`docker compose run --rm flyway` はこのmigrationをPostgreSQLへ適用します。アプリケーション起動時にもFlywayがスキーマを検証・適用します。

`./gradlew generateJooq` は、Flyway適用済みのPostgreSQLスキーマを読み取り、Kotlin向けjOOQコードを `build/generated-src/jooq/main` に生成します。生成コードは`build/`配下であり、手動編集・Git管理しません。

`./gradlew clean build` でもjOOQコード生成が実行されるため、事前にPostgreSQLを起動しmigrationを適用してください。

## DB構成

| テーブル | 内容 |
|---|---|
| `authors` | 著者（`id`, `name`, `birth_date`） |
| `books` | 書籍（`id`, `title`, `price`, `publication_status`） |
| `book_authors` | 書籍と著者の中間テーブル。`book_id`と`author_id`の複合主キーおよび外部キーを持つ |

`books`と`authors`は`book_authors`を通じた多対多の関係です。

## API一覧

| Method | Path | 内容 |
|---|---|---|
| POST | `/authors` | 著者登録 |
| GET | `/authors/{authorId}` | 著者取得 |
| PUT | `/authors/{authorId}` | 著者全体更新 |
| GET | `/authors/{authorId}/books` | 著者に紐づく書籍一覧 |
| POST | `/books` | 書籍登録 |
| GET | `/books/{bookId}` | 書籍取得 |
| PUT | `/books/{bookId}` | 書籍全体更新（著者関連も置換） |

## Postman実行例

PostmanでEnvironmentまたはCollection Variablesに以下を設定します。

| 変数 | 初期値 |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `authorId1` | 著者登録レスポンスの`id` |
| `authorId2` | 著者登録レスポンスの`id` |
| `bookId` | 書籍登録レスポンスの`id` |

POSTまたはPUTでは、Headerに`Content-Type: application/json`を設定し、Bodyを`raw`・`JSON`に選択します。

### 著者登録

| 項目 | 値 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/authors` |

```json
{"name":"夏目漱石","birthDate":"1867-02-09"}
```

続けて共同著者を登録し、各レスポンスの`id`を`authorId1`、`authorId2`へ設定します。

```json
{"name":"共同著者","birthDate":"1870-01-01"}
```

### 複数著者の書籍登録

| 項目 | 値 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/books` |

```json
{
  "title": "吾輩は猫である",
  "price": 1000,
  "publicationStatus": "UNPUBLISHED",
  "authorIds": [{{authorId1}}, {{authorId2}}]
}
```

登録レスポンスの`id`を`bookId`へ設定します。

### 書籍・著者別書籍一覧の取得

| 内容 | Method | URL |
|---|---|---|
| 書籍取得 | `GET` | `{{baseUrl}}/books/{{bookId}}` |
| 著者別書籍一覧 | `GET` | `{{baseUrl}}/authors/{{authorId2}}/books` |

書籍取得のレスポンス例です。`authors`には関連著者がネストされます。

```json
{
  "id": 1,
  "title": "吾輩は猫である",
  "price": 1000.00,
  "publicationStatus": "UNPUBLISHED",
  "authors": [
    {"id": 1, "name": "夏目漱石", "birthDate": "1867-02-09"},
    {"id": 2, "name": "共同著者", "birthDate": "1870-01-01"}
  ]
}
```

### 著者更新

| 項目 | 値 |
|---|---|
| Method | `PUT` |
| URL | `{{baseUrl}}/authors/{{authorId1}}` |

```json
{"name":"夏目漱石（更新）","birthDate":"1867-02-09"}
```

### 書籍更新

PUTでは`authorIds`の内容で関連付け全体を置換します。

| 項目 | 値 |
|---|---|
| Method | `PUT` |
| URL | `{{baseUrl}}/books/{{bookId}}` |

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": [{{authorId2}}]
}
```

## 入力制約と業務ルール

- 著者の`name`は必須で、空文字・空白のみは不可
- `birthDate`は必須で、当日以前のみ許可
- 書籍の`title`は必須で、空文字・空白のみは不可
- `price`は0以上（0を含む）
- `authorIds`は最低1件必要で、全IDが既存著者であること
- `publicationStatus`は`UNPUBLISHED`または`PUBLISHED`
- `UNPUBLISHED → PUBLISHED`は許可、`PUBLISHED → UNPUBLISHED`は拒否
- 存在しない著者・書籍の取得または更新は404
- 入力値・業務ルール違反は400

書籍と`book_authors`の変更は、同一トランザクションで実行します。

## テスト

Dockerを起動した状態で、次のコマンドを実行します。

```bash
./gradlew test            # 単体テスト・Web層テスト・DB結合テスト
./gradlew functionalTest  # HTTPからDBまでを通すfunctionalテスト
./gradlew check           # 上記を含む全検証とC1カバレッジ判定
```

上記コマンドの前に、環境構築手順どおりPostgreSQLを起動しFlyway migrationを適用してください。jOOQ生成にはローカルPostgreSQL、Testcontainersによる結合テストにはDockerデーモンが必要です。

Web層・Service層の単体テストに加え、Testcontainersで起動する独立PostgreSQLを使った結合テストを実行します。結合テストでは、Flyway migration、著者・書籍・多対多関連、関連付け全置換、著者別一覧を検証します。

functionalテストは`src/functionalTest/kotlin`に分離しています。ランダムポートで起動したアプリケーションにHTTPリクエストを送り、ControllerからService、jOOQ、PostgreSQLまでの一連の動作を検証します。

また、テスト専用のPostgreSQLトリガーで`book_authors`の挿入エラーを発生させ、書籍更新と関連付け更新がともにロールバックされることを確認しています。テストメソッド自体にはトランザクションを付けていないため、ロールバック不備をテスト側で隠しません。

## 設計上の判断

- DBアクセスはJPA/Hibernateを使わず、jOOQの生成済みテーブル定義と`DSLContext`で実装しています。
- 構成は`controller → service → repository → database`に限定し、DTO・最小限の例外処理だけを追加しています。
- リクエスト形式の検証はController、著者存在確認や出版状態遷移などの業務ルールはServiceに分離しています。
- 書籍更新の関連付けは差分更新ではなく、PUTの意味に合わせてリクエストの`authorIds`で全置換します。
- 日付依存の業務ルールには`Clock`を注入し、テストで日付を固定可能にしています。
