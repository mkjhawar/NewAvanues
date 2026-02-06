package com.augmentalis.voiceoscore.dsl.ast

/**
 * Source location for error reporting and debugging.
 */
data class SourceLocation(
    val line: Int,
    val column: Int,
    val length: Int = 0
)

/**
 * AVU DSL file type.
 */
enum class AvuDslFileType {
    WORKFLOW,
    PLUGIN
}

/**
 * Extended header data for DSL files.
 * Wraps AVUCodec HeaderData concepts and adds DSL-specific fields.
 */
data class AvuDslHeader(
    val schema: String,
    val version: String,
    val type: AvuDslFileType,
    val metadata: Map<String, String>,
    val codes: Map<String, String>,
    val permissions: List<String>,
    val triggers: List<String>,
    val location: SourceLocation
)

/**
 * Root node of a parsed AVU DSL file.
 */
data class AvuDslFile(
    val header: AvuDslHeader,
    val declarations: List<AvuAstNode.Declaration>,
    val location: SourceLocation
)

/**
 * AVU DSL Abstract Syntax Tree node hierarchy.
 *
 * Immutable sealed class hierarchy. All nodes carry [SourceLocation] for error reporting.
 * Three main categories: [Declaration] (top-level), [Statement] (inside bodies),
 * and [Expression] (values and conditions).
 */
sealed class AvuAstNode {

    abstract val location: SourceLocation

    // =========================================================================
    // TOP-LEVEL DECLARATIONS
    // =========================================================================

    sealed class Declaration : AvuAstNode() {

        /**
         * @workflow "Name"
         *   <body>
         */
        data class Workflow(
            val name: String,
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Declaration()

        /**
         * @define function_name(param1, param2)
         *   <body>
         */
        data class FunctionDef(
            val name: String,
            val parameters: List<String>,
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Declaration()

        /**
         * @on "trigger pattern {var}"
         *   <body>
         */
        data class TriggerHandler(
            val pattern: String,
            val captureVars: List<String>,
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Declaration()
    }

    // =========================================================================
    // STATEMENTS (inside declaration bodies)
    // =========================================================================

    sealed class Statement : AvuAstNode() {

        /**
         * CODE(param: value, param2: value2)
         * e.g., VCM(id: "cmd1", action: "SCROLL_TOP")
         */
        data class CodeInvocation(
            val code: String,
            val arguments: List<NamedArgument>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * function_name(param: value)
         * Call to a @define'd function.
         */
        data class FunctionCall(
            val name: String,
            val arguments: List<NamedArgument>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @wait 1000
         * Simple delay in milliseconds.
         */
        data class WaitDelay(
            val milliseconds: Expression,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @wait condition timeout 5000
         * Wait for a condition with timeout.
         */
        data class WaitCondition(
            val condition: Expression,
            val timeoutMs: Expression,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @if condition
         *   <thenBody>
         * @else
         *   <elseBody>
         */
        data class IfElse(
            val condition: Expression,
            val thenBody: List<Statement>,
            val elseBody: List<Statement>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @repeat 5
         *   <body>
         */
        data class Repeat(
            val count: Expression,
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @while condition
         *   <body>
         */
        data class While(
            val condition: Expression,
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @sequence
         *   CODE(...)
         *   CODE(...)
         * Executes statements sequentially (explicit grouping).
         */
        data class Sequence(
            val body: List<Statement>,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @set variable_name = expression
         */
        data class Assignment(
            val variableName: String,
            val value: Expression,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @log "message" or @log expression
         */
        data class Log(
            val message: Expression,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @return expression
         */
        data class Return(
            val value: Expression?,
            override val location: SourceLocation
        ) : Statement()

        /**
         * @emit "event_name" with optional data
         */
        data class Emit(
            val eventName: String,
            val data: Expression?,
            override val location: SourceLocation
        ) : Statement()
    }

    // =========================================================================
    // EXPRESSIONS
    // =========================================================================

    sealed class Expression : AvuAstNode() {

        data class StringLiteral(
            val value: String,
            override val location: SourceLocation
        ) : Expression()

        data class IntLiteral(
            val value: Int,
            override val location: SourceLocation
        ) : Expression()

        data class FloatLiteral(
            val value: Double,
            override val location: SourceLocation
        ) : Expression()

        data class BooleanLiteral(
            val value: Boolean,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Variable reference: $name
         */
        data class VariableRef(
            val name: String,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Bare identifier (for conditions, built-in objects like `screen`).
         */
        data class Identifier(
            val name: String,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Binary operation: left op right
         */
        data class BinaryOp(
            val left: Expression,
            val operator: BinaryOperator,
            val right: Expression,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Unary operation: not condition, -value
         */
        data class UnaryOp(
            val operator: UnaryOperator,
            val operand: Expression,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Member access: screen.contains
         */
        data class MemberAccess(
            val target: Expression,
            val member: String,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Function/method call expression: contains("text"), len($list)
         */
        data class CallExpression(
            val callee: Expression,
            val arguments: List<Expression>,
            override val location: SourceLocation
        ) : Expression()

        /**
         * Parenthesized expression for grouping: (a + b)
         */
        data class Grouped(
            val inner: Expression,
            override val location: SourceLocation
        ) : Expression()
    }

    // =========================================================================
    // SUPPORTING TYPES
    // =========================================================================

    /**
     * Named argument in CODE invocations and function calls.
     * Can be named (param: value) or positional (just value).
     */
    data class NamedArgument(
        val name: String?,
        val value: Expression,
        override val location: SourceLocation
    ) : AvuAstNode()
}

/**
 * Binary operators ordered by typical precedence context.
 */
enum class BinaryOperator {
    PLUS, MINUS, STAR, SLASH,
    EQ, NEQ, LT, GT, LTE, GTE,
    AND, OR
}

/**
 * Unary operators.
 */
enum class UnaryOperator {
    NOT, NEGATE
}
