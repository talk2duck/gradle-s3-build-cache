package com.talk2duck.gradle.cache.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.StorageClass.ReducedRedundancy
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

private const val TEN_MB = 10000000

open class S3BuildCacheService(
    private val amazonS3: AmazonS3,
    private val bucketName: String,
    private val prefix: String,
    private val reducedRedundancyStorage: Boolean
) : BuildCacheService {
    override fun load(buildCacheKey: BuildCacheKey, buildCacheEntryReader: BuildCacheEntryReader): Boolean {
        val key: String = createS3Key(prefix, buildCacheKey.hashCode)
        try {
            amazonS3.getObject(bucketName, key).use { s3Object ->
                s3Object.objectContent.use { inputStream ->
                    buildCacheEntryReader.readFrom(inputStream)
                    return true
                }
            }
        } catch (e: AmazonServiceException) {
            if (e.errorCode == "NoSuchKey") {
                return false
            } else {
                throw BuildCacheException("Error while reading cache object from S3 bucket", e)
            }
        } catch (e: IOException) {
            throw BuildCacheException("Error while reading cache object from S3 bucket", e)
        }
    }

    override fun store(buildCacheKey: BuildCacheKey, buildCacheEntryWriter: BuildCacheEntryWriter) {
        val key = createS3Key(prefix, buildCacheKey.hashCode)
        try {
            if (buildCacheEntryWriter.size < TEN_MB) {
                ByteArrayOutputStream().use { outputStream ->
                    buildCacheEntryWriter.writeTo(outputStream)
                    val bytes = outputStream.toByteArray()
                    ByteArrayInputStream(bytes).use { inputStream ->
                        putObject(key, inputStream, bytes.size.toLong())
                    }
                }
            } else {
                // Use a temporary file to transfer the object
                val file = File.createTempFile("talk2duck-gradle-s3-build-cache", ".tmp")
                try {
                    FileOutputStream(file).use { os ->
                        buildCacheEntryWriter.writeTo(os)
                        FileInputStream(file).use { inputStream -> putObject(key, inputStream, file.length()) }
                    }
                } finally {
                    file.delete()
                }
            }
        } catch (e: IOException) {
            throw BuildCacheException("Error while storing cache object in S3 bucket", e)
        }
    }

    private fun putObject(key: String, inputStream: InputStream, size: Long) {
        val meta = ObjectMetadata().apply {
            contentType = BUILD_CACHE_CONTENT_TYPE
            contentLength = size
        }

        val request = PutObjectRequest(bucketName, key, inputStream, meta).apply {
            if (reducedRedundancyStorage) {
                setStorageClass(ReducedRedundancy)
            }
        }

        amazonS3.putObject(request)
    }

    private fun createS3Key(prefix: String, buildCacheHashCode: String): String {
        return when {
            prefix.isEmpty() -> buildCacheHashCode
            else -> {
                StringBuilder().apply {
                    append(prefix)
                    if (!prefix.endsWith("/")) append("/")
                    append(buildCacheHashCode)
                }.toString()
            }
        }
    }

    override fun close() {
        amazonS3.shutdown()
    }
}
