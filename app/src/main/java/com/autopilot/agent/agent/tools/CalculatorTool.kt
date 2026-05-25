package com.autopilot.agent.agent.tools

import com.autopilot.agent.domain.model.ToolResult
import javax.inject.Inject
import kotlin.math.*

/**
 * Safe mathematical expression evaluator.
 * Supports: +, -, *, /, ^, sqrt, sin, cos, tan, log, ln, pi, e, abs, parentheses.
 * No eval() or ScriptEngine for security.
 */
class CalculatorTool @Inject constructor() : Tool {

    override val name = "CALCULATOR"
    override val description = "Evaluate mathematical expressions safely"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val expression = input["expression"]?.toString()

        if (expression.isNullOrBlank()) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Mathematical expression is required",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        return try {
            val result = evaluate(expression)
            val formatted = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                "%.10g".format(result)
            }
            ToolResult(
                toolName = name,
                success = true,
                output = "$expression = $formatted",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Calculation error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /** Recursive descent parser for mathematical expressions. */
    private fun evaluate(expr: String): Double {
        val cleanExpr = expr
            .replace(" ", "")
            .replace("pi", Math.PI.toString())
            .replace("PI", Math.PI.toString())
            .replace("(?<![a-zA-Z])e(?![a-zA-Z])".toRegex(), Math.E.toString())
        val parser = ExprParser(cleanExpr)
        val result = parser.parseExpression()
        if (parser.pos < parser.input.length) {
            throw IllegalArgumentException("Unexpected character: '${parser.input[parser.pos]}'")
        }
        return result
    }

    private class ExprParser(val input: String) {
        var pos = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < input.length) {
                when (input[pos]) {
                    '+' -> { pos++; result += parseTerm() }
                    '-' -> { pos++; result -= parseTerm() }
                    else -> break
                }
            }
            return result
        }

        fun parseTerm(): Double {
            var result = parsePower()
            while (pos < input.length) {
                when (input[pos]) {
                    '*' -> { pos++; result *= parsePower() }
                    '/' -> {
                        pos++
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }
                    '%' -> { pos++; result %= parsePower() }
                    else -> break
                }
            }
            return result
        }

        fun parsePower(): Double {
            var result = parseUnary()
            while (pos < input.length && input[pos] == '^') {
                pos++
                result = result.pow(parseUnary())
            }
            return result
        }

        fun parseUnary(): Double {
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseUnary()
            }
            if (pos < input.length && input[pos] == '+') {
                pos++
                return parseUnary()
            }
            return parsePrimary()
        }

        fun parsePrimary(): Double {
            // Parentheses
            if (pos < input.length && input[pos] == '(') {
                pos++ // skip '('
                val result = parseExpression()
                if (pos < input.length && input[pos] == ')') {
                    pos++ // skip ')'
                }
                return result
            }

            // Functions
            val funcNames = listOf("sqrt", "sin", "cos", "tan", "log", "ln", "abs", "ceil", "floor", "round")
            for (funcName in funcNames) {
                if (input.startsWith(funcName, pos)) {
                    pos += funcName.length
                    if (pos < input.length && input[pos] == '(') {
                        pos++ // skip '('
                        val arg = parseExpression()
                        if (pos < input.length && input[pos] == ')') {
                            pos++ // skip ')'
                        }
                        return when (funcName) {
                            "sqrt" -> sqrt(arg)
                            "sin" -> sin(arg)
                            "cos" -> cos(arg)
                            "tan" -> tan(arg)
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            "abs" -> abs(arg)
                            "ceil" -> ceil(arg)
                            "floor" -> floor(arg)
                            "round" -> round(arg)
                            else -> throw IllegalArgumentException("Unknown function: $funcName")
                        }
                    }
                }
            }

            // Number
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            if (pos > start) {
                return input.substring(start, pos).toDouble()
            }

            throw IllegalArgumentException("Unexpected input at position $pos: '${if (pos < input.length) input[pos] else "EOF"}'")
        }
    }
}
