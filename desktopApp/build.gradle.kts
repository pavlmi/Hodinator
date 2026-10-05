import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.datetime)
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
    implementation(libs.sqlite.jdbc)
    implementation(libs.koin.core)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "cz.pavlik.timetracker.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)

            packageName = "cz.pavlik.timetracker"
            packageVersion = "1.0.0"

            modules("java.sql")

            macOS {
                bundleID = "cz.pavlik.timetracker"
            }
        }
    }
}
