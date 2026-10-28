# Azure DevOpsからAWSへOIDCで接続する

Azure PipelinesからAWSへデプロイするには、OIDCでIAMロールの一時認証情報を取得し、その権限でAWS APIを呼び出します。長期アクセスキーをAzure DevOpsに保存する必要はありません。

これは構成と設定方法を理解するための学習資料です。現在のリポジトリのパイプラインはMavenのテスト用CIのみです。以下の例は実環境では未検証で、AWSリソースやデプロイ用パイプラインは作成していません。

公式資料の確認日：2026-10-28。

## 1. 全体像と役割

```text
Azure Pipelinesのジョブ（エージェント上で実行）
  → AWS Service Connectionを参照
  → Azure DevOpsがOIDCトークンを発行
  → AWS STSのAssumeRoleWithWebIdentityへ渡す
  → AWSが署名・発行元・信頼条件を検証
  → IAMロールの一時認証情報を返す
  → AWS CLIなどでECR・ECSのAPIを呼ぶ
```

| 要素 | 役割 |
| --- | --- |
| エージェント | ビルドやAWS CLIを実行するマシン |
| AWS Toolkit | Azure PipelinesのタスクからAWS認証や操作を行う拡張機能 |
| Service Connection | 使用するIAMロールと認証方式を保持するAzure DevOpsの接続設定 |
| IAM OIDC Provider | AWSが信頼するトークン発行元の登録 |
| IAMロールの信頼ポリシー | 誰がそのロールを引き受けられるか |
| IAMロールの権限ポリシー | 引き受けたロールで何を操作できるか |

初期のProvider・ロール作成は管理者や基盤構築用の処理が担当します。日々のデプロイ用ロールに、自身の信頼関係を作成・変更する権限を与える必要はありません。

このフローは[AWS公式のOIDC連携手順](https://aws.amazon.com/blogs/modernizing-with-aws/how-to-federate-into-aws-from-azure-devops-using-openid-connect/)を基にしています。

## 2. 前提と設定値

必要なものはAWSのIAM設定権限、Azure DevOps Servicesのプロジェクト、拡張機能とService Connectionを管理する権限、ジョブを実行できるエージェント枠です。

この資料ではMicrosoftホストのUbuntu 24.04を想定します。`Organization settings → Pipelines → Parallel jobs`で利用枠を確認します。プールの表示だけでは、無料枠の付与や実行可能性は判断できません。[実行枠の公式説明](https://learn.microsoft.com/en-us/azure/devops/pipelines/licensing/concurrent-jobs?view=azure-devops)

例の`<...>`は次の値に置き換えます。組織のGUIDと表示名は別の値です。

| プレースホルダー | 内容 |
| --- | --- |
| `<AWS_ACCOUNT_ID>` | 接続先AWSアカウントの12桁ID |
| `<ORG_ID>` | Azure DevOps組織のGUID |
| `<ORG_NAME>` | Azure DevOps組織名 |
| `<PROJECT_NAME>` | Azure DevOpsプロジェクト名 |
| `<SERVICE_CONNECTION_NAME>` | AWS Service Connection名。YAMLの`awsCredentials`にも同じ値を使用 |
| `<ROLE_NAME>` | 作成するIAMロール名 |
| `<AWS_REGION>` | 操作対象のAWSリージョン |

ロールARNは`arn:aws:iam::<AWS_ACCOUNT_ID>:role/<ROLE_NAME>`です。以下は標準のAWSパーティションで、パスを付けないロールを想定します。

## 3. Azure DevOps側：接続設定を用意する

1. 組織に[AWS Toolkit for Azure DevOps](https://marketplace.visualstudio.com/items?itemName=AmazonWebServices.aws-vsts-tools)をインストールします。OIDC対応版（1.15以降）を使用します。
2. `Project settings → Service connections → New service connection → AWS`を開きます。
3. `Role to assume`に作成予定のロールARNを入力し、`Use OIDC`を有効にします。
4. Access Key IDとSecret Access Keyは空欄にし、接続名を設定します。
5. 全パイプラインへのアクセス許可を有効にせず、認証確認用・デプロイ用の必要なパイプラインだけに利用を許可します。

この時点でAWS側が未設定なら、認証はまだ成功しません。後述の確認用タスクを実行すると、Toolkitのログで`issuer`・`audience`・`subject`を確認できます。AWS公式手順も初回の失敗ログからこれらの値を取得します。トークン本体や一時認証情報を出力・保存する必要はありません。

## 4. AWS側：発行元と信頼関係を設定する

### OIDC Provider

IAMの`Identity providers → Add provider → OpenID Connect`で、ログの値に合わせて登録します。AWS Toolkitの公式例で使われる値は次のとおりです。

| 項目 | 例と意味 |
| --- | --- |
| Provider URL / `iss` | `https://vstoken.dev.azure.com/<ORG_ID>`：発行元 |
| Audience / `aud` | `api://AzureADTokenExchange`：トークンの対象 |
| Subject / `sub` | `sc://<ORG_NAME>/<PROJECT_NAME>/<SERVICE_CONNECTION_NAME>`：接続の識別子 |

`sub`はProvider作成画面ではなく、ロールの信頼ポリシーに設定します。名前や大文字・小文字を推測せず、発行された値と一致させます。

### IAMロールの信頼ポリシー

Web identity用のロールを作り、登録したProviderを選択します。次の例は対象のService Connectionだけを信頼します。Provider ARNと条件キーのホスト・パスを一致させます。

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/vstoken.dev.azure.com/<ORG_ID>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "vstoken.dev.azure.com/<ORG_ID>:aud": "api://AzureADTokenExchange",
          "vstoken.dev.azure.com/<ORG_ID>:sub": "sc://<ORG_NAME>/<PROJECT_NAME>/<SERVICE_CONNECTION_NAME>"
        }
      }
    }
  ]
}
```

この信頼条件はService Connection単位です。特定ブランチからの実行制限まで自動的に表現されるわけではありません。Azure DevOps側の接続利用権限やブランチ制御も必要です。[AWS Toolkitの認証仕様](https://github.com/aws/aws-toolkit-azure-devops)

### 権限ポリシー

まず認証だけを確認するなら、ECRやECSの権限は不要です。`sts get-caller-identity`自体は権限付与なしで呼び出せます。[STS API仕様](https://docs.aws.amazon.com/STS/latest/APIReference/API_GetCallerIdentity.html)

デプロイを追加する場合は、対象リポジトリへのECR push、ECSタスク定義の登録、対象サービスの更新・状態参照などを許可します。リソース制限に対応する操作は対象ARNに限定します。タスク定義にロールを指定する場合の`iam:PassRole`も対象ロールに限定します。パイプライン用ロールと、ECSのタスク実行ロール・アプリ用タスクロールは役割が異なります。

### 発行元の変更情報について

MicrosoftはAzure公開クラウドの単一テナントEntraアプリやマネージドIDを使うService Connectionについて、旧Azure DevOps issuerの2027年7月1日終了を案内しています。この告知から、AWS ToolkitのAWS接続も同日に停止する、またはEntra issuerへ置換すればよいとは断定できません。

本資料の例は確認日時点のAWS Toolkitの記載に合わせています。将来設定する際は、実際の`iss`・`aud`・`sub`とToolkitの最新資料を再確認してください。[Microsoftの告知と対象範囲](https://devblogs.microsoft.com/devops/retirement-of-azure-devops-issuer-in-workload-identity-federation-service-connections/)

## 5. 認証確認用のYAML

既存の`azure-pipelines.yml`とは別の、手動実行を想定した例です。実際に登録する場合はプレースホルダーを置換し、このパイプラインに接続の利用を許可します。AWS CLIが利用可能なエージェントを使用します。

```yaml
trigger: none
pr: none

pool:
  vmImage: ubuntu-24.04

steps:
  - checkout: none
  - task: AWSCLI@1
    displayName: Check AWS identity via OIDC
    inputs:
      awsCredentials: '<SERVICE_CONNECTION_NAME>'
      regionName: '<AWS_REGION>'
      awsCommand: sts
      awsSubCommand: get-caller-identity
```

`awsCredentials`に指定するのは秘密鍵ではなくService Connection名です。Toolkitがトークン取得とロール引き受けを担当します。別の通常のスクリプトタスクに、認証情報が自動的に引き継がれるとは想定しないでください。

成功時は出力の`Account`が接続先ID、`Arn`が`arn:aws:sts::<AWS_ACCOUNT_ID>:assumed-role/<ROLE_NAME>/...`になっていることを確認します。これは認証の確認であり、デプロイ権限やアプリの稼働を確認したことにはなりません。

| 症状 | 確認する場所 |
| --- | --- |
| ジョブが開始されない | エージェント枠、プール利用権限、待機中のジョブ |
| 接続が見つからない・使用できない | 接続名、対象プロジェクト、パイプラインへの利用許可 |
| `InvalidIdentityToken` | 発行元、Audience、Provider登録、トークン有効期限 |
| `AssumeRoleWithWebIdentity`で`AccessDenied` | ロールARN、信頼ポリシーのProvider・`aud`・`sub` |
| 認証後のECR/ECS操作が`AccessDenied` | ロールの権限、対象ARN、SCPやPermissions boundaryなどの制約 |

## 6. ECR・ECSへデプロイする場合の全体像

```text
テスト → MavenでJAR作成 → コンテナ作成
  → OIDC認証 → ECRへpush
  → 新しいイメージを指定したECSタスク定義を登録
  → 開発環境のサービスを更新 → 安定化・HTTP動作確認
  → 本番承認 → 本番用ロールでOIDC認証
  → 同じイメージを本番へ反映 → 安定化・HTTP動作確認
```

- コミットIDのタグやdigestで成果物を識別します。本番で再ビルドせず、検証した同じイメージを反映します。
- 開発用・本番用のロールとService Connectionを分離します。本番承認はAzure DevOpsのEnvironmentなどのApprovals and checksで設定し、承認を通るデプロイジョブとして構成します。
- ECSの更新要求が受理された時点で成功とせず、サービス安定化とアプリの動作を確認します。
- ECSのデプロイ失敗検出とロールバックを設定します。例えばローリング更新ではdeployment circuit breakerとrollbackを利用できます。[ECSの更新・失敗検出](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/update-service-console-v2.html)
- OIDCは認証の仕組みです。プライベートなエンドポイントへのネットワーク経路は別途必要です。

承認設定の詳細は[Azure PipelinesのApprovals and checks](https://learn.microsoft.com/en-us/azure/devops/pipelines/process/approvals?view=azure-devops)を参照してください。

ECR、ECS、ネットワーク、ログ、ヘルスチェックなどの実行基盤は事前に用意する前提です。本資料ではその構築手順、Dockerfile、完全なデプロイYAML、Terraformは扱いません。また、このTODOアプリはメモリにデータを保存するため、再起動でデータが消え、複数タスク間でも共有されません。
