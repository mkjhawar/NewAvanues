#!/usr/bin/env kotlin

/**
 * Import Cleanup Tool for VOS4 Project
 * Analyzes Kotlin files to identify and remove unused imports
 * Author: AGENT 3 - IMPORT CLEANUP SPECIALIST
 */

import java.io.File
import java.util.regex.Pattern

data class ImportAnalysis(
    val filePath: String,
    val allImports: List<String>,
    val usedImports: List<String>,
    val unusedImports: List<String>,
    val needsOrganization: Boolean
)

class ImportCleanupTool {
    private val projectRoot = "/Volumes/M Drive/Coding/Warp/vos4"
    
    fun analyzeAllFiles(): List<ImportAnalysis> {
        val kotlinFiles = findKotlinFiles()
        println("Found ${kotlinFiles.size} Kotlin files to analyze...")
        
        return kotlinFiles.mapNotNull { file ->
            try {
                analyzeFile(file)
            } catch (e: Exception) {
                println("Error analyzing ${file.absolutePath}: ${e.message}")
                null
            }
        }
    }
    
    private fun findKotlinFiles(): List<File> {
        return File(projectRoot).walkTopDown()
            .filter { it.extension == "kt" }
            .filter { !it.path.contains("/build/") }
            .filter { !it.path.contains("/whisper-source/") }
            .toList()
    }
    
    private fun analyzeFile(file: File): ImportAnalysis {
        val content = file.readText()
        val lines = content.lines()
        
        val imports = extractImports(lines)
        val usedImports = findUsedImports(content, imports)
        val unusedImports = imports - usedImports.toSet()
        val needsOrganization = needsImportOrganization(imports)
        
        return ImportAnalysis(
            filePath = file.absolutePath,
            allImports = imports,
            usedImports = usedImports,
            unusedImports = unusedImports,
            needsOrganization = needsOrganization
        )
    }
    
    private fun extractImports(lines: List<String>): List<String> {
        return lines
            .filter { it.trim().startsWith("import ") }
            .map { it.trim() }
            .filter { !it.startsWith("//") } // Skip commented imports
    }
    
    private fun findUsedImports(content: String, imports: List<String>): List<String> {
        val usedImports = mutableListOf<String>()
        
        for (import in imports) {
            val importStatement = import.substringAfter("import ").trim()
            
            // Handle star imports
            if (importStatement.endsWith(".*")) {
                val packageName = importStatement.substringBeforeLast(".*")
                if (isStarImportUsed(content, packageName)) {
                    usedImports.add(import)
                }
                continue
            }
            
            // Handle regular imports
            val className = when {
                importStatement.contains(" as ") -> {
                    // Handle aliased imports
                    importStatement.substringAfter(" as ").trim()
                }
                else -> {
                    importStatement.substringAfterLast(".")
                }
            }
            
            if (isClassUsed(content, className, import)) {
                usedImports.add(import)
            }
        }
        
        return usedImports
    }
    
    private fun isStarImportUsed(content: String, packageName: String): Boolean {
        // For star imports, we need more sophisticated analysis
        // For now, conservatively keep them unless obviously unused
        return true
    }
    
    private fun isClassUsed(content: String, className: String, fullImport: String): Boolean {
        // Remove the import line from content to avoid false positives
        val contentWithoutImports = content.lines()
            .filterNot { it.trim().startsWith("import ") }
            .joinToString("\n")
        
        // Check for direct class usage
        val patterns = listOf(
            "\\b$className\\b",           // Direct class reference
            "$className\\(",              // Constructor call
            "$className\\.",              // Static method/property access
            ": $className",               // Type declaration
            "<$className>",               // Generic type
            "($className)",               // Function parameter type
            "@$className",                // Annotation usage
            "is $className",              // Type check
            "as $className"               // Type cast
        )
        
        return patterns.any { pattern ->
            Pattern.compile(pattern).matcher(contentWithoutImports).find()
        }
    }
    
    private fun needsImportOrganization(imports: List<String>): Boolean {
        if (imports.size <= 1) return false
        
        // Check if imports are properly organized:
        // 1. Standard libraries first
        // 2. Third-party libraries
        // 3. Local project imports
        // 4. Alphabetically sorted within each group
        
        val androidImports = imports.filter { it.contains("android.") }
        val kotlinImports = imports.filter { it.contains("kotlin") || it.contains("kotlinx") }
        val thirdPartyImports = imports.filter { 
            !it.contains("android.") && 
            !it.contains("kotlin") && 
            !it.contains("com.augmentalis") &&
            !it.contains("androidx.")
        }
        val androidxImports = imports.filter { it.contains("androidx.") }
        val projectImports = imports.filter { it.contains("com.augmentalis") }
        
        // Check if groups are in correct order and alphabetically sorted
        val expectedOrder = listOf(
            androidImports.sorted(),
            androidxImports.sorted(),
            kotlinImports.sorted(),
            thirdPartyImports.sorted(),
            projectImports.sorted()
        ).flatten()
        
        return imports != expectedOrder
    }
    
    fun printAnalysisReport(analyses: List<ImportAnalysis>) {
        println("\n=== IMPORT CLEANUP ANALYSIS REPORT ===")
        
        var totalFiles = analyses.size
        var filesWithUnusedImports = analyses.count { it.unusedImports.isNotEmpty() }
        var filesNeedingOrganization = analyses.count { it.needsOrganization }
        var totalUnusedImports = analyses.sumOf { it.unusedImports.size }
        
        println("Total files analyzed: $totalFiles")
        println("Files with unused imports: $filesWithUnusedImports")
        println("Files needing organization: $filesNeedingOrganization")
        println("Total unused imports: $totalUnusedImports")
        
        println("\n=== FILES WITH UNUSED IMPORTS ===")
        analyses.filter { it.unusedImports.isNotEmpty() }
            .sortedByDescending { it.unusedImports.size }
            .take(20)
            .forEach { analysis ->
                println("\nFile: ${analysis.filePath.substringAfterLast("/")}")
                println("  Unused imports (${analysis.unusedImports.size}):")
                analysis.unusedImports.forEach { import ->
                    println("    $import")
                }
            }
    }
    
    fun cleanupFile(filePath: String, unusedImports: List<String>): Boolean {
        return try {
            val file = File(filePath)
            val content = file.readText()
            val lines = content.lines().toMutableList()
            
            // Remove unused imports
            val cleanedLines = lines.filterNot { line ->
                unusedImports.any { unused -> line.trim() == unused.trim() }
            }
            
            // Remove consecutive empty lines in import section
            val finalLines = removeExcessiveEmptyLines(cleanedLines)
            
            file.writeText(finalLines.joinToString("\n"))
            true
        } catch (e: Exception) {
            println("Error cleaning up $filePath: ${e.message}")
            false
        }
    }
    
    private fun removeExcessiveEmptyLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var consecutiveEmpty = 0
        var inImportSection = false
        
        for (line in lines) {
            when {
                line.trim().startsWith("import ") -> {
                    inImportSection = true
                    result.add(line)
                    consecutiveEmpty = 0
                }
                line.trim().isEmpty() -> {
                    consecutiveEmpty++
                    if (!inImportSection || consecutiveEmpty <= 1) {
                        result.add(line)
                    }
                }
                else -> {
                    if (inImportSection && line.trim().startsWith("class") || 
                        line.trim().startsWith("interface") ||
                        line.trim().startsWith("fun") ||
                        line.trim().startsWith("val") ||
                        line.trim().startsWith("var") ||
                        line.trim().startsWith("@")) {
                        inImportSection = false
                    }
                    result.add(line)
                    consecutiveEmpty = 0
                }
            }
        }
        
        return result
    }
}

fun main() {
    val tool = ImportCleanupTool()
    val analyses = tool.analyzeAllFiles()
    tool.printAnalysisReport(analyses)
    
    println("\n=== CLEANUP SUMMARY ===")
    println("Analysis complete. Review the report above to see files that need cleanup.")
    println("To proceed with automatic cleanup, run this tool with --cleanup flag")
}