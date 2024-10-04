package com.talk2duck.gradle.buildcache

import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheException
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import org.gradle.internal.impldep.com.amazonaws.AmazonServiceException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.StorageClass.REDUCED_REDUNDANCY
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

private const val TEN_MB = 10000000

open class S3BuildCacheService(
    private val s3Client: S3Client,
    private val bucketName: String,
    private val prefix: String,
    private val reducedRedundancyStorage: Boolean
) : BuildCacheService {
    override fun load(buildCacheKey: BuildCacheKey, buildCacheEntryReader: BuildCacheEntryReader): Boolean {
        try {
            val request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(buildCacheKey.asS3Key())
                .build()

            s3Client.getObject(request).use { s3Object ->
                s3Object.use { inputStream ->
                    buildCacheEntryReader.readFrom(inputStream)
                    return true
                }
            }
        } catch (e: NoSuchKeyException) {
            return false
        } catch (e: AmazonServiceException) {
            throw BuildCacheException("Error while reading cache object from S3 bucket", e)
        } catch (e: IOException) {
            throw BuildCacheException("Error while reading cache object from S3 bucket", e)
        }
    }

    override fun store(buildCacheKey: BuildCacheKey, buildCacheEntryWriter: BuildCacheEntryWriter) {
        val key = buildCacheKey.asS3Key()
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
        val request =
            software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(BUILD_CACHE_CONTENT_TYPE)
                .contentLength(size)
                .let {
                    when (reducedRedundancyStorage) {
                        true -> it.storageClass(REDUCED_REDUNDANCY)
                        false -> it
                    }
                }.build()

        println("request = ${request}")

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size))
        } catch (e: NoSuchBucketException) {
            e.cause?.printStackTrace()
            e.printStackTrace()
        }
    }

    private fun BuildCacheKey.asS3Key(): String {
        return when {
            prefix.isEmpty() -> hashCode
            else -> {
                StringBuilder().apply {
                    append(prefix)
                    if (!prefix.endsWith("/")) append("/")
                    append(this@asS3Key.hashCode)
                }.toString()
            }
        }
    }

    override fun close() {
        s3Client.close()
    }
}
