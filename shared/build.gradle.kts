plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutinesCore)
            api(libs.kotlinx.datetime)
            api(libs.koin.core)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqlite.jdbc)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
