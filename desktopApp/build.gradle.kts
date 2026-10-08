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
}

compose.desktop {
    application {
        mainClass = "cz.pavlik.timetracker.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)

            packageName = "Hodinator"
            packageVersion = "1.0.0"
            description = "Jednoduché měření času stráveného na projektech"

            // Balený runtime obsahuje jen vyjmenované moduly JDK; JDBC (SQLite) potřebuje java.sql.
            modules("java.sql")

            macOS {
                bundleID = "cz.pavlik.timetracker"
            }
        }
    }
}
