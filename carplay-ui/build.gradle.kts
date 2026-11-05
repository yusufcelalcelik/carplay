plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
val localAuthAssets = providers.environmentVariable("FIORINO_CARPLAY_AUTH_ASSETS").orNull?.let { file(it) }
android {
    namespace = "com.yusufcelik.carplayui"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    localAuthAssets?.let { sourceSets.getByName("main").assets.srcDir(it) }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(project(":carplay-engine"))
    testImplementation(project(":carplay-engine"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}
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
