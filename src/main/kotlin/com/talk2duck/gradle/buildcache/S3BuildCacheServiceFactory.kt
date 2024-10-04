package com.talk2duck.gradle.buildcache

import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory
import org.gradle.caching.BuildCacheServiceFactory.Describer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

const val BUILD_CACHE_CONTENT_TYPE = "application/vnd.gradle.build-cache-artifact"

open class S3BuildCacheServiceFactory : BuildCacheServiceFactory<S3BuildCache> {
    override fun createBuildCacheService(configuration: S3BuildCache, describer: Describer): BuildCacheService {
        describer
            .type("S3")
            .config("region", configuration.region)
            .config("bucket", configuration.bucket)
            .config("reducedRedundancyStorage", java.lang.String.valueOf(configuration.reducedRedundancyStorage))

        if (configuration.prefix.isNotBlank()) {
            describer.config("prefix", configuration.prefix)
        }

        if (configuration.endpoint.isNotBlank()) {
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
        fun createAmazonS3Client(configuration: S3BuildCache): S3Client {
            val credentials = mutableListOf<AwsCredentialsProvider>()

            if (configuration.awsAccessKeyId.isNotBlank() || configuration.awsSecretKey.isNotBlank()) {
                if (configuration.sessionToken.isNotBlank()) {
                    credentials.add(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.builder()
                                .accessKeyId(configuration.awsAccessKeyId)
                                .secretAccessKey(configuration.awsSecretKey)
                                .build()
                        )
                    )
                } else {
                    credentials.add(
                        StaticCredentialsProvider.create(
                            AwsSessionCredentials.builder()
                                .accessKeyId(configuration.awsAccessKeyId)
                                .secretAccessKey(configuration.awsSecretKey)
                                .sessionToken(configuration.sessionToken)
                                .build()
                        )
                    )
                }
            } else if (configuration.awsProfile.isNotBlank()) {
                credentials.add(ProfileCredentialsProvider.builder().profileName(configuration.awsProfile).build())
            } else {
                credentials.add(DefaultCredentialsProvider.create())
            }

            return S3Client.builder()
                .apply {
                    serviceConfiguration(
                        S3Configuration.builder()
                            .chunkedEncodingEnabled(false)
                            .pathStyleAccessEnabled(false)
                            .build()
                    )

                    credentialsProvider(
                        AwsCredentialsProviderChain.builder()
                            .credentialsProviders(credentials)
                            .build()
                    )

                    if (configuration.region.isNotBlank()) {
                        region(Region.of(configuration.region))
                    }

                    if (configuration.endpoint.isNotBlank()) {
                        endpointOverride(URI.create(configuration.endpoint))
                    }
                }.build()
        }
    }
}
