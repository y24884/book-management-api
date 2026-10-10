# 書籍管理バックエンドAPI 要件定義書

## 1. 目的
QUO CARD Digital Innovation Lab の公開コーディングテスト仕様に沿って、書籍・著者を管理するバックエンドAPIを構築する。

**必須要件（公開資料に記載）** と **実装提案（公開資料には明記なし）** を区別する。実装提案は着手前に確認すること。

## 2. 必須技術・提出条件
- 言語: Kotlin
- フレームワーク: Spring Boot、jOOQ
- RDBにデータを保存する
- Web APIのみ実装し、フロントエンドは不要
- 可能な範囲で単体テストを作成する
- 完成したプロジェクトを **public** のGitHubリポジトリにアップロードし、URLを提出する
- 公開記事上の期限: 2週間（実際の案内があればそちらを優先）
- 生成AI・コーディングエージェントの使用可

### 開発環境の提案（記事の例示であり必須指定ではない）
- Java 21、Gradle (Groovy DSL)
- PostgreSQL、Flyway、Docker Compose
- Flywayでスキーマ管理後、jOOQ Code Generationで型安全なDBアクセスコードを生成
- JUnit 5 / Spring Boot Test

## 3. 必須機能
1. 書籍と著者の情報をRDBに登録・更新できる
2. 指定した著者に紐づく書籍を取得できる

### 書籍
- タイトル
- 価格: 0以上
- 著者: 最低1人。複数著者を許可
- 出版状況: `UNPUBLISHED`（未出版）、`PUBLISHED`（出版済み）
- 一度 `PUBLISHED` になった書籍を `UNPUBLISHED` に戻すことは禁止

### 著者
- 名前
- 生年月日: 現在日以前
- 1人の著者が複数の書籍を執筆できる

書籍と著者は多対多の関係とする。

## 4. 実装提案（設計段階でレビューする）
### データベース
- `authors`: `id`, `name`, `birth_date`
- `books`: `id`, `title`, `price`, `publication_status`
- `book_authors`: `book_id`, `author_id`（複合主キー、外部キー）
- NOT NULL、CHECK、FOREIGN KEYなど適切な制約を設ける
- 書籍の著者最低1人という制約は、書籍登録・更新時のトランザクション内で検証する

### REST API案
| Method | Path | 内容 |
|---|---|---|
| POST | `/authors` | 著者登録 |
| GET | `/authors/{authorId}` | 著者取得（追加提案） |
| PUT | `/authors/{authorId}` | 著者更新 |
| POST | `/books` | 書籍登録 |
| GET | `/books/{bookId}` | 書籍取得（追加提案） |
| PUT | `/books/{bookId}` | 書籍更新 |
| GET | `/authors/{authorId}/books` | 著者に紐づく書籍一覧取得 |

### JSON例
`POST /authors`
```json
{"name":"夏目漱石","birthDate":"1867-02-09"}
```

`POST /books`
```json
{"title":"吾輩は猫である","price":1000,"publicationStatus":"UNPUBLISHED","authorIds":[1]}
```

### アーキテクチャ
- `controller` → `service` → `repository` → jOOQ → PostgreSQL
- DTOとエラー処理を分離し、必要以上の層や抽象化は導入しない
- RepositoryではjOOQの`DSLContext`を使い、JPA/Hibernateは使用しない
- 複数テーブルを更新する書籍登録・更新はトランザクションを適用する
- リクエストの形式検証と、出版状態遷移などの業務ルール検証を分ける
- 404、400等のHTTPステータスと一貫したエラーレスポンスを返す

## 5. テスト項目
- 正常な著者登録・更新
- 生年月日が未来の場合の拒否
- 正常な書籍登録・更新
- 価格が負の場合の拒否（0は許可）
- 著者が0人の場合の拒否
- 存在しない著者IDの拒否
- `UNPUBLISHED` → `PUBLISHED` を許可
- `PUBLISHED` → `UNPUBLISHED` を拒否
- 複数著者の書籍と、著者別書籍一覧取得
- 必要に応じてDBを用いた結合・トランザクションの統合テスト

## 6. 品質・評価観点
公開資料の評価観点を優先する。
- 指定技術スタックとライブラリの適切な使用
- 手順どおりに起動でき、仕様どおりに動作する
- 明確なクラス名・関数名・変数名
- KotlinのNull安全性、`val`の優先使用、統一されたフォーマット
- オーバーエンジニアリングの回避
- 適切な単体テスト

## 7. README
- 技術構成、設計方針、DB構成
- 環境要件と起動手順
- Docker Compose、Flyway、jOOQコード生成手順
- API一覧、curlリクエスト例
- テスト実行方法

## 8. Codexへの進行指示
**最初はSTEP 1のみ実施し、ユーザーの承認前にコード生成・ファイル変更を行わないこと。**

1. **設計提案のみ**: ディレクトリ構成、DB設計、API仕様、クラス責務、実装順序を提示。公開仕様と追加提案を区別する。
2. 承認後、Gradle・Docker Compose・Flywayを設定する。
3. jOOQコード生成を設定する。
4. 著者APIを実装する。
5. 書籍APIを実装する。
6. テストを作成・実行する。
7. READMEを作成する。
8. ビルド・テスト・起動・主要APIの動作を検証し、仕様漏れ・Null安全性・過剰設計をレビューする。

不明点は推測で仕様を確定せず、選択肢と推奨案を提示して確認すること。

## 9. 出典
ユーザー提供資料: 「コーディングテストについて - QUO CARD Digital Innovation Lab Tech Blog」（2024年3月22日公開）。本書の「実装提案」は公開記事の明示的な必須要件ではない。
