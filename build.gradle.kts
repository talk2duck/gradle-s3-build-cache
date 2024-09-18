import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
        classpath("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.30.0")
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.4.0")
        classpath("com.github.johnrengelman:shadow:8.1.1")
        classpath("com.gradle.publish:plugin-publish-plugin:1.1.0")
        classpath("com.github.breadmoirai:github-release:2.4.1")
    }
}

plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
    id("signing")
    id("io.codearte.nexus-staging") version "0.30.0"
    id("java-gradle-plugin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.github.breadmoirai.github-release") version "2.4.1"
}

fun String.exec(workingDir: File = projectDir) = try {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText().trim()
} catch (e: Exception) {
    e.printStackTrace()
    ""
}

group = "com.talk2duck"
version = "./scripts/get-version.sh".exec()

repositories {
    mavenCentral()
}

dependencies {
    shadow(localGroovy())
    shadow(gradleApi())

    implementation("com.amazonaws:aws-java-sdk-s3:1.12.772")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("io.findify:s3mock_2.12:0.2.6")
}

tasks {
    val signingKey = System.getenv("SIGNING_KEY") ?: "notset"
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: "notset"

    val nexusStagingUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    val nexusSnapshotsUrl = "https://s01.oss.sonatype.org/service/local/snapshots/deploy/maven2/"
    val nexusUsername = System.getenv("NEXUS_USERNAME") ?: "notset"
    val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: "notset"

    val githubToken = System.getenv("GITHUB_TOKEN") ?: "noset"
    val githubReleaseNotes = "./scripts/release-notes.sh ${project.version}".exec()

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(project.the<SourceSetContainer>()["main"].allSource)
        dependsOn(named("classes"))
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(named<Javadoc>("javadoc").get().destinationDir)
        dependsOn(named("javadoc"))
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            allWarningsAsErrors = true
            compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
            compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
        }
    }

    withType<JavaCompile> {
        targetCompatibility = "11"
    }

    shadowJar {
        archiveClassifier.set("")
        isEnableRelocation = true
        relocationPrefix = "com.talk2duck.gradle.buildcache.shadow"
        mergeServiceFiles()
        exclude("META-INF/versions/")
        exclude("META-INF/maven/")
        exclude("META-INF/*.kotlin_module")
    }

    jar {
        manifest {
            attributes["gradle_buildcache_version"] = archiveVersion
        }
    }

    test {
        useJUnitPlatform()
    }

    artifacts {
        archives(sourcesJar)
        archives(javadocJar)
    }

    gradlePlugin {
        plugins {
            create("s3BuildCache") {
                id = "com.talk2duck.gradle-s3-build-cache"
                implementationClass = "com.talk2duck.gradle.buildcache.S3BuildCachePlugin"
                displayName = "Gradle S3 Build Cache Plugin"
                description = "Gradle build cache plugin that uses AWS S3 to store build artifacts"
            }
        }
    }

    signing {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }

    publishing {
        repositories {
            maven {
                name = "SonatypeStaging"
                setUrl(nexusStagingUrl)
                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
            }

            maven {
                name = "SonatypeSnapshot"
                setUrl(nexusSnapshotsUrl)
                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.name

                pom {
                    name.set(project.name)
                    description.set("Gradle build cache plugin that uses AWS S3 to store build artifacts")
                    url.set("https://github.com/talk2duck/gradle-s3-build-cache")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            name.set("Talk2Duck")
                            email.set("support@talk2duck.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git@github.com:talk2duck/gradle-s3-build-cache.git")
                        developerConnection.set("scm:git:git@github.com:talk2duck/gradle-s3-build-cache.git")
                        url.set("git@github.com:talk2duck/gradle-s3-build-cache.git")
                    }
                }

                artifact(shadowJar)
                artifact(sourcesJar)
                artifact(javadocJar)
            }
        }
    }

    nexusStaging {
        serverUrl = "https://s01.oss.sonatype.org/service/local"
        username = nexusUsername
        password = nexusPassword
    }

    githubRelease {
        setToken { githubToken }
        owner.set("talk2duck")
        repo.set(project.name)
        tagName.set(project.version.toString())
        targetCommitish.set("main")
        releaseName.set(project.version.toString())
        body.set(githubReleaseNotes)
        draft.set(false)
        prerelease.set(false)
        overwrite.set(false)
        dryRun.set(false)
    }
}