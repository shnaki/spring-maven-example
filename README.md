# Java・Spring・Maven学習用TODOアプリ

ThymeleafでHTMLを生成する、小さな画面付きTODOアプリです。追加、完了／未完了の切替、削除ができます。データはメモリに保存され、アプリを終了すると消えます。

## 起動する

JDK 25が必要です。Maven Wrapperを同梱しているため、Mavenの別途インストールは不要です。初回はMavenと依存ライブラリをダウンロードするため、インターネット接続が必要です。

このフォルダーでPowerShellを開き、実行します。

```powershell
java -version
javac -version
.\mvnw.cmd spring-boot:run
```

[http://localhost:8080](http://localhost:8080) をブラウザーで開きます。停止するときは実行したターミナルで `Ctrl+C` を押します。

macOS / Linuxでは、JDK 25を用意して `sh ./mvnw spring-boot:run` を実行してください。

## 操作する

1. タイトルを入力して「追加」を押します。
2. 「完了にする」で完了にし、「未完了に戻す」で戻します。
3. 「削除」で一覧から削除します。

タイトルの前後の空白は取り除きます。空欄・空白だけの入力は登録できません。ブラウザーの必須入力チェックに加え、サーバーでも検証します。初期状態は空で、TODOは追加順に表示します。ログインはなく、同じアプリにアクセスしたブラウザーは同じ一覧を共有します。

## コードを読む順番

すべてのJavaクラスは `src/main/java/com/example/todo` にあります。

| クラス・ファイル | 役割と学習ポイント |
| --- | --- |
| `TodoApplication` | `main` と `@SpringBootApplication` でSpringを起動する |
| `Todo` | ID、タイトル、完了状態を持つ `record`。変更時には新しい値を作る |
| `TodoService` | `@Service`、コレクション、追加順を保持する `LinkedHashMap`、メモリ上でのデータ操作 |
| `TodoController` | `@Controller`、コンストラクターによる依存性注入、HTTP処理、`Model`へのデータ設定 |
| `src/main/resources/templates/todos.html` | Thymeleafの `th:each`、`th:if`、`th:text`、`th:action` でHTMLを生成する |
| `src/main/resources/templates/not-found.html` | 存在しないTODOへの404画面 |
| `src/main/resources/static/style.css` | Springが配信する静的CSS |
| `src/test/java/com/example/todo` | Serviceの単体テスト、MockMvcによる画面処理のテスト、起動テスト |

ServiceはSpringが作成し、Controllerのコンストラクターに渡します。Controllerが自分で `new TodoService()` を呼ばずに、必要なオブジェクトを受け取る仕組みが「依存性注入（DI）」です。

`synchronized` は同時に届くリクエストが保存内容を壊さないようにするための指定です。`findAll()` は不変の一覧を返すので、呼び出し側が保存データを直接変更することはできません。

## リクエストから画面まで

```text
ブラウザー → TodoController → TodoService（メモリ）
                    ↓
                  Model → Thymeleaf → HTML → ブラウザー
```

一覧を開くと、ControllerがServiceからTODOを取得し、`Model` に入れて `todos` というテンプレート名を返します。Thymeleafが `todos.html` を描画します。`th:text` は入力内容をHTMLとして実行せず、文字として表示します。

フォームから追加すると、Controllerがタイトルを受け取りServiceに登録します。その後 `redirect:/todos` で一覧に移動します。更新後にGETへ移動することで、画面を再読み込みしても同じフォームを再送信しないようにしています。入力エラー時は同じ画面に日本語のエラーを表示します。

SpringはREST API専用ではありません。このアプリの `@Controller` はHTML画面を返します。JSONなどを返すAPIを作るときには `@RestController` を使います。

| HTTP | パス | 動作 |
| --- | --- | --- |
| GET | `/` | `/todos`へ移動 |
| GET | `/todos` | 一覧を表示 |
| POST | `/todos` | `title`を追加 |
| POST | `/todos/{id}/toggle` | 完了状態を切替 |
| POST | `/todos/{id}/delete` | 削除 |

存在しないIDの切替・削除は404を返します。

## Mavenを学ぶ

`pom.xml` はビルド設定です。`groupId` と `artifactId` はプロジェクトの識別子、`version` はそのバージョンです。`java.version` はJava 25を指定します。

- `spring-boot-starter-parent`：Spring Boot 4.1.1の依存バージョンとビルド設定をまとめます。
- `spring-boot-starter-webmvc`：HTTP処理、Spring MVC、組み込みWebサーバーを提供します。
- `spring-boot-starter-thymeleaf`：HTMLテンプレートを描画します。
- `scope` が `test` の依存関係：JUnitやMockMvcなど、テストで使うライブラリです。
- `spring-boot-maven-plugin`：起動コマンドと、`java -jar` で実行できるJARの作成を提供します。

```powershell
# コンパイル
.\mvnw.cmd compile

# テスト
.\mvnw.cmd test

# テストして実行可能JARを作成
.\mvnw.cmd package

# 作成したJARを起動（spring-boot:runは先に停止）
java -jar target/todo-0.0.1-SNAPSHOT.jar

# ビルド結果を削除し、再ビルド・検証
.\mvnw.cmd clean verify

# 依存ライブラリの関係を見る
.\mvnw.cmd dependency:tree
```

Mavenは `compile` → `test` → `package` の順に処理します。`package` を指定すると、その前のテストも実行します。ビルド結果は `target/` に生成されます。Wrapperが使うMavenのバージョンは `.mvn/wrapper/maven-wrapper.properties` で固定しています。

## Javadocのルール

本体（`src/main/java`）の全クラス・record・メソッド・明示的なコンストラクターに、日本語の `/** ... */` 形式でJavadocを記述します。privateメソッド、getter/setter、`@Override` が付いたメソッドも対象です。テスト、フィールド、自動生成コードは対象外です。

説明には役割や利用上の制約を記載し、引数には `@param`、戻り値があるメソッドには `@return` を付けます。recordの各構成要素も `@param` で説明します。呼び出し側が把握する必要のある例外条件は `@throws` に記載します。

`checkstyle.xml` のルールをMavenの `validate` フェーズで検査します。Javadocや必要な引数・戻り値のタグが不足すると、`compile`・`test`・`package`・`verify` も失敗します。例外条件の説明が適切かどうかは、コードレビューで確認します。

`maven-checkstyle-plugin` は3.6.0、Checkstyle本体は14.3.0に固定しています。

```powershell
# Javadocの検査のみ
.\mvnw.cmd checkstyle:check

# Javadocの検査・テスト・ビルド
.\mvnw.cmd clean verify
```

## Azure Pipelinesでテストする

`azure-pipelines.yml` は、MicrosoftホストのUbuntu 24.04エージェントでJDK 25を選択し、Maven Wrapperで `clean test` を実行します。Javadoc検査も含まれ、JUnitの結果は実行画面のTestsタブで確認できます。テスト失敗時も結果を公開します。

初回はAzure DevOpsで次の操作を行います。YAMLをプッシュしただけではPipelineは登録されません。

1. `spring-maven-example` プロジェクトの **Pipelines → New pipeline** を開く。
2. **Azure Repos Git** を選び、`spring-maven-example` リポジトリを選ぶ。
3. **Existing Azure Pipelines YAML file** を選び、ブランチ `main`、パス `/azure-pipelines.yml` を指定する。
4. **Run** で初回実行する。以後は `main` へのプッシュで自動実行される。

1回の実行は1ジョブ・最大15分です。実行中に追加のプッシュがあった場合は、次回の実行にまとめます。手動実行も可能です。Azure Reposのプルリクエスト検証は、必要に応じてブランチポリシーのBuild validationで別途設定します。

無料枠を使うには、**Organization settings → Pipelines → Parallel jobs** でMicrosoft-hostedの無料枠が有効になっていることを確認してください。非公開プロジェクトの無料枠は組織全体で1並列ジョブ・1回最大60分・月1,800分です。このYAMLは有料枠を購入したり、組織の課金設定を変更したりしません。

無料枠が未付与の場合は、[Microsoftの無料枠の有効化手順](https://learn.microsoft.com/en-us/azure/devops/pipelines/licensing/concurrent-jobs?view=azure-devops#how-do-i-enable-the-free-tier-of-parallel-jobs)を確認してください。組織側の枠がない場合は、YAMLだけでは実行できません。

## 困ったとき

- `JAVA_HOME` が設定されている場合はJDK 25のフォルダーを指すようにしてください。`.\mvnw.cmd -v` でMavenが使うJavaを確認できます。
- ポート8080が使われている場合は `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'` で起動し、`http://localhost:8081` を開きます。
- 初回ダウンロードに失敗した場合は、ネットワークやプロキシの設定を確認してください。

## 練習課題

1. 未完了のTODO件数を一覧に表示する。
2. タイトルの最大文字数を決め、Serviceの検証とテストを追加する。
3. 完了済みTODOだけを表示するフィルターを追加する。

ローカル学習用の構成です。DB、認証、外部サービスは含めていません。
