# spring-boot-postgresql-rag-sample

Spring BootベースのAI Agent向けのRAGのサンプルコードを作成する

以下の技術スタックを採用する

* 呼び出し元はCodexやClaude CodeでMCP経由。STDIOではなくSSE。
* Spring Boot 4.1＋Spring WebMVC＋Spring Security＋MyBatis＋MCP Server Boot Starters
* Java 25
* PostgreSQLはDocker composeで用意し、Apache AgeとPGroongaを組み合わせて、属性検索＋全文検索＋グラフ検索を実装

データは架空の運送会社のビジネスルールを作成して、デモデータとすること
認証は個人別に払いだすPATで行う。PATはPostgreSQLの中で状態を管理する。このPATの人向けの管理機能は不要。

## ディレクトリ構造と実装範囲

[構成・責務・検索設計](docs/architecture.md)を参照。
Spring Boot起動クラス、SSE、PAT認証、DB初期化、充実した架空デモシナリオ、検索サービス、MCP検索ツールを含む。

デモシナリオは、コード体系、用語集、組織構造、業務フロー、商品・サービスとして提供する輸送メニュー、経営・取引先・現場ニーズ、KPI、業界・競合動向を58項目・108関係で表現する。
市場・競合・会社・数値はすべて架空であり、事実情報として利用しないこと。

## 初期セットアップ

必要環境：Docker Desktop（Linuxコンテナ）。ローカルでアプリを実行する場合はJDK 25とMaven 3.9以上も必要。

```powershell
# 初回のみ。既存の.envは上書きしない
Copy-Item .env.example .env
# DBとアプリをJava 25のコンテナでビルド・起動
docker compose --profile app up -d --build
docker compose ps
docker compose logs app
```

公開ポートはlocalhostの5432（DB）と8080（アプリ）。`.env`で変更可能。
`.env`はGit管理対象外。記載したパスワードはローカル開発専用。
ポートが使用中の場合は`.env`の`POSTGRES_PORT`を例として`15432`に変更する。
その場合、ローカルアプリ側も`DB_URL=jdbc:postgresql://localhost:15432/transport_rag`に合わせる。
コンテナ同士の接続設定は変更不要。

ローカルでアプリを開発する場合：

```powershell
docker compose up -d --build postgres
$env:DB_PASSWORD = 'local-development-only' # .envのPOSTGRES_PASSWORDに合わせる
mvn verify
mvn spring-boot:run
```

Mavenからの起動では`.env`は自動で読み込まれない。
DBの設定を変えた場合は`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`を環境変数に設定する。

## DBと認証の確認

```powershell
Get-Content -Raw -Encoding utf8 scripts/verify-db.sql | docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
$pat = ./scripts/create-pat.ps1 -Subject demo-user
# 認証なしでは401
curl.exe -i http://localhost:8080/sse
# 認証ありではSSEのendpointイベント。接続を継続するため10秒で終了
curl.exe -N --max-time 10 -H "Authorization: Bearer $pat" http://localhost:8080/sse
```

PATは発行時だけ出力されるので安全に保管する。ソースや設定ファイルへコミットしない。
MCPクライアントにはSSE URL `http://localhost:8080/sse` とBearerヘッダーを設定する。
メッセージ送信先はSSEのendpointイベントから取得する（基本パス `/mcp/message`）。

利用できるMCPツール：

- `search_business_knowledge`：日本語全文、種別、地域、状態、基準日で検索
- `get_business_knowledge`：コードから根拠本文、主管、有効期間、出典を取得
- `explore_business_relationships`：最大3段階で組織、工程、規程、サービス、ニーズ、KPI、市場仮説の関係を探索

MCPの疎通確認には、有効なPATを環境変数へ設定してスモークテストを実行できる。

```powershell
$env:MCP_PAT = $pat
node scripts/smoke-mcp.mjs http://localhost:8080
```

失効する場合はDBで対象PATの`revoked_at`を更新する：

```sql
UPDATE public.personal_access_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE id = 1;
```

停止は`docker compose --profile app down`。データはボリュームに保持される。
初期SQLは初回起動時のみ適用されるため、詳細は構成ドキュメントを参照。

既存の開発用DBボリュームへデモシナリオの変更を反映する場合は、必要なデータを退避したうえでボリュームを作り直す。
次の`down -v`はPATを含むローカルDBデータを削除する。

```powershell
docker compose --profile app down -v
docker compose --profile app up -d --build
```
