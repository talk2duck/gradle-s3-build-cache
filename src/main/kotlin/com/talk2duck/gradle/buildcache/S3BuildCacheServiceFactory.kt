package com.talk2duck.gradle.buildcache

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory
import org.gradle.caching.BuildCacheServiceFactory.Describer

const val BUILD_CACHE_CONTENT_TYPE = "application/vnd.gradle.build-cache-artifact"

open class S3BuildCacheServiceFactory : BuildCacheServiceFactory<S3BuildCache> {
    override fun createBuildCacheService(configuration: S3BuildCache, describer: Describer): BuildCacheService {
        describer
            .type("S3")
            .config("region", configuration.region)
            .config("bucket", configuration.bucket)
            .config("reducedRedundancyStorage", java.lang.String.valueOf(configuration.reducedRedundancyStorage))

        if (configuration.prefix.isNotEmpty()) {
            describer.config("prefix", configuration.prefix)
        }

        if (configuration.endpoint.isNotEmpty()) {
            describer.config("endpoint", configuration.endpoint)
        }
        return S3BuildCacheService(
            createAmazonS3Client(configuration),
            configuration.bucket,
            configuration.prefix,
            configuration.reducedRedundancyStorage
        )
    }

    companion object {
        fun createAmazonS3Client(configuration: S3BuildCache): AmazonS3 {
            val s3Builder = AmazonS3ClientBuilder.standard()

            if (configuration.awsAccessKeyId.isNotEmpty() || configuration.awsSecretKey.isNotEmpty()) {
                val credentials = if (configuration.sessionToken.isEmpty()) {
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

            if (configuration.region.isNotEmpty()) {
                s3Builder.region = configuration.region
            }

            if (configuration.endpoint.isNotEmpty()) {
                s3Builder
                    .withPathStyleAccessEnabled(true)
                    .setEndpointConfiguration(EndpointConfiguration(configuration.endpoint, configuration.region))
            }

            return s3Builder.build()
        }
    }
}
