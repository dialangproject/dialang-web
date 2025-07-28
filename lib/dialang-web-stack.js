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

    const dataCaptureTable = this.createDataCaptureTable();

    const setTLFunction = this.createSetTLFunction();
    dataCaptureTable.grantWriteData(setTLFunction);

    const scoreVSPTFunction = this.createScoreVSPTFunction();
    dataCaptureTable.grantWriteData(scoreVSPTFunction);

    const websiteBucket = this.createStaticBucket(props);

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
        managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess"), iam.ManagedPolicy.fromAwsManagedPolicyName("AWSLambdaExecute")] // for production make this more granular.
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

    const assets = api.root.addResource("assets");
    assets.addResource("{path+}").addMethod("GET", assetsIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})

    const content = api.root.addResource("content");
    content.addResource("{path+}").addMethod("GET", contentIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})
  }

  createDataCaptureTable() {

    return new dynamodb.TableV2(this, 'DialangDataCaptureTable', {
      tableName: 'dialang-data-capture',
      partitionKey: { name: "session_id", type: dynamodb.AttributeType.STRING },
    });
  }

  createSetTLFunction() {

    return new lambda.Function(this, 'SetTL', {
      code: lambda.Code.fromAsset('lambdas/settl'),
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'index.handler'
    });
  }

  createScoreVSPTFunction() {

    return new lambda.Function(this, 'ScoreVSPT', {
      code: lambda.Code.fromAsset('lambdas/scorevspt'),
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'index.handler'
    });
  }

  createStaticBucket(props) {

    const websiteBucket = new s3.Bucket(this, "WebsiteBucket");

    // Deploy site contents to S3 bucket
    new s3deploy.BucketDeployment(this, "DeployWebsite", {
      sources: [s3deploy.Source.asset("static-site")],
      destinationBucket: websiteBucket,
      destinationKeyPrefix: props.staticContentPrefix, // optional prefix in destination bucket
    });

    return websiteBucket;
  }
}

module.exports = { DialangWebStack }
