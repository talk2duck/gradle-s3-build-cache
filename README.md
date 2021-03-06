# Gradle S3 Build Cache

[![Build](https://github.com/talk2duck/gradle-s3-build-cache/actions/workflows/build.yaml/badge.svg)](https://github.com/talk2duck/gradle-s3-build-cache/actions/workflows/build.yaml)
<a href="LICENSE"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="http://kotlinlang.org"><img alt="kotlin version" src="https://img.shields.io/badge/kotlin-1.4-blue.svg"></a>
<a href="https://codebeat.co/projects/github-com-talk2duck-gradle-s3-build-cache-main"><img alt="codebeat badge" src="https://codebeat.co/badges/6cf2556d-0523-4d92-9878-bdbecc3b6bc4" /></a>

Makes use of the [Gradle build cache](https://docs.gradle.org/current/userguide/build_cache.html) and stores build artifacts 
in AWS S3 bucket.


## Usage

*NOTE: Project is in early stages so use at your own risk.*

### Apply plugin

settings.gradle

```
buildscript {
  repositories {
    maven {
      url "https://dl.bintray.com/talk2duck/maven"
    }
  }
  dependencies {
    classpath "com.talk2duck:gradle-build-cache-plugin:1.0.0.11"
  }
}

apply plugin: 'com.talk2duck.gradle-s3-build-cache'

ext.isCiServer = System.getenv().containsKey("CI")
 
buildCache {
  local {
    enabled = !isCiServer
  }
  remote(com.talk2duck.gradle.buildcache.S3BuildCache) {
    region = '<your region>'
    bucket = '<your bucket name>'
    push = isCiServer
  }
}
```

### Configuration

| Configuration Key        | Type    | Description                                                                                                | Mandatory | Default Value |
| ------------------------ | ------- | ---------------------------------------------------------------------------------------------------------- | --------- | ------------- |
| awsAccessKeyId           | String  | The AWS access key id                                                                                      |           | [Using the Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default) |
| awsSecretKey             | String  | The AWS secret access key                                                                                  |           | [Using the Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default) |
| awsProfile               | String  | The AWS profile to source credentials from                                                                 |           | [Using the Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default) |
| sessionToken             | String  | The AWS sessionToken                                                                                       |           | [Using the Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default) |
| region                   | String  | The AWS region                                                                                             | yes       |               |
| bucket                   | String  | The name of the AWS S3 bucket where cache objects should be stored.                                        | yes       |               |
| prefix                   | String  | The prefix of the AWS S3 object key in the bucket                                                          |           | `cache/`      |
| endpoint                 | String  | The S3 endpoint                                                                                            |           |               |
| reducedRedundancyStorage | boolean | Whether to use [Reduced Redundancy Storage](https://aws.amazon.com/s3/reduced-redundancy/?nc1=h_ls) or not |           | `true`        |

### AWS credentials

By default, this plugin uses `Default Credential Provider Chain` to lookup the AWS credentials.  
See [Using the Default Credential Provider Chain - AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default) for more details.


You can also select which `profile` to use for AWS authentication by specifying `awsProfile`.  
See [Configuring the AWS CLI - Named profiles](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-profiles.html) for more details. 

If you want to set `access key id` and `secret access key` manually,
configure `awsAccessKeyId` and `awsSecretKey` (and `sessionToken` optionally).


### S3 Bucket Policy

The AWS credential must have at least the following permissions to the bucket:

```json
{
  "Version": "2020-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket"
      ],
      "Resource": [
          "arn:aws:s3:::your-bucket/*",
          "arn:aws:s3:::your-bucket"
      ]
    }
  ]
}
```

### Run build with build cache

The Gradle build cache is an incubating feature and needs to be enabled per build (`--build-cache`) or in the Gradle properties (`org.gradle.caching=true`).
See [the official doc](https://docs.gradle.org/current/userguide/build_cache.html#sec:build_cache_enable) for details.

# Versioning

This project uses explicit versioning *Release.Breaking.Feature.Fix* as described [here](https://medium.com/sapioit/why-having-3-numbers-in-the-version-name-is-bad-92fc1f6bc73c).

![Explicit Versioning](/explicit-versioning.png)


# License

[Apache License 2.0](./LICENSE)
