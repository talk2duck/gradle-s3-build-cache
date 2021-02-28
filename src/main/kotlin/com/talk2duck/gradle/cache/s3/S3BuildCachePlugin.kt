package com.talk2duck.gradle.cache.s3

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class S3BuildCachePlugin : Plugin<Settings> {
    override fun apply(settings: Settings) = settings.applyS3BuildCachePlugin()
}

private fun Settings.applyS3BuildCachePlugin() {
    settings.buildCache.registerBuildCacheService(S3BuildCache::class.java, S3BuildCacheServiceFactory::class.java)
}
