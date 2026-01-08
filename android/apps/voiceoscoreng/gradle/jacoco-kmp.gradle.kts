/**
 * Jacoco coverage configuration for KMP libraries
 *
 * Generates HTML and XML coverage reports for all KMP libraries.
 * Reports are aggregated at the root build/reports/jacoco directory.
 */

// Apply Jacoco plugin to all KMP library projects
subprojects {
    if (project.path.startsWith(":libraries:core:")) {
        apply(plugin = "jacoco")

        configure<JacocoPluginExtension> {
            toolVersion = "0.8.11"
        }

        tasks.withType<Test> {
            configure<JacocoTaskExtension> {
                isEnabled = true
                destinationFile = layout.buildDirectory.file("jacoco/test.exec").get().asFile
                includes = listOf("com.augmentalis.voiceos.*")
                excludes = listOf(
                    "**/*Test*",
                    "**/*Mock*",
                    "**/*Fake*",
                    "**/BuildConfig*"
                )
            }
        }

        tasks.register<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.withType<Test>())
            group = "verification"
            description = "Generate Jacoco coverage reports for ${project.name}"

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)

                xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
                html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
            }

            sourceDirectories.setFrom(files("src/commonMain/kotlin"))
            classDirectories.setFrom(files(layout.buildDirectory.dir("classes/kotlin/jvm/main")))
            executionData.setFrom(files(layout.buildDirectory.file("jacoco/test.exec")))
        }
    }
}

// Root task to generate aggregated coverage report
tasks.register("jacocoAggregatedReport") {
    group = "verification"
    description = "Generate aggregated Jacoco coverage report for all KMP libraries"

    val libraryProjects = subprojects.filter { it.path.startsWith(":libraries:core:") }

    dependsOn(libraryProjects.map { "${it.path}:jacocoTestReport" })

    doLast {
        val reportDir = file("${rootProject.layout.buildDirectory.get()}/reports/jacoco/aggregated")
        reportDir.mkdirs()

        val summary = StringBuilder()
        summary.appendLine("# Jacoco Coverage Report - VoiceOS KMP Libraries")
        summary.appendLine()
        summary.appendLine("Generated: ${java.time.LocalDateTime.now()}")
        summary.appendLine()
        summary.appendLine("## Library Coverage")
        summary.appendLine()
        summary.appendLine("| Library | Report |")
        summary.appendLine("|---------|--------|")

        libraryProjects.forEach { project ->
            val htmlReport = file("${project.layout.buildDirectory.get()}/reports/jacoco/test/html/index.html")
            if (htmlReport.exists()) {
                val relativePath = htmlReport.relativeTo(rootProject.projectDir).path
                summary.appendLine("| ${project.name} | [View Report]($relativePath) |")
            }
        }

        summary.appendLine()
        summary.appendLine("## Instructions")
        summary.appendLine()
        summary.appendLine("1. Run tests: `./gradlew test`")
        summary.appendLine("2. Generate coverage: `./gradlew jacocoAggregatedReport`")
        summary.appendLine("3. View reports in `build/reports/jacoco/` for each library")
        summary.appendLine()

        val summaryFile = File(reportDir, "README.md")
        summaryFile.writeText(summary.toString())

        println()
        println("========================================")
        println("   Jacoco Coverage Report Generated")
        println("========================================")
        println()
        println("Summary: ${summaryFile.absolutePath}")
        println()
        libraryProjects.forEach { project ->
            val htmlReport = file("${project.layout.buildDirectory.get()}/reports/jacoco/test/html/index.html")
            if (htmlReport.exists()) {
                println("${project.name}: ${htmlReport.absolutePath}")
            }
        }
        println()
    }
}
