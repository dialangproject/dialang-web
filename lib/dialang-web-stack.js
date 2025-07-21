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

    const setALFunction = this.createSetALFunction();

    const sessionTable = this.createSessionTable();

    sessionTable.grantWriteData(setALFunction);

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
        managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess")] // for production make this more granular.
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
    api.root.addResource("setal").addMethod("GET", new apigw.LambdaIntegration(setALFunction), methodOptions);
    //const setALFunctionIntegration = new apigw.LambdaIntegration(setALFunction);

    const assets = api.root.addResource("assets");
    assets.addResource("{path+}").addMethod("GET", assetsIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})

    const content = api.root.addResource("content");
    content.addResource("{path+}").addMethod("GET", contentIntegration, {...methodOptions, requestParameters: {"method.request.path.path": true}})
  }

  createSessionTable() {

    return new dynamodb.TableV2(this, 'DialangSessionTable', {
      tableName: 'dialang-sessions',
      partitionKey: { name: 'session-id', type: dynamodb.AttributeType.STRING },
    });
  }

  createSetALFunction() {

    return new lambda.Function(this, 'SetAL', {
      code: lambda.Code.fromAsset('lambdas/setal'),
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'index.handler'
    });
  }

  createStaticBucket(props) {

    const indexDocument = "index.html";

    const websiteBucket = new s3.Bucket(this, "WebsiteBucket");

    /*
    const websiteBucket = new s3.Bucket(this, "WebsiteBucket", {
      websiteIndexDocument: indexDocument,
      publicReadAccess: true,
      blockPublicAccess: new s3.BlockPublicAccess({
        blockPublicAcls: false,
        blockPublicPolicy: false,
        ignorePublicAcls: false,
        restrictPublicBuckets: false,
      }),
      removalPolicy: RemovalPolicy.DESTROY, // NOT recommended for production code

      autoDeleteObjects: true, // NOT recommended for production code
    });
    */

    // Deploy site contents to S3 bucket
    new s3deploy.BucketDeployment(this, "DeployWebsite", {
      sources: [s3deploy.Source.asset("static-site")],
      destinationBucket: websiteBucket,
      destinationKeyPrefix: props.staticContentPrefix, // optional prefix in destination bucket
    });

    /*
    websiteBucket.addToResourcePolicy(
      new iam.PolicyStatement({
        principals: [ new iam.AnyPrincipal() ],
        effect: iam.Effect.ALLOW,
        actions: ["s3:GetObject"],
        resources: [websiteBucket.bucketArn, websiteBucket.arnForObjects('*'), websiteBucket.arnForObjects('generated/*')],
      })
    );

    new CfnOutput(this, "Bucket", { value: websiteBucket.bucketName });
    new CfnOutput(this, "StaticSiteUrl", {
      value: [
        websiteBucket.bucketWebsiteUrl,
        props.staticContentPrefix,
        indexDocument,
      ].join("/"),
    });
    */

    return websiteBucket;
  }
}

module.exports = { DialangWebStack }
