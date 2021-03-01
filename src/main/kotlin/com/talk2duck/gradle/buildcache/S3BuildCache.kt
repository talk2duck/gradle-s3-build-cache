package com.talk2duck.gradle.buildcache

import org.gradle.caching.configuration.AbstractBuildCache

open class S3BuildCache : AbstractBuildCache() {
    var awsProfile: String = ""
    var awsAccessKeyId: String = ""
    var awsSecretKey: String = ""
    var sessionToken: String = ""
    var region: String = ""
    var bucket: String = ""
    var prefix: String = "cache/"
    var endpoint: String = ""
    var reducedRedundancyStorage = true
}
