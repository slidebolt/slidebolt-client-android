plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.slidebolt.client.platform"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core-model"))
    api(project(":core-session"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

