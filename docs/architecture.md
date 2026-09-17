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
│       ├── 001-schema.sql          # 拡張、PAT、業務規程、インデックス、グラフ
│       └── 002-demo.sql            # 架空の青空運送の規程と関係
├── src/main/java/com/example/transportrag/
│   ├── TransportRagApplication.java
│   ├── auth/                      # PATのハッシュ照合、有効期限・失効確認
│   ├── config/                    # Spring Security設定
│   ├── mcp/                       # MCPツールの追加先（現時点はパッケージ定義）
│   └── search/                    # 検索サービス・Mapper追加先（同上）
├── src/main/resources/
│   ├── application.yml
│   └── mappers/auth/PatMapper.xml  # MapperのSQLはXMLに記述
├── src/test/java/com/example/transportrag/auth/
├── scripts/
│   ├── create-pat.ps1              # PAT発行、90日有効
│   └── verify-db.sql               # 拡張と3種類の検索の確認
└── docs/architecture.md
```

## 検索の設計

実装規約：MapperのSQLは必ず`src/main/resources/mappers/`以下のXMLに記述する。
`@Select`、`@Insert`などSQLアノテーションは使用禁止。Lombokも使用禁止。
コンストラクタやアクセサはJavaで明示的に実装する。

- 属性検索：`business_rules` の `category`、`region`、`effective_from` を使う。
- 全文検索：PGroongaで日本語の `title`、`content` を検索する。
- グラフ検索：AGEの `transport_rules` 内で `Rule` ノードと `EXTENDS` / `RELATED_TO` の関係を検索する。`rule_id` で業務規程と対応させる。
- MCP層は検索サービスを呼び、根拠となる規程IDと本文を返す。SQLやCypherの自由入力は公開しない。

今回は初期セットアップとして認証・DB・データ・SSE設定までを実装した。
検索SQLのサンプルはあるが、検索サービスとMCP検索ツールの実装、検索結果の統合は後続開発とする。
LLMによる回答生成は呼び出し元エージェントが担い、この初期構成にLLM APIキーは不要。

## 認証

すべてのHTTPリクエストに `Authorization: Bearer <PAT>` を要求する。
SSE接続のGETとメッセージ送信のPOSTの両方に付与する。
ステートレス認証とし、Cookieログインと人向け管理画面は設けない。
DBにはSHA-256ハッシュだけを保存し、各HTTPリクエストで有効期限と失効状態を確認する。
既存のSSE接続をPAT失効時に強制切断する処理は初期構成に含めない。

## DB初期化と運用範囲

初期SQLは空のDockerボリュームを初回起動した時にだけ実行される。
既存ボリュームに対してSQLファイルの変更は自動反映されない。
今後スキーマを更新する際はマイグレーション運用を追加する。
開発環境ではComposeのDB管理ユーザーを利用する。実運用への展開時は権限を分離する。
AGEはPG17向け公開タグ `PG17/v1.6.0-rc0` を固定し、PGroongaは公式APTリポジトリを利用する。

## 参照

- [Spring AI 2.0とSpring Boot 4.1の互換性](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)
- [MCP Server Boot StarterのSSE設定](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MyBatis Spring Boot Starter 4.0](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [PGroonga Debianインストール](https://pgroonga.github.io/install/debian.html)
- [Apache AGEリリース](https://age.apache.org/release-notes/)

Spring AI 2.0ではSSEは非推奨だが、本プロジェクトではREADMEの要件に合わせて明示的にSSEを選択する。
