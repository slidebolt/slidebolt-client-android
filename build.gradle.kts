plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("check") {
    dependsOn(
        ":core-model:test",
        ":core-client:test",
        ":core-session:test",
        ":platform-android:testDebugUnitTest",
        ":feature-browser:testDebugUnitTest",
    )
}

tasks.register("bddTest") {
    dependsOn(":acceptance:bddTest")
}

