plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.allopen") version "1.5.30"
    kotlin("kapt") version "1.5.30"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val arrowKtVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(platform("io.arrow-kt:arrow-stack:${arrowKtVersion}"))
    implementation("io.quarkus:quarkus-quartz")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.+")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    implementation("io.egm:kngsild:0.4.0-dev")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.29.1")
}

group = "io.egm"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}
