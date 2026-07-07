package com.darkempire78.opencalculator.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** A single reduction in an order-of-operations trace, e.g. "3 × 16 = 48". */
data class EvaluationStep(val description: String)

/** Outcome of building a step-by-step trace. */
sealed class StepEvaluationResult {
    /** Ordered reduction [steps]; empty when the expression is a bare number. */
    data class Success(val steps: List<EvaluationStep>) : StepEvaluationResult()

    /** The expression uses features the tracer does not cover (functions, π, …). */
    object Unsupported : StepEvaluationResult()
}

/**
 * Pure order-of-operations tracer for basic arithmetic (+ − × ÷ ^ and parentheses).
 *
 * Tokenises a normalised expression (decimals with '.', operators '+ - * / ^'),
 * converts to RPN via shunting-yard, then evaluates the RPN recording each binary
 * reduction. Anything else (functions, π, √, %, unary minus before a parenthesis)
 * yields [StepEvaluationResult.Unsupported]. This is a teaching aid; the
 * authoritative result still comes from [Calculator].
 */
object StepEvaluator {

    private val PRECEDENCE = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, '^' to 3)
    private val MATH_CONTEXT = MathContext(15, RoundingMode.HALF_UP)

    /** Builds the step-by-step trace for [expression]. */
    fun evaluate(expression: String): StepEvaluationResult {
        val tokens = tokenize(expression) ?: return StepEvaluationResult.Unsupported
        val rpn = toReversePolish(tokens) ?: return StepEvaluationResult.Unsupported
        val steps = reduce(rpn) ?: return StepEvaluationResult.Unsupported
        return StepEvaluationResult.Success(steps)
    }

    /** Splits [expression] into number / operator / parenthesis tokens. */
    private fun tokenize(expression: String): List<String>? {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            when {
                c == ' ' -> i++
                c.isDigit() || c == '.' -> { val end = readNumber(expression, i); tokens.add(expression.substring(i, end)); i = end }
                c == '-' && isUnaryContext(tokens) -> {
                    if (i + 1 < expression.length && (expression[i + 1].isDigit() || expression[i + 1] == '.')) {
                        val end = readNumber(expression, i + 1); tokens.add(expression.substring(i, end)); i = end
                    } else return null
                }
                c in "+-*/^()" -> { tokens.add(c.toString()); i++ }
                else -> return null
            }
        }
        return tokens
    }

    private fun readNumber(expression: String, start: Int): Int {
        var j = start
        while (j < expression.length && (expression[j].isDigit() || expression[j] == '.')) j++
        return j
    }

    private fun isUnaryContext(tokens: List<String>): Boolean =
        tokens.isEmpty() || tokens.last() == "(" || (tokens.last().length == 1 && tokens.last()[0] in PRECEDENCE)

    /** Shunting-yard conversion to reverse-Polish notation. */
    private fun toReversePolish(tokens: List<String>): List<String>? {
        val output = mutableListOf<String>()
        val operators = ArrayDeque<String>()
        for (token in tokens) {
            when {
                token == "(" -> operators.addLast(token)
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") output.add(operators.removeLast())
                    if (operators.isEmpty()) return null
                    operators.removeLast()
                }
                token.length == 1 && token[0] in PRECEDENCE -> {
                    while (operators.isNotEmpty() && shouldPopOperator(operators.last(), token[0])) output.add(operators.removeLast())
                    operators.addLast(token)
                }
                else -> output.add(token)
            }
        }
        while (operators.isNotEmpty()) {
            val op = operators.removeLast()
            if (op == "(" || op == ")") return null
            output.add(op)
        }
        return output
    }

    /** Whether [top] should be popped before pushing operator [current] (^ is right-associative). */
    private fun shouldPopOperator(top: String, current: Char): Boolean {
        if (top.length != 1 || top[0] !in PRECEDENCE) return false
        val topPrecedence = PRECEDENCE.getValue(top[0])
        val currentPrecedence = PRECEDENCE.getValue(current)
        return if (current == '^') topPrecedence > currentPrecedence else topPrecedence >= currentPrecedence
    }

    /** Evaluates the RPN, recording each binary reduction as a step. */
    private fun reduce(rpn: List<String>): List<EvaluationStep>? {
        val stack = ArrayDeque<BigDecimal>()
        val steps = mutableListOf<EvaluationStep>()
        for (token in rpn) {
            if (token.length == 1 && token[0] in PRECEDENCE) {
                if (stack.size < 2) return null
                val right = stack.removeLast()
                val left = stack.removeLast()
                val result = applyOperator(left, token[0], right) ?: return null
                steps.add(EvaluationStep("${format(left)} ${displaySymbol(token[0])} ${format(right)} = ${format(result)}"))
                stack.addLast(result)
            } else {
                stack.addLast(token.toBigDecimalOrNull() ?: return null)
            }
        }
        return if (stack.size == 1) steps else null
    }

    private fun applyOperator(left: BigDecimal, operator: Char, right: BigDecimal): BigDecimal? = when (operator) {
        '+' -> left.add(right)
        '-' -> left.subtract(right)
        '*' -> left.multiply(right)
        '/' -> if (right.signum() == 0) null else left.divide(right, MATH_CONTEXT)
        '^' -> power(left, right)
        else -> null
    }

    private fun power(base: BigDecimal, exponent: BigDecimal): BigDecimal? {
        val result = Math.pow(base.toDouble(), exponent.toDouble())
        return if (result.isFinite()) BigDecimal(result, MATH_CONTEXT) else null
    }

    private fun format(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    private fun displaySymbol(operator: Char): String = when (operator) {
        '*' -> "×"
        '/' -> "÷"
        '-' -> "−"
        else -> operator.toString()
    }
}
