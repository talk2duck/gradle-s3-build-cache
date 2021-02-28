package com.talk2duck.gradle.cache.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.talk2duck.gradle.cache.s3.S3BuildCacheServiceFactory.Companion.createAmazonS3Client
import io.findify.s3mock.S3Mock
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.util.UUID.nameUUIDFromBytes


class S3BuildCacheServiceTest {

    @Test
    fun `should store and load data from s3 cache`() {
        val data = "some data"
        val cacheKey = TestBuildCacheKey(data)
        val cacheService = S3BuildCacheService(amazonS3, bucketName, prefix, true)

        cacheService.store(cacheKey, cacheEntryWriterFor(data))
        assertThat(readCacheDataFromS3(cacheKey), equalTo(data))

        var dataFromCache  = ByteArray(0)
        cacheService.load(cacheKey) { dataFromCache = it.readAllBytes() }
        assertThat(String(dataFromCache), equalTo(data))
    }

    private val bucketName = "some-build-cache"
    private val prefix = "cache/"

    private val amazonS3 = createAmazonS3Client(S3BuildCache().apply {
        awsAccessKeyId = "someKey"
        awsSecretKey = "someSecret"
        endpoint = localS3Url
        region = ""
    }).apply {
        createBucket(bucketName)
    }

    private fun readCacheDataFromS3(cacheKey: TestBuildCacheKey) = String(amazonS3.getObject(bucketName, prefix + cacheKey.hashCode).objectContent.readAllBytes())

    @Suppress("UnstableApiUsage")
    data class TestBuildCacheKey(private val data: String) : BuildCacheKey {
        override fun getDisplayName() = "${javaClass.name}:${hashCode}"
        override fun getHashCode() = nameUUIDFromBytes(toByteArray()).toString()
        override fun toByteArray() = data.toByteArray()
    }

    private fun cacheEntryWriterFor(data: String) = object: BuildCacheEntryWriter {
        override fun writeTo(outputStream: OutputStream) = outputStream.write(data.toByteArray())
        override fun getSize() = data.toByteArray().size.toLong()
    }

    companion object {
        lateinit var localS3Url: String
        private val localS3 = S3Mock.Builder().withPort(0).withInMemoryBackend().build()


        @BeforeAll
        @JvmStatic
        fun setupMockS3() {
            localS3Url = localS3.start().localAddress().let { "http://localhost:${it.port}" }
        }

        @AfterAll
        @JvmStatic
        fun teardownMockS3() {
            localS3.stop()
        }
    }
}
