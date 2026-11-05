plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.shilapi.xcertplay.shared"
    compileSdk = 36
    ndkVersion = "28.2.13676358"
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        externalNativeBuild { ndkBuild { arguments += "APP_PLATFORM=android-24" } }
    }
    externalNativeBuild { ndkBuild { path = file("src/main/jni/Android.mk") } }
    buildFeatures { aidl = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation("androidx.car.app:app:1.4.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.jmdns:jmdns:3.6.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}
// Android's test compile bootclasspath contains java.base, but the upstream network tests
// additionally use the local JDK HTTP server. Expose that API only to the host test compiler.
val hostHttpApi by tasks.registering(Jar::class) {
    archiveFileName.set("host-httpserver-api.jar")
    destinationDirectory.set(layout.buildDirectory.dir("host-test-api"))
    from(zipTree(file("${System.getProperty("java.home")}/jmods/jdk.httpserver.jmod"))) {
        include("classes/**")
        eachFile { path = path.removePrefix("classes/") }
        includeEmptyDirs = false
    }
}
dependencies { testCompileOnly(files(hostHttpApi)) }
tasks.withType<Test>().configureEach { jvmArgs("--add-modules=jdk.httpserver") }
