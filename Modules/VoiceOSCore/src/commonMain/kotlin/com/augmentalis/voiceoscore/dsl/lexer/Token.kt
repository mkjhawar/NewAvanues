package com.augmentalis.voiceoscore.dsl.lexer

/**
 * All token types produced by the AVU DSL lexer.
 */
enum class TokenType {
    // Structure
    HEADER_SEPARATOR,     // ---
    INDENT,               // Synthetic: indentation increased
    DEDENT,               // Synthetic: indentation decreased
    NEWLINE,              // End of significant line
    EOF,                  // End of file

    // Literals
    STRING_LITERAL,       // "hello" or 'hello'
    INT_LITERAL,          // 42
    FLOAT_LITERAL,        // 3.14
    BOOLEAN_LITERAL,      // true, false

    // Identifiers & References
    IDENTIFIER,           // variable names, function names
    VARIABLE_REF,         // $name (dollar-prefixed reference)
    CODE_NAME,            // 3-letter uppercase: VCM, AAC, SCR, etc.

    // Directives (@ prefixed)
    AT_WORKFLOW,          // @workflow
    AT_DEFINE,            // @define
    AT_ON,                // @on
    AT_IF,                // @if
    AT_ELSE,              // @else
    AT_WAIT,              // @wait
    AT_REPEAT,            // @repeat
    AT_WHILE,             // @while
    AT_SEQUENCE,          // @sequence
    AT_LOG,               // @log
    AT_SET,               // @set
    AT_RETURN,            // @return
    AT_EMIT,              // @emit

    // Delimiters
    LPAREN,               // (
    RPAREN,               // )
    LBRACE,               // {
    RBRACE,               // }
    COMMA,                // ,
    COLON,                // :
    DOT,                  // .

    // Operators
    PLUS,                 // +
    MINUS,                // -
    STAR,                 // *
    SLASH,                // /
    EQ,                   // ==
    NEQ,                  // !=
    LT,                   // <
    GT,                   // >
    LTE,                  // <=
    GTE,                  // >=
    AND,                  // and
    OR,                   // or
    NOT,                  // not
    ASSIGN,               // = (single equals for named params)

    // Keywords
    KW_TIMEOUT,           // timeout

    // Special
    HEADER_LINE,          // Raw line inside --- ... --- header
    COMMENT,              // # line comment
    ERROR                 // Lexer error token
}

/**
 * A single token produced by the lexer.
 *
 * @property type The classification of this token
 * @property lexeme The source text that produced this token
 * @property line 1-based line number in the source
 * @property column 1-based column number in the source
 * @property literal Parsed literal value (Int, Double, String, Boolean) or null
 */
data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int,
    val literal: Any? = null
)
