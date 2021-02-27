package com.talk2duck.gradle.cache.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.StorageClass
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheException
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

fun createS3Key(prefix: String?, buildCacheHashCode: String): String {
    if (prefix == null || prefix.isEmpty()) {
        return buildCacheHashCode
    }
    val sb = StringBuilder()
    sb.append(prefix)
    if (!prefix.endsWith("/")) {
        sb.append("/")
    }
    sb.append(buildCacheHashCode)
    return sb.toString()
}

open class S3BuildCacheService(
    private val amazonS3: AmazonS3? = null,
    private val bucketName: String? = null,
    private val prefix: String? = null,
    private val reducedRedundancyStorage: Boolean = false
) : BuildCacheService {
    @kotlin.jvm.Throws(BuildCacheException::class)
    override fun load(buildCacheKey: BuildCacheKey, buildCacheEntryReader: BuildCacheEntryReader): Boolean {
        val key: String = createS3Key(prefix, buildCacheKey.hashCode)
        if (!amazonS3!!.doesObjectExist(bucketName, key)) {
//            S3BuildCacheService.log.info("Build cache not found. key='{}'", key)
            return false
        }
        try {
            amazonS3.getObject(bucketName, key).use { `object` ->
                `object`.objectContent.use { `is` ->
                    buildCacheEntryReader.readFrom(`is`)
//                    S3BuildCacheService.log.info("Build cache found. key='{}'", key)
                    return true
                }
            }
        } catch (e: IOException) {
            throw BuildCacheException("Error while reading cache object from S3 bucket", e)
        }
    }


    override fun store(buildCacheKey: BuildCacheKey, buildCacheEntryWriter: BuildCacheEntryWriter) {
        val key: String = createS3Key(prefix, buildCacheKey.hashCode)
//        S3BuildCacheService.log.info("Start storing cache entry. key='{}'", key)
        try {
            if (buildCacheEntryWriter.size < 10000000 /* 10MB */) {
                ByteArrayOutputStream().use { os ->
                    buildCacheEntryWriter.writeTo(os)
                    val bytes = os.toByteArray()
                    ByteArrayInputStream(bytes).use { `is` -> putObject(key, `is`, bytes.size.toLong()) }
                }
            } else {
                // Use a temporary file to transfer the object
                val file = File.createTempFile("s3-gradle-build-cache-plugin", ".tmp")
                try {
                    FileOutputStream(file).use { os ->
                        buildCacheEntryWriter.writeTo(os)
                        FileInputStream(file).use { `is` -> putObject(key, `is`, file.length()) }
                    }
                } finally {
                    file.delete()
                }
            }
        } catch (e: IOException) {
            throw BuildCacheException("Error while storing cache object in S3 bucket", e)
        }
    }

    private fun putObject(key: String, `is`: InputStream, size: Long) {
        val meta = ObjectMetadata()
        meta.contentType = BUILD_CACHE_CONTENT_TYPE
        meta.contentLength = size
        val request = PutObjectRequest(bucketName, key, `is`, meta)
        if (reducedRedundancyStorage) {
            request.setStorageClass(StorageClass.ReducedRedundancy)
        }
        amazonS3!!.putObject(request)
    }

    override fun close() {
        amazonS3!!.shutdown()
    }
}
