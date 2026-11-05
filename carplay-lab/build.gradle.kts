import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val localAuthAssets = providers.environmentVariable("FIORINO_CARPLAY_AUTH_ASSETS").orNull?.let { file(it) }
android {
    namespace = "com.yusufcelik.carplaylab"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.yusufcelik.fiorino.carplaylab"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = if (localAuthAssets == null) "0.1.2-lab" else "0.1.2-local-auth-test"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    localAuthAssets?.let { sourceSets.getByName("main").assets.srcDir(it) }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    signingConfigs {
        create("release") {
            val keyFile = rootProject.file("key.properties")
            if (keyFile.isFile) {
                val keys = Properties().apply { keyFile.inputStream().use { load(it) } }
                storeFile = file(keys.getProperty("storeFile"))
                storePassword = keys.getProperty("storePassword")
                keyAlias = keys.getProperty("keyAlias")
                keyPassword = keys.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
dependencies {
    implementation(project(":carplay-engine"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}
// Runtime identity inputs are always external, explicit and local. Ordinary source builds omit them.
val verifyLocalAuth by tasks.registering {
    doLast {
        localAuthAssets?.let { directory ->
            val expected = setOf("offline-mfi/identity.pk8", "offline-mfi/certificate.p7b")
            check(expected.all { directory.resolve(it).let { f -> f.isFile && f.length() in 1..16384 } }) { "Incomplete local accessory identity input" }
            check(directory.walkTopDown().filter { it.isFile }.all { it.relativeTo(directory).invariantSeparatorsPath in expected }) { "Unexpected local authentication asset" }
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyLocalAuth) }

// The optional interoperability identity must never enter a public release by accident.
val requirePublicInputs by tasks.registering {
    doLast { check(localAuthAssets == null) { "Local-authentication builds are private test artifacts" } }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { mustRunAfter(requirePublicInputs) }
tasks.register("assemblePublicRelease") {
    group = "build"
    dependsOn(requirePublicInputs, "assembleRelease")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/carplay-lab-release.apk").get().asFile
        ZipFile(apk).use { zip ->
            check(zip.entries().asSequence().none { it.name.startsWith("assets/offline-mfi/") }) {
                "Accessory identity found in an APK intended for public distribution"
            }
        }
    }
}
