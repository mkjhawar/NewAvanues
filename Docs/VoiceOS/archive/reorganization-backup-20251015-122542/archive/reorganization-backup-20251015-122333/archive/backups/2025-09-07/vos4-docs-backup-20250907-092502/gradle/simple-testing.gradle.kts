/**
 * simple-testing.gradle.kts - Simplified testing configuration for VOS4
 * 
 * Basic testing setup without aggressive validation rules
 */

// Apply to subprojects with source code
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("kotlin-android") || plugins.hasPlugin("kotlin")) {
            
            // Configure test tasks with minimal setup
            tasks.withType<Test> {
                // Don't configure useJUnitPlatform for Android modules
                // as it conflicts with AndroidUnitTest task creation
                
                // Set test execution requirements
                maxParallelForks = 1 // Conservative setting
                
                // Add test listeners
                testLogging {
                    events("passed", "failed", "skipped")
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showStandardStreams = true
                }
                
                // Don't fail fast for now
                failFast = false
            }
        }
    }
}