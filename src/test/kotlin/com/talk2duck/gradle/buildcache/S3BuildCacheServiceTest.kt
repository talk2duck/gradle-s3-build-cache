package com.talk2duck.gradle.buildcache

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.talk2duck.gradle.buildcache.S3BuildCacheServiceFactory.Companion.createAmazonS3Client
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.FakeS3
import org.http4k.connect.amazon.s3.createBucket
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.OutputStream
import java.util.UUID.nameUUIDFromBytes

class S3BuildCacheServiceTest {

    @Test
    fun `should store and load data from s3 cache`() {
        val cacheData = "some data"
        val cacheKey = TestBuildCacheKey(cacheData)
        cacheService.store(cacheKey, cacheEntryWriterFor(cacheData))
        assertThat(readCacheDataFromS3(cacheKey), equalTo(cacheData))

        var dataFromCache = ByteArray(0)
        val found = cacheService.load(cacheKey) { dataFromCache = it.readAllBytes() }
        assertThat(found, equalTo(true))
        assertThat(String(dataFromCache), equalTo(cacheData))
    }

    @Test
    fun `should not blow up when loading missing item`() {
        var dataFromCache = ByteArray(0)
        val found = cacheService.load(TestBuildCacheKey("some invalid cache key")) { dataFromCache = it.readAllBytes() }
        assertThat(found, equalTo(false))
        assertThat(dataFromCache.size, equalTo(0))
    }

    private val bucketName = "some-build-cache"
    private val prefix = "cache/"
    private val awsRegion = Region.AP_EAST_1
    private val amazonS3 = createAmazonS3Client(S3BuildCache().apply {
        awsAccessKeyId = "someKey"
        awsSecretKey = "someSecret"
        region = awsRegion.value
        endpoint = localS3Url
    }).apply {
        fakeS3.s3Client().createBucket(BucketName.of(bucketName), awsRegion)
    }

    private val cacheService = S3BuildCacheService(amazonS3, bucketName, prefix, true)

    private fun readCacheDataFromS3(cacheKey: TestBuildCacheKey) = String(amazonS3.getObject(GetObjectRequest.builder().bucket(bucketName).key(prefix + cacheKey.hashCode).build()).readAllBytes())

    data class TestBuildCacheKey(private val data: String) : BuildCacheKey {
        @Deprecated("Deprecated")
        override fun getDisplayName() = "${javaClass.name}:${hashCode}"
        override fun getHashCode() = nameUUIDFromBytes(toByteArray()).toString()
        override fun toByteArray() = data.toByteArray()
    }

    private fun cacheEntryWriterFor(data: String) = object : BuildCacheEntryWriter {
        override fun writeTo(outputStream: OutputStream) = outputStream.write(data.toByteArray())
        override fun getSize() = data.toByteArray().size.toLong()
    }

    companion object {
        val fakeS3 = FakeS3()
        lateinit var localS3Url: String
        lateinit var fakeS3Server: Http4kServer

        @BeforeAll
        @JvmStatic
        fun setupMockS3() {
            fakeS3Server = fakeS3.asServer(SunHttp(0)).start()
            localS3Url = fakeS3Server.let { "http://localhost:${it.port()}" }
        }

        @AfterAll
        @JvmStatic
        fun teardownMockS3() {
            fakeS3Server.stop()
        }
    }
}
