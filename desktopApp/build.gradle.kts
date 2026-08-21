import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion = project.findProperty("app.version")?.toString() ?: "1.0.0"
version = appVersion

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.vlcj)
    
    // Dependencias de registro para VLCJ
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "com.geraciodev.lumina.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "Lumina"
            packageVersion = appVersion
            description = "Lumina - Reproductor multimedia minimalista con soporte bíblico"
            copyright = "© 2024 GeracioDev"
            vendor = "GeracioDev"

            linux {
                packageName = "lumina"
                debMaintainer = "contacto@geraciodev.com"
                menuGroup = "Multimedia"
                appCategory = "Video"
                shortcut = true
            }
            windows {
                packageName = "Lumina"
                shortcut = true
                menu = true
                upgradeUuid = "6f5f9e20-7b2c-4e9b-9a8c-8f9d8a7b6c5d"
                dirChooser = true
                console = false
            }
            macOS {
                bundleID = "com.geraciodev.lumina"
                dockName = "Lumina"
            }
        }
    }
}

// Tarea para generar el paquete portable .tar.gz (Linux/Portable)
tasks.register<Tar>("packageTarGz") {
    group = "distribution"
    description = "Crea un paquete .tar.gz de la aplicación"
    dependsOn("createDistributable")
    
    val inputDir = layout.buildDirectory.dir("compose/binaries/main/app")
    from(inputDir)
    archiveFileName.set("Lumina-$appVersion.tar.gz")
    compression = Compression.GZIP
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.register("generateVersionProperties") {
    val resourcesDir = File(project.projectDir, "src/main/resources")
    val outputFile = File(resourcesDir, "version.properties")
    
    doLast {
        if (!resourcesDir.exists()) resourcesDir.mkdirs()
        outputFile.writeText("version=$appVersion")
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}
