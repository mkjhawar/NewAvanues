/**
 * testing-rules.gradle.kts - Mandatory testing rules for VOS4
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Enforces test coverage requirements and quality standards
 */

import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

// Apply to all projects
allprojects {
    // Configure test coverage thresholds
    extra["minTestCoverage"] = 85.0
    extra["minBranchCoverage"] = 80.0
    extra["minMutationScore"] = 75.0
}

// Apply to subprojects with source code
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("kotlin-android") || plugins.hasPlugin("kotlin")) {
            
            // Configure JaCoCo for coverage
            apply(plugin = "jacoco")
            
            configure<JacocoPluginExtension> {
                toolVersion = "0.8.11"
            }
            
            // Configure test tasks
            tasks.withType<Test> {
                useJUnitPlatform()
                
                // Set test execution requirements
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                
                // Configure test reporting
                reports {
                    html.required.set(true)
                    junitXml.required.set(true)
                }
                
                // Add test listeners
                testLogging {
                    events("passed", "failed", "skipped")
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showStandardStreams = false
                }
                
                // Fail fast on test failures
                failFast = false
                
                // Add system properties for testing
                systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
            }
            
            // Create coverage verification task
            tasks.register<JacocoCoverageVerification>("verifyCoverage") {
                dependsOn("test")
                
                violationRules {
                    rule {
                        element = "CLASS"
                        
                        limit {
                            counter = "LINE"
                            value = "COVEREDRATIO"
                            minimum = (extra["minTestCoverage"] as Double / 100).toBigDecimal()
                        }
                        
                        limit {
                            counter = "BRANCH"
                            value = "COVEREDRATIO"
                            minimum = (extra["minBranchCoverage"] as Double / 100).toBigDecimal()
                        }
                    }
                    
                    // Exclude generated files and test utilities
                    rule {
                        element = "CLASS"
                        excludes = listOf(
                            "**/R.class",
                            "**/R$*.class",
                            "**/BuildConfig.*",
                            "**/Manifest*.*",
                            "**/*Test*.*",
                            "**/*Mock*.*",
                            "**/test/**",
                            "**/androidTest/**"
                        )
                    }
                }
            }
            
            // Create test quality validation task
            tasks.register("validateTestQuality") {
                group = "verification"
                description = "Validates test quality metrics"
                
                doLast {
                    val testDir = file("src/test/java")
                    val androidTestDir = file("src/androidTest/java")
                    
                    var totalTests = 0
                    var totalAssertions = 0
                    var filesWithoutTests = mutableListOf<String>()
                    
                    // Check main source files have corresponding tests
                    fileTree("src/main/java") {
                        include("**/*.kt")
                        exclude("**/models/**", "**/data/**")
                    }.forEach { sourceFile ->
                        val testFile = findTestFile(sourceFile)
                        if (testFile == null || !testFile.exists()) {
                            filesWithoutTests.add(sourceFile.relativeTo(projectDir).path)
                        }
                    }
                    
                    // Count tests and assertions
                    listOf(testDir, androidTestDir).forEach { dir ->
                        if (dir.exists()) {
                            fileTree(dir) {
                                include("**/*Test.kt", "**/*Spec.kt")
                            }.forEach { testFile ->
                                val content = testFile.readText()
                                totalTests += content.count { content.contains("@Test") }
                                totalAssertions += content.count { 
                                    content.contains("assert") || 
                                    content.contains("verify") ||
                                    content.contains("should")
                                }
                            }
                        }
                    }
                    
                    // Validate metrics
                    if (filesWithoutTests.isNotEmpty()) {
                        logger.warn("⚠️  Files without tests:")
                        filesWithoutTests.forEach { logger.warn("   - $it") }
                    }
                    
                    if (totalTests == 0) {
                        throw GradleException("❌ No tests found in module: ${project.name}")
                    }
                    
                    val assertionRatio = totalAssertions.toDouble() / totalTests
                    if (assertionRatio < 1.0) {
                        logger.warn("⚠️  Low assertion ratio: ${String.format("%.2f", assertionRatio)} assertions per test")
                    }
                    
                    logger.lifecycle("✅ Test quality validation passed:")
                    logger.lifecycle("   Tests: $totalTests")
                    logger.lifecycle("   Assertions: $totalAssertions")
                    logger.lifecycle("   Ratio: ${String.format("%.2f", assertionRatio)}")
                }
            }
            
            // Add mutation testing if configured
            if (plugins.hasPlugin("info.solidsoft.pitest")) {
                configurations.create("pitestAgent")
                // Configure PiTest if plugin is available
                // Note: PiTest configuration removed due to compilation issues
                // Re-enable when PiTest plugin dependency is properly configured
            }
            
            // Create comprehensive test task
            tasks.register("testComprehensive") {
                group = "verification"
                description = "Runs all test validations"
                
                dependsOn("test")
                dependsOn("verifyCoverage")
                dependsOn("validateTestQuality")
                
                if (plugins.hasPlugin("info.solidsoft.pitest")) {
                    dependsOn("pitest")
                }
            }
            
            // Hook into build process
            tasks.named("check") {
                dependsOn("testComprehensive")
            }
        }
    }
}

// Helper function to find test file
fun findTestFile(sourceFile: File): File? {
    val baseName = sourceFile.nameWithoutExtension
    val relativePath = sourceFile.parentFile.path
        .replace("src/main/java", "src/test/java")
    
    val testFile = File(relativePath, "${baseName}Test.kt")
    val specFile = File(relativePath, "${baseName}Spec.kt")
    val androidTestFile = File(
        relativePath.replace("src/test/java", "src/androidTest/java"),
        "${baseName}Test.kt"
    )
    
    return when {
        testFile.exists() -> testFile
        specFile.exists() -> specFile
        androidTestFile.exists() -> androidTestFile
        else -> null
    }
}

// Task to generate test report
tasks.register("testReport") {
    group = "reporting"
    description = "Generates comprehensive test report"
    
    doLast {
        val reportFile = file("${layout.buildDirectory.get().asFile}/reports/test-summary.md")
        reportFile.parentFile.mkdirs()
        
        val report = buildString {
            appendLine("# VOS4 Test Report")
            appendLine("Generated: ${java.time.LocalDateTime.now()}")
            appendLine()
            
            subprojects.forEach { project ->
                if (project.tasks.findByName("test") != null) {
                    appendLine("## ${project.name}")
                    
                    val testResults = project.file("${project.layout.buildDirectory.get().asFile}/test-results/test")
                    if (testResults.exists()) {
                        val xmlFiles = testResults.listFiles { _, name -> name.endsWith(".xml") }
                        var totalTests = 0
                        var totalPassed = 0
                        var totalFailed = 0
                        var totalSkipped = 0
                        
                        xmlFiles?.forEach { xmlFile ->
                            try {
                                val xmlContent = xmlFile.readText()
                                val testsMatch = """tests="(\d+)""""".toRegex().find(xmlContent)
                                val failuresMatch = """failures="(\d+)""""".toRegex().find(xmlContent)
                                val skippedMatch = """skipped="(\d+)""""".toRegex().find(xmlContent)
                                
                                totalTests += testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                totalFailed += failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                totalSkipped += skippedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                            } catch (e: Exception) {
                                logger.warn("Failed to parse XML file: ${xmlFile.name} - ${e.message}")
                            }
                        }
                        totalPassed = totalTests - totalFailed - totalSkipped
                        
                        appendLine("- Total Tests: $totalTests")
                        appendLine("- Passed: $totalPassed ✅")
                        appendLine("- Failed: $totalFailed ❌")
                        appendLine("- Skipped: $totalSkipped ⏭️")
                        appendLine("- Success Rate: ${if (totalTests > 0) "%.1f%%".format((totalPassed.toDouble() / totalTests) * 100) else "N/A"}")
                    } else {
                        appendLine("- No test results found")
                    }
                    appendLine()
                }
            }
        }
        
        reportFile.writeText(report)
        logger.lifecycle("📊 Test report generated: ${reportFile.absolutePath}")
    }
}