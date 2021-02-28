package com.talk2duck.gradle.cache.s3

import org.gradle.caching.configuration.AbstractBuildCache

open class S3BuildCache : AbstractBuildCache() {
    var awsAccessKeyId: String = ""
    var awsSecretKey: String = ""
    var sessionToken: String = ""
    var region: String = "eu-west-2"
    var bucket: String = ""
    var prefix: String = "cache/"
    var endpoint: String = ""
    var reducedRedundancyStorage = true
}
