# 初期構成と責務

単一Mavenモジュールとし、`com.example.transportrag` 以下を機能ごとに分割する。

```text
.
├── pom.xml                         # Java 25 / Spring Boot 4.1 / Spring AI / MyBatis
├── compose.yaml                    # PostgreSQL、任意でアプリも起動
├── Dockerfile                      # Java 25によるビルドと実行
├── .env.example                    # ローカル開発用設定のひな形
├── docker/postgres/
│   ├── Dockerfile                  # PostgreSQL 17 + PGroonga + Apache AGE
│   └── init/
│       ├── 001-schema.sql          # ユーザー、プロジェクト、PAT、知識、関係、拡張
│       ├── 002-demo.sql            # 架空の青空運送の知識項目と関係
│       ├── 003-project-access.sql  # 所属とプロジェクト別の知識割当
│       └── 004-audit.sql           # MCPで取得されたリソースの監査ログ
├── demo/scenario.json              # 読みやすいシナリオ原本（全データは架空）
├── demo/project-access.json        # 架空ユーザー、所属、知識の公開範囲
├── src/main/java/com/example/transportrag/
│   ├── TransportRagApplication.java
│   ├── auth/                      # PATのハッシュ照合、有効期限・失効確認
│   ├── audit/                     # 取得リソースの監査ログ記録
│   ├── config/                    # Spring Security設定
│   ├── mcp/                       # 所属確認と4つのMCP知識検索ツール
│   ├── project/                   # 所属プロジェクトの取得
│   └── search/                    # 検索サービス、モデル、Mapper
├── src/main/resources/
│   ├── application.yml
│   └── mappers/                   # MapperのSQLはすべてXMLに記述
├── src/test/java/com/example/transportrag/
├── scripts/
│   ├── create-pat.ps1              # PAT発行、90日有効
│   └── verify-db.sql               # 拡張と3種類の検索の確認
└── docs/architecture.md
```

## 検索の設計

実装規約：MapperのSQLは必ず`src/main/resources/mappers/`以下のXMLに記述する。
`@Select`、`@Insert`などSQLアノテーションは使用禁止。Lombokも使用禁止。
コンストラクタやアクセサはJavaで明示的に実装する。

- 属性検索：`knowledge_items` の種別、地域、状態、有効期間を使う。
- 全文検索：PGroongaで日本語のタイトル、本文、別名を検索する。
- 関係探索：`knowledge_relations` を再帰検索し、規程から担当組織・工程・サービス・ニーズ・KPI・市場仮説へ移動する。同じ関係をApache AGEの`business_knowledge`にも格納し、DB検証でCypher検索を確認する。
- MCP層は検索サービスを呼び、コード、出典、有効期間、主管組織を返す。SQLやCypherの自由入力は公開しない。
- `knowledge_item_projects`と`user_project_memberships`をSQLの`EXISTS`条件で照合する。取得後のJavaフィルターに依存しない。

関係探索は有向関係を双方向にたどる。定義と逆向きにたどった結果は関係名へ`INVERSE_`を付け、元の意味を失わないようにする。

MCPには`list_my_projects`、`search_business_knowledge`、`get_business_knowledge`、`explore_business_relationships`を公開する。
検索結果を統合して文章を生成する処理は呼び出し元エージェントが担う。
LLMによる回答生成は呼び出し元エージェントが担い、この初期構成にLLM APIキーは不要。

## デモシナリオ

架空の青空運送が、北海道向け冷蔵配送の品質改善と共同配送による収益・現場負荷の改善を検討するシナリオである。
コード体系、用語集、組織構造、業務フロー、商品・サービス、荷主、規程、経営・取引先・現場ニーズ、KPI、業界・競合動向を含む。
業界・競合動向は事実情報ではなく、意思決定の検索デモ用に作った観測・仮説である。

状態は`APPROVED`（確定情報）、`PROPOSED`（ニーズ・提案）、`OBSERVATION`（外部環境の仮説）、`RETIRED`（廃止）を区別する。
有効期間検索により、旧POD規程と2026年4月以降の現行規程を基準日ごとに分離できる。

### プロジェクト別シナリオ

- `PRJ-COLD-HOKKAIDO`：冷蔵温度管理、北海道冬季配送、北星フーズ、温度逸脱、コールドチェーン関連情報を公開する。
- `PRJ-JOINT-DELIVERY`：共同配送、混載条件、積載率、利益率、環境配慮調達、共同配送競合の情報を公開する。
- `PRJ-DIGITAL-POD`：配送可視化、電子POD、通信断、追跡ニーズ、デジタル競合の情報を公開する。

コード体系や基本組織など一部の共通知識は各プロジェクトへ個別に割り当てる。
共通扱いも明示的な割当であり、所属のないユーザーへ無条件公開する情報は設けない。
デモ担当者の可視件数は冷蔵42件、共同配送39件、配送DX41件で、横断マネージャーは58件すべてを閲覧できる。

## 認証

Stateless Streamable HTTPのすべての `POST /mcp` に `Authorization: Bearer <PAT>` を要求する。
MCPセッションと接続状態はサーバーに保持しない。Cookieログインと人向け管理画面は設けない。
DBにはSHA-256ハッシュだけを保存し、各HTTPリクエストで有効期限と失効状態を確認する。
認証後のsubjectを検索SQLへ渡し、毎回DB上の有効なプロジェクト所属を照合する。
PATの失効やプロジェクト所属の変更は、次のMCPリクエストから反映される。

Stateless方式ではサーバーからクライアントへのelicitation、sampling、pingを利用できない。
本プロジェクトの検索ツールはこれらの機能に依存しない。

## 監査ログ

`list_my_projects`、検索、コード指定取得、関係探索が返したリソースを
`resource_access_audit_log`へ記録する。
監査行には操作ID、取得時刻、認証subject、操作種別、指定プロジェクト、
リソース種別・コードを保存する。関係探索では起点コードと関係名も保存する。

検索語、PAT、知識本文は保存しない。結果が0件の呼び出しは取得されたリソースがないため行を作らない。
検索結果と監査書き込みは同じDBトランザクションで処理し、監査書き込みに失敗した場合はツール呼び出しも失敗させる。
アプリケーションには監査行を更新・削除する機能を設けない。

## DB初期化と運用範囲

初期SQLは空のDockerボリュームを初回起動した時にだけ実行される。
既存ボリュームに対してSQLファイルの変更は自動反映されない。
今後スキーマを更新する際はマイグレーション運用を追加する。
開発環境ではComposeのDB管理ユーザーを利用する。実運用への展開時は権限を分離する。
AGEはPG17向け公開タグ `PG17/v1.6.0-rc0` を固定し、PGroongaは公式APTリポジトリを利用する。

## 参照

- [Spring AI 2.0とSpring Boot 4.1の互換性](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)
- [Spring AI Stateless MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html)
- [MyBatis Spring Boot Starter 4.0](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [PGroonga Debianインストール](https://pgroonga.github.io/install/debian.html)
- [Apache AGEリリース](https://age.apache.org/release-notes/)

Spring AI 2.0のStateless Streamable HTTPを採用し、MCPリクエストを単一の `POST /mcp` で処理する。
