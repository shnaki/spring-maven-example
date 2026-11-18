# Azure PipelinesからS3にファイルを置く

`azure-pipelines-s3.yml`を手動実行すると、OIDCでAWSに接続して、小さな`hello.txt`を既存のS3バケットへアップロードします。ファイルには挨拶とBuild IDが入ります。保存先は`pipeline-test/<BuildId>/hello.txt`です。既存のMavenビルド用Pipelineとは別に登録します。

## 1. AWSと接続を準備する

1. S3バケットを作成し、バケット名とリージョンを控えます。Block Public Accessは有効のままで構いません。この例はSSE-S3（AES256）暗号化を指定します。
2. [OIDC接続手順](azure-devops-aws-oidc.md)の3〜4節に従って、AWS Toolkit、AWS Service Connection、IAM OIDC Provider、IAMロールの信頼ポリシーを設定します。接続名の例は`aws-oidc`です。アクセスキーは不要です。
3. そのIAMロールに、以下の権限ポリシーを追加します。アップロード先は`shnaki-work-apne1-dev`（東京リージョン：`ap-northeast-1`）です。

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::shnaki-work-apne1-dev/pipeline-test/*"
    }
  ]
}
```

この例でPipelineが使うS3権限は、指定プレフィックスへの書き込みだけです。認証確認の`sts get-caller-identity`に権限追加は不要です。バケットの作成・一覧取得・読み取り・削除は行いません。SSE-KMSを必須とするバケットを使う場合は、暗号化指定とKMSの権限を別途合わせる必要があります。

## 2. Pipelineを登録して実行する

1. このYAMLをAzure Reposに反映します。
2. Azure DevOpsで **Pipelines → New pipeline → Azure Repos Git** を開き、対象リポジトリを選びます。
3. **Existing Azure Pipelines YAML file** で対象ブランチと`/azure-pipelines-s3.yml`を指定します。
4. 実行時に以下のパラメーターを指定します。

| パラメーター | 指定値 |
| --- | --- |
| `awsServiceConnection` | 作成したAWS Service Connection名。初期値は`aws-oidc` |
| `awsRegion` | バケットのリージョン。初期値は`ap-northeast-1` |
| `s3Bucket` | 初期値は`shnaki-work-apne1-dev`。変更時は`s3://`やパスを付けずに入力 |

5. このPipelineにService Connectionの利用を許可し、実行します。Microsoftホストのエージェント利用枠も必要です。

自動トリガーは無効です。以降は **Run pipeline** で実行します。JDKやMavenビルドは不要です。

## 3. 結果を確認する

ログの`get-caller-identity`でAWSアカウントと引き受けたロールを確認します。`put-object`が成功するとETagなどが出力され、最後に保存先のS3 URIが表示されます。

S3コンソールで対象バケットの`pipeline-test/<BuildId>/hello.txt`を開き、ダウンロードして内容を確認します。コンソール操作には、操作するユーザー側の一覧・読み取り権限が必要です。不要になったファイルはコンソールから削除できます。

`AssumeRoleWithWebIdentity`で失敗する場合はOIDC Providerとロールの信頼条件を、`PutObject`で`AccessDenied`になる場合は権限ポリシーのバケット名・プレフィックスとバケットポリシーを確認します。

実際のAWS環境へのアップロードは未検証です。仕様は[AWS Shell Scriptタスク](https://docs.aws.amazon.com/vsts/latest/userguide/awsshell.html)と[AWS公式のOIDC連携手順](https://aws.amazon.com/blogs/modernizing-with-aws/how-to-federate-into-aws-from-azure-devops-using-openid-connect/)を参照しています。
