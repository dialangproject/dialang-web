const { CfnOutput, Stack, Duration, RemovalPolicy } = require('aws-cdk-lib');
const iam = require('aws-cdk-lib/aws-iam');
const lambda = require('aws-cdk-lib/aws-lambda');
const apigw = require('aws-cdk-lib/aws-apigateway');
const dynamodb = require('aws-cdk-lib/aws-dynamodb');
const s3 = require('aws-cdk-lib/aws-s3');
const s3deploy = require('aws-cdk-lib/aws-s3-deployment');

class DialangWebStack extends Stack {

  constructor(scope, id, props) {

    super(scope, id, props);

    const setTLFunction = this.createSetTLFunction();

    const websiteBucket = this.createWebsiteBucket(props);

    const [ dbImportBucket, dbImportBucketDeployment] = this.createDBImportBucket(props);

    // Create IAM role for DynamoDB import
    const importRole = new iam.Role(this, 'DynamoDbImportRole', {
      assumedBy: new iam.ServicePrincipal('dynamodb.amazonaws.com'),
    });

    // Grant permissions to read from S3 bucket
    dbImportBucket.grantRead(importRole);

    // Additional permissions for DynamoDB import service
    importRole.addToPolicy(new iam.PolicyStatement({
      actions: [
        'dynamodb:DescribeTable',
        'dynamodb:CreateTable',
        'dynamodb:UpdateTable',
        'dynamodb:PutItem',
        'dynamodb:BatchWriteItem',
        "dynamodb:ImportTable",
        "dynamodb:DescribeImport",
        's3:GetObject',
        's3:ListBucket'
      ],
      resources: [
        dbImportBucket.bucketArn,
        `${dbImportBucket.bucketArn}/*`,
        `arn:aws:dynamodb:${this.region}:${this.account}:table/*`
      ]
    }));

    const dataCaptureTable = this.createDataCaptureTable();
    const vsptWordsTable = this.createVSPTWordsTable(dbImportBucket);
    const vsptBandsTable = this.createVSPTBandsTable(dbImportBucket);
    const saWeightsTable = this.createSAWeightsTable(dbImportBucket);
    const saGradingTable = this.createSAGradingTable(dbImportBucket);
    const preestWeightsTable = this.createPreestWeightsTable(dbImportBucket);
    const preestAssignTable = this.createPreestAssignTable(dbImportBucket);
    const bookletDataTable = this.createBookletDataTable(dbImportBucket);
    const bookletBasketTable = this.createBookletBasketTable(dbImportBucket);

    // Ensure the CSV file is deployed before the table tries to import it
    vsptWordsTable.node.addDependency(dbImportBucketDeployment);
    vsptBandsTable.node.addDependency(dbImportBucketDeployment);
    saWeightsTable.node.addDependency(dbImportBucketDeployment);
    saGradingTable.node.addDependency(dbImportBucketDeployment);
    preestWeightsTable.node.addDependency(dbImportBucketDeployment);
    preestAssignTable.node.addDependency(dbImportBucketDeployment);
    bookletDataTable.node.addDependency(dbImportBucketDeployment);
    bookletBasketTable.node.addDependency(dbImportBucketDeployment);

    dataCaptureTable.grantWriteData(setTLFunction);

    const scoreVSPTFunction = this.createScoreVSPTFunction();
    vsptWordsTable.grantReadData(scoreVSPTFunction);
    vsptBandsTable.grantReadData(scoreVSPTFunction);
    dataCaptureTable.grantWriteData(scoreVSPTFunction);

    const scoreSAFunction = this.createScoreSAFunction();
    dataCaptureTable.grantWriteData(scoreSAFunction);
    saWeightsTable.grantReadData(scoreSAFunction);
    saGradingTable.grantReadData(scoreSAFunction);

    const startTestFunction = this.createStartTestFunction();
    dataCaptureTable.grantWriteData(startTestFunction);
    preestWeightsTable.grantReadData(startTestFunction);
    preestAssignTable.grantReadData(startTestFunction);
    bookletDataTable.grantReadData(startTestFunction);
    bookletBasketTable.grantReadData(startTestFunction);

    const api = new apigw.RestApi(
      this,
      "ApiGatewayS3Proxy",
      {
        restApiName: "StaticWebsite",
        endpointTypes: [apigw.EndpointType.REGIONAL],
        binaryMediaTypes: [
          "image/avif",
          "image/webp",
          "image/gif",
          "image/png",
          "image/apng",
          "image/jpeg",
          "image/svg+xml",
          "application/font-woff2",
          "application/font-woff",
          "font/woff",
          "font/woff2",
        ],
      }
    );

    const apiGatewayS3ReadRole = new iam.Role(
      this,
      "ApiGatewayS3ReadRole",
      {
        assumedBy: new iam.ServicePrincipal("apigateway.amazonaws.com"),
        path: "/",
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess"),
          iam.ManagedPolicy.fromAwsManagedPolicyName("AWSLambdaExecute")
        ],
      }
    );

    const indexPageIntegration = new apigw.AwsIntegration({
      service: "s3",
      integrationHttpMethod: "GET",
      path: `${websiteBucket.bucketName}/index.html`,
      options: {
        credentialsRole: apiGatewayS3ReadRole,
        passthroughBehavior: apigw.PassthroughBehavior.WHEN_NO_MATCH,
        integrationResponses: [
          {
            statusCode: "200",
            responseParameters: {
              "method.response.header.Content-Type": "integration.response.header.Content-Type",
              "method.response.header.Timestamp": "integration.response.header.Date"
            },
          },
        ],
      },
    });

    const assetsIntegration = new apigw.AwsIntegration({
      service: "s3",
      integrationHttpMethod: "GET",
      path: `${websiteBucket.bucketName}/assets/{path}`,
      options: {
        credentialsRole: apiGatewayS3ReadRole,
        passthroughBehavior: apigw.PassthroughBehavior.WHEN_NO_MATCH,
        requestParameters: {
          "integration.request.path.path": "method.request.path.path"
        },
        integrationResponses: [
          {
            statusCode: "200",
            responseParameters: {
              "method.response.header.Content-Type": "integration.response.header.Content-Type",
              "method.response.header.Timestamp": "integration.response.header.Date"
            },
          },
        ],
      },
    });

    const contentIntegration = new apigw.AwsIntegration({
      service: "s3",
      integrationHttpMethod: "GET",
      path: `${websiteBucket.bucketName}/content/{path}`,
      options: {
        credentialsRole: apiGatewayS3ReadRole,
        passthroughBehavior: apigw.PassthroughBehavior.WHEN_NO_MATCH,
        requestParameters: {
          "integration.request.path.path": "method.request.path.path"
        },
        integrationResponses: [
          {
            statusCode: "200",
            responseParameters: {
              "method.response.header.Content-Type": "integration.response.header.Content-Type",
              "method.response.header.Timestamp": "integration.response.header.Date"
            },
          },
        ],
      },
    });

    const methodOptions = { methodResponses: [
      { statusCode: '200', responseParameters: {"method.response.header.Content-Type": true, "method.response.header.Timestamp": true}},
      { statusCode: '400' },
      { statusCode: '500' }
    ]};

    api.root.addMethod("GET", indexPageIntegration, methodOptions);

    api.root.addResource("settl").addMethod("POST", new apigw.LambdaIntegration(setTLFunction), methodOptions);
    api.root.addResource("scorevspt").addMethod("POST", new apigw.LambdaIntegration(scoreVSPTFunction), methodOptions);
    api.root.addResource("scoresa").addMethod("POST", new apigw.LambdaIntegration(scoreSAFunction), methodOptions);
    api.root.addResource("starttest").addMethod("POST", new apigw.LambdaIntegration(startTestFunction), methodOptions);

    const assets = api.root.addResource("assets");
    assets.addResource("{path+}").addMethod("GET", assetsIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})

    const content = api.root.addResource("content");
    content.addResource("{path+}").addMethod("GET", contentIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})
  }

  createDataCaptureTable() {

    return new dynamodb.Table(this, 'DialangDataCaptureTable', {
      tableName: 'dialang-data-capture',
      partitionKey: { name: "session_id", type: dynamodb.AttributeType.STRING },
    });
  }

  createVSPTWordsTable(fromBucket) {

    return new dynamodb.Table(this, "DialangVSPTWordsTable", {
      tableName: "dialang-vspt-words",
      partitionKey: { name: "test_language", type: dynamodb.AttributeType.STRING },
      sortKey: { name: "word_id", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-vspt-words.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createVSPTBandsTable(fromBucket) {

    return new dynamodb.Table(this, "DialangVSPTBandsTable", {
      tableName: "dialang-vspt-bands",
      partitionKey: { name: "test_language", type: dynamodb.AttributeType.STRING },
      sortKey: { name: "level", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-vspt-bands.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createSAWeightsTable(fromBucket) {

    return new dynamodb.Table(this, "DialangSAWeightsTable", {
      tableName: "dialang-sa-weights",
      partitionKey: { name: "skill", type: dynamodb.AttributeType.STRING },
      sortKey: { name: "wid", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-sa-weights.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createSAGradingTable(fromBucket) {

    return new dynamodb.Table(this, "DialangSAGradingTable", {
      tableName: "dialang-sa-grading",
      partitionKey: { name: "skill", type: dynamodb.AttributeType.STRING },
      sortKey: { name: "rsc", type: dynamodb.AttributeType.NUMBER },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-sa-grading.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createPreestWeightsTable(fromBucket) {

    return new dynamodb.Table(this, "DialangPreestWeightsTable", {
      tableName: "dialang-preest-weights",
      partitionKey: { name: "key", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-preest-weights.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createPreestAssignTable(fromBucket) {

    return new dynamodb.Table(this, "DialangPreestAssignTable", {
      tableName: "dialang-preest-assignments",
      partitionKey: { name: "key", type: dynamodb.AttributeType.STRING },
      sortKey: { name: "pe", type: dynamodb.AttributeType.NUMBER },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-preest-assignments.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createBookletDataTable(fromBucket) {

    return new dynamodb.Table(this, "DialangBookletDataTable", {
      tableName: "dialang-booklet-data",
      partitionKey: { name: "booklet_id", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-booklet-data.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }

  createBookletBasketTable(fromBucket) {

    return new dynamodb.Table(this, "DialangBookletBasketsTable", {
      tableName: "dialang-booklet-baskets",
      partitionKey: { name: "booklet_id", type: dynamodb.AttributeType.STRING },
      importSource: {
        bucket: fromBucket,
        keyPrefix: "dialang-booklet-baskets.csv",
        inputFormat: dynamodb.InputFormat.csv(),
        inputCompressionType: dynamodb.InputCompressionType.NONE,
      },
    });
  }


  createSetTLFunction() {

    return new lambda.Function(this, 'SetTL', {
      code: lambda.Code.fromAsset('lambdas/settl'),
      runtime: lambda.Runtime.NODEJS_22_X,
      handler: 'index.handler'
    });
  }

  createScoreVSPTFunction() {

    return new lambda.Function(this, 'ScoreVSPT', {
      code: lambda.Code.fromAsset('lambdas/scorevspt'),
      runtime: lambda.Runtime.NODEJS_22_X,
      handler: 'index.handler'
    });
  }

  createScoreSAFunction() {

    return new lambda.Function(this, 'ScoreSA', {
      code: lambda.Code.fromAsset('lambdas/scoresa'),
      runtime: lambda.Runtime.NODEJS_22_X,
      handler: 'index.handler'
    });
  }

  createStartTestFunction() {

    return new lambda.Function(this, 'StartTest', {
      code: lambda.Code.fromAsset('lambdas/starttest'),
      runtime: lambda.Runtime.NODEJS_22_X,
      handler: 'index.handler'
    });
  }

  /**
   * Creates a bucket for static website content. Dialang's pre generated html fragments will all
   * be deployed to this bucket.
   */
  createWebsiteBucket(props) {

    const websiteBucket = new s3.Bucket(this, "WebsiteBucket");

    // Deploy site contents to S3 bucket
    new s3deploy.BucketDeployment(this, "DeployWebsite", {
      sources: [s3deploy.Source.asset("static-site")],
      destinationBucket: websiteBucket,
      destinationKeyPrefix: props.staticContentPrefix, // optional prefix in destination bucket
    });

    return websiteBucket;
  }

  createDBImportBucket(props) {

    const bucket = new s3.Bucket(this, "CSVBucket", {
      removalPolicy: RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
      accessControl: s3.BucketAccessControl.PRIVATE,
    });

    // Deploy site contents to S3 bucket
    const deployment = new s3deploy.BucketDeployment(this, "DeployDBIImportFiles", {
      sources: [ s3deploy.Source.asset("db-import") ],
      destinationBucket: bucket,
      retainOnDelete: false,
      destinationKeyPrefix: props.staticContentPrefix, // optional prefix in destination bucket
    });

    return [ bucket, deployment ];
  }
}

module.exports = { DialangWebStack }
