plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.6.21"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}