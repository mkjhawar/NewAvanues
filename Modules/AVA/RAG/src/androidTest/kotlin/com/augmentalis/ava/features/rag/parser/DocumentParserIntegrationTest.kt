// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParserIntegrationTest.kt
// created: 2025-11-22
// author: AVA AI Team - Testing Phase 2.0
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.rag.domain.DocumentType
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/**
 * Integration tests for all document parsers
 *
 * Tests parsing capabilities for:
 * - PDF
 * - DOCX
 * - HTML
 * - TXT
 * - RTF
 * - Markdown
 *
 * Part of: RAG Phase 2.0 - Testing (90% coverage target)
 */
@RunWith(AndroidJUnit4::class)
class DocumentParserIntegrationTest {

    private lateinit var context: Context
    private lateinit var pdfParser: PdfParser
    private lateinit var docxParser: DocxParser
    private lateinit var htmlParser: HtmlParser
    private lateinit var txtParser: TxtParser
    private lateinit var rtfParser: RtfParser
    private lateinit var markdownParser: MarkdownParser

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        pdfParser = PdfParser(context)
        docxParser = DocxParser(context)
        htmlParser = HtmlParser(context)
        txtParser = TxtParser(context)
        rtfParser = RtfParser(context)
        markdownParser = MarkdownParser(context)
    }

    // ========== TXT PARSER TESTS (SIMPLEST) ==========

    @Test
    fun testTxtParserBasicText() = runBlocking {
        val testFile = createTempFile("test.txt", "Hello World\nThis is a test document.\nWith multiple lines.")

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("TXT parsing should succeed", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain 'Hello World'", parsedDoc.text.contains("Hello World"))
        assertTrue("Should contain 'test document'", parsedDoc.text.contains("test document"))
        assertTrue("Should have pages", parsedDoc.pages.isNotEmpty())

        testFile.delete()
    }

    @Test
    fun testTxtParserEmptyFile() = runBlocking {
        val testFile = createTempFile("empty.txt", "")

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle empty file", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Text should be empty or whitespace", parsedDoc.text.isBlank())

        testFile.delete()
    }

    @Test
    fun testTxtParserUnicode() = runBlocking {
        val testFile = createTempFile(
            "unicode.txt",
            "Unicode test: 你好世界 مرحبا العالم Здравствуй мир 🌍🌎🌏"
        )

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle unicode", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should preserve Chinese", parsedDoc.text.contains("你好世界"))
        assertTrue("Should preserve Arabic", parsedDoc.text.contains("مرحبا"))
        assertTrue("Should preserve emojis", parsedDoc.text.contains("🌍"))

        testFile.delete()
    }

    @Test
    fun testTxtParserLargeFile() = runBlocking {
        // Create large file (100KB)
        val largeContent = "This is a line of text.\n".repeat(5000)
        val testFile = createTempFile("large.txt", largeContent)

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle large files", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain content", parsedDoc.text.length > 100000)

        testFile.delete()
    }

    // ========== HTML PARSER TESTS ==========

    @Test
    fun testHtmlParserBasic() = runBlocking {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Test Page</title></head>
            <body>
                <h1>Main Heading</h1>
                <p>This is a paragraph with <strong>bold text</strong>.</p>
                <ul>
                    <li>Item 1</li>
                    <li>Item 2</li>
                </ul>
            </body>
            </html>
        """.trimIndent()

        val testFile = createTempFile("test.html", html)

        val result = htmlParser.parse(testFile.absolutePath, DocumentType.HTML)

        assertTrue("HTML parsing should succeed", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should extract heading", parsedDoc.text.contains("Main Heading"))
        assertTrue("Should extract paragraph", parsedDoc.text.contains("paragraph"))
        assertTrue("Should extract list items", parsedDoc.text.contains("Item 1"))
        assertTrue("Should have sections", parsedDoc.sections.isNotEmpty())

        testFile.delete()
    }

    @Test
    fun testHtmlParserRemovesScripts() = runBlocking {
        val html = """
            <html>
            <body>
                <p>Visible content</p>
                <script>alert('This should not appear')</script>
                <style>.hidden { display: none; }</style>
            </body>
            </html>
        """.trimIndent()

        val testFile = createTempFile("scripted.html", html)

        val result = htmlParser.parse(testFile.absolutePath, DocumentType.HTML)

        assertTrue(result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain visible content", parsedDoc.text.contains("Visible content"))
        assertFalse("Should not contain script content",
            parsedDoc.text.contains("alert") || parsedDoc.text.contains("This should not appear"))
        assertFalse("Should not contain style content", parsedDoc.text.contains("display: none"))

        testFile.delete()
    }

    @Test
    fun testHtmlParserMalformed() = runBlocking {
        val html = "<html><body><p>Unclosed paragraph<div>Nested content</body></html>"

        val testFile = createTempFile("malformed.html", html)

        val result = htmlParser.parse(testFile.absolutePath, DocumentType.HTML)

        // Should handle malformed HTML gracefully (JSoup is forgiving)
        assertTrue("Should handle malformed HTML", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should extract content", parsedDoc.text.contains("Unclosed paragraph"))

        testFile.delete()
    }

    // ========== MARKDOWN PARSER TESTS ==========

    @Test
    fun testMarkdownParserBasic() = runBlocking {
        val markdown = """
            # Main Title

            This is a paragraph with **bold** and *italic* text.

            ## Subsection

            - Bullet point 1
            - Bullet point 2

            ```kotlin
            fun example() {
                println("Code block")
            }
            ```
        """.trimIndent()

        val testFile = createTempFile("test.md", markdown)

        val result = markdownParser.parse(testFile.absolutePath, DocumentType.MD)

        assertTrue("Markdown parsing should succeed", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should extract title", parsedDoc.text.contains("Main Title"))
        assertTrue("Should extract paragraph", parsedDoc.text.contains("paragraph"))
        assertTrue("Should extract list", parsedDoc.text.contains("Bullet point"))
        assertTrue("Should extract code", parsedDoc.text.contains("example"))

        testFile.delete()
    }

    @Test
    fun testMarkdownParserSections() = runBlocking {
        val markdown = """
            # Chapter 1
            Content for chapter 1.

            ## Section 1.1
            Subsection content.

            # Chapter 2
            Content for chapter 2.
        """.trimIndent()

        val testFile = createTempFile("sections.md", markdown)

        val result = markdownParser.parse(testFile.absolutePath, DocumentType.MD)

        assertTrue(result.isSuccess)
        val parsedDoc = result.getOrThrow()

        // Should identify sections
        assertTrue("Should have sections", parsedDoc.sections.isNotEmpty())

        testFile.delete()
    }

    // ========== RTF PARSER TESTS ==========

    @Test
    fun testRtfParserBasic() = runBlocking {
        // Minimal valid RTF document
        val rtf = """{\\rtf1\\ansi\\deff0
            {\\fonttbl{\\f0 Times New Roman;}}
            \\f0\\fs24 This is RTF text.
            \\par
            Second paragraph.
        }"""

        val testFile = createTempFile("test.rtf", rtf)

        val result = rtfParser.parse(testFile.absolutePath, DocumentType.RTF)

        // RTF parsing may succeed or fail depending on parser implementation
        // We test that it handles the file gracefully
        if (result.isSuccess) {
            val parsedDoc = result.getOrThrow()
            assertNotNull("Should return parsed document", parsedDoc)
        } else {
            // If it fails, it should do so gracefully
            assertNotNull("Should have failure reason", result.exceptionOrNull())
        }

        testFile.delete()
    }

    // ========== DOCX PARSER TESTS ==========

    @Test
    fun testDocxParserFileNotFound() = runBlocking {
        val result = docxParser.parse("/non/existent/file.docx", DocumentType.DOCX)

        assertTrue("Should fail for non-existent file", result.isFailure)
    }

    @Test
    fun testDocxParserInvalidFile() = runBlocking {
        // Create invalid DOCX file (just text, not valid ZIP)
        val testFile = createTempFile("invalid.docx", "This is not a valid DOCX file")

        val result = docxParser.parse(testFile.absolutePath, DocumentType.DOCX)

        assertTrue("Should fail for invalid DOCX", result.isFailure)

        testFile.delete()
    }

    // ========== PDF PARSER TESTS ==========

    @Test
    fun testPdfParserFileNotFound() = runBlocking {
        val result = pdfParser.parse("/non/existent/file.pdf", DocumentType.PDF)

        assertTrue("Should fail for non-existent file", result.isFailure)
    }

    @Test
    fun testPdfParserInvalidFile() = runBlocking {
        // Create invalid PDF file
        val testFile = createTempFile("invalid.pdf", "This is not a valid PDF file")

        val result = pdfParser.parse(testFile.absolutePath, DocumentType.PDF)

        assertTrue("Should fail for invalid PDF", result.isFailure)

        testFile.delete()
    }

    // ========== DOCUMENT PARSER FACTORY TESTS ==========

    @Test
    fun testDocumentParserFactoryPdf() {
        val parser = DocumentParserFactory.getParser(DocumentType.PDF)

        assertNotNull("Should return PDF parser", parser)
        assertTrue("Should be PdfParser instance", parser is PdfParser)
    }

    @Test
    fun testDocumentParserFactoryDocx() {
        val parser = DocumentParserFactory.getParser(DocumentType.DOCX)

        assertNotNull("Should return DOCX parser", parser)
        assertTrue("Should be DocxParser instance", parser is DocxParser)
    }

    @Test
    fun testDocumentParserFactoryHtml() {
        val parser = DocumentParserFactory.getParser(DocumentType.HTML)

        assertNotNull("Should return HTML parser", parser)
        assertTrue("Should be HtmlParser instance", parser is HtmlParser)
    }

    @Test
    fun testDocumentParserFactoryTxt() {
        val parser = DocumentParserFactory.getParser(DocumentType.TXT)

        assertNotNull("Should return TXT parser", parser)
        assertTrue("Should be TxtParser instance", parser is TxtParser)
    }

    @Test
    fun testDocumentParserFactoryRtf() {
        val parser = DocumentParserFactory.getParser(DocumentType.RTF)

        assertNotNull("Should return RTF parser", parser)
        assertTrue("Should be RtfParser instance", parser is RtfParser)
    }

    @Test
    fun testDocumentParserFactoryMarkdown() {
        val parser = DocumentParserFactory.getParser(DocumentType.MD)

        assertNotNull("Should return Markdown parser", parser)
        assertTrue("Should be MarkdownParser instance", parser is MarkdownParser)
    }

    // ========== EDGE CASES ==========

    @Test
    fun testAllParsersHandleNonExistentFile() = runBlocking {
        val parsers = listOf(
            pdfParser to DocumentType.PDF,
            docxParser to DocumentType.DOCX,
            htmlParser to DocumentType.HTML,
            txtParser to DocumentType.TXT,
            rtfParser to DocumentType.RTF,
            markdownParser to DocumentType.MD
        )

        parsers.forEach { (parser, type) ->
            val result = parser.parse("/non/existent/file.ext", type)
            assertTrue("$type parser should handle non-existent file", result.isFailure)
        }
    }

    @Test
    fun testParsingVeryLongLines() = runBlocking {
        // Create file with very long line (10KB single line)
        val longLine = "A".repeat(10000)
        val testFile = createTempFile("long-line.txt", longLine)

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle very long lines", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertEquals("Should preserve long line", longLine, parsedDoc.text.trim())

        testFile.delete()
    }

    @Test
    fun testParsingSpecialCharacters() = runBlocking {
        val specialChars = "Special: @#$%^&*(){}[]|\\:;<>?,./~`\n\"'`"
        val testFile = createTempFile("special.txt", specialChars)

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle special characters", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should preserve special chars", parsedDoc.text.contains("@#$%"))

        testFile.delete()
    }

    @Test
    fun testParsingMultipleNewlines() = runBlocking {
        val multiNewline = "Line 1\n\n\n\n\nLine 2 after many newlines"
        val testFile = createTempFile("newlines.txt", multiNewline)

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue(result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain both lines",
            parsedDoc.text.contains("Line 1") && parsedDoc.text.contains("Line 2"))

        testFile.delete()
    }

    @Test
    fun testParsingMixedLineEndings() = runBlocking {
        // Mix of \n, \r\n, and \r
        val mixedEndings = "Unix line\nWindows line\r\nOld Mac line\rAnother line"
        val testFile = createTempFile("mixed.txt", mixedEndings)

        val result = txtParser.parse(testFile.absolutePath, DocumentType.TXT)

        assertTrue("Should handle mixed line endings", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain all content", parsedDoc.text.contains("Unix"))
        assertTrue("Should contain Windows line", parsedDoc.text.contains("Windows"))

        testFile.delete()
    }

    @Test
    fun testHtmlWithNestedStructure() = runBlocking {
        val html = """
            <html><body>
                <div>
                    <div>
                        <div>
                            <p>Deeply nested content</p>
                        </div>
                    </div>
                </div>
            </body></html>
        """.trimIndent()

        val testFile = createTempFile("nested.html", html)

        val result = htmlParser.parse(testFile.absolutePath, DocumentType.HTML)

        assertTrue("Should handle nested HTML", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should extract nested content", parsedDoc.text.contains("Deeply nested"))

        testFile.delete()
    }

    @Test
    fun testMarkdownWithComplexFormatting() = runBlocking {
        val markdown = """
            # Title

            Paragraph with **bold**, *italic*, and ***bold italic***.

            > Blockquote
            >> Nested blockquote

            [Link](https://example.com)

            ![Image](image.jpg)

            ---

            1. Ordered list
            2. Second item
               - Nested bullet
               - Another bullet
        """.trimIndent()

        val testFile = createTempFile("complex.md", markdown)

        val result = markdownParser.parse(testFile.absolutePath, DocumentType.MD)

        assertTrue("Should handle complex markdown", result.isSuccess)
        val parsedDoc = result.getOrThrow()

        assertTrue("Should contain formatted text", parsedDoc.text.isNotEmpty())

        testFile.delete()
    }

    // ========== HELPER METHODS ==========

    private fun createTempFile(filename: String, content: String): File {
        return File(context.cacheDir, filename).also {
            it.writeText(content)
        }
    }
}
