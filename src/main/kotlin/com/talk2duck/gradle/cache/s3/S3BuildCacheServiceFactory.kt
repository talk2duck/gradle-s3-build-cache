package com.talk2duck.gradle.cache.s3

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

const val BUILD_CACHE_CONTENT_TYPE = "application/vnd.gradle.build-cache-artifact"

open class S3BuildCacheServiceFactory : BuildCacheServiceFactory<S3BuildCache> {
    override fun createBuildCacheService(configuration: S3BuildCache, describer: BuildCacheServiceFactory.Describer): BuildCacheService {
        describer
            .type("S3")
            .config("region", configuration.region)
            .config("bucket", configuration.bucket)
            .config("reducedRedundancyStorage", java.lang.String.valueOf(configuration.reducedRedundancyStorage))
        if (!isEmpty(configuration.prefix)) {
            describer.config("prefix", configuration.prefix)
        }
        if (!isEmpty(configuration.endpoint)) {
            describer.config("endpoint", configuration.endpoint)
        }
        return S3BuildCacheService(
            createAmazonS3Client(configuration),
            configuration.bucket,
            configuration.prefix,
            configuration.reducedRedundancyStorage
        )
    }

    private fun createAmazonS3Client(configuration: S3BuildCache): AmazonS3 {
        val s3Builder = AmazonS3ClientBuilder.standard()
        if (!isEmpty(configuration.awsAccessKeyId) || !isEmpty(configuration.awsSecretKey)) {
            val credentials: AWSCredentials
            credentials = if (isEmpty(configuration.sessionToken)) {
                BasicAWSCredentials(
                    configuration.awsAccessKeyId,
                    configuration.awsSecretKey
                )
            } else {
                BasicSessionCredentials(
                    configuration.awsAccessKeyId,
                    configuration.awsSecretKey,
                    configuration.sessionToken
                )
            }
            s3Builder.credentials = AWSStaticCredentialsProvider(credentials)
        }
        if (!isEmpty(configuration.region)) {
            s3Builder.region = configuration.region
        }
        if (!isEmpty(configuration.endpoint)) {
            s3Builder.setEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(configuration.endpoint, configuration.region)
            )
        }
        return s3Builder.build()
    }

    private fun isEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }
}
