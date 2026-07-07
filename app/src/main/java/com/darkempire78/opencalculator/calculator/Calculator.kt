package com.darkempire78.opencalculator.calculator

import android.os.Build
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.acosh
import kotlin.math.asinh
import kotlin.math.atanh
import kotlin.math.cosh
import kotlin.math.sinh
import kotlin.math.tanh

/**
 * Outcome of a calculation performed by [Calculator.evaluate].
 *
 * Replaces the former package-level mutable error flags (`divisionByZero`,
 * `domainError`, `syntaxError`, `isInfinity`, `requireRealNumber`). Those
 * were shared global state: not thread-safe and prone to leaking error state
 * from one evaluation into the next (the "stale state" bug). `evaluate()` now
 * returns one of these variants instead of mutating globals.
 */
sealed class CalculationResult {
    /**
     * The numeric value produced by the parser. Always present so callers can
     * still inspect it for error variants (e.g. the sign of an overflow when
     * deciding between the "infinity" and "value too large" messages).
     */
    abstract val value: BigDecimal

    data class Success(override val value: BigDecimal) : CalculationResult()

    sealed class Error : CalculationResult() {
        data class SyntaxError(override val value: BigDecimal = BigDecimal.ZERO) : Error()
        data class DomainError(override val value: BigDecimal = BigDecimal.ZERO) : Error()
        data class RequireRealNumber(override val value: BigDecimal = BigDecimal.ZERO) : Error()
        data class DivisionByZero(override val value: BigDecimal = BigDecimal.ZERO) : Error()
        data class Infinity(override val value: BigDecimal = BigDecimal.ZERO) : Error()
    }
}

class Calculator(
    private val numberPrecisionDecimal: Int
) {

    companion object {
        /** Factorial inputs at or above this value overflow BigInteger → treated as infinity. */
        const val MAX_FACTORIAL_INPUT = 3000

        /** Exponents above this value bound computation as infinity to prevent runaway growth. */
        const val MAX_EXPONENT = 10000

        /**
         * Fractional parts smaller than this are snapped to zero after exponentiation.
         * Prevents floating-point noise in results like sqrt(2)^2.
         */
        const val FRACTIONAL_SNAP_EPSILON = 1.0E-30

        /**
         * Trig results whose absolute value is smaller than this are snapped to zero.
         * Compensates for floating-point error in expressions like sin(π).
         */
        const val TRIG_SNAP_EPSILON = 1.0E-14

        /** Tolerance for detecting tangent's vertical asymptotes after floating-point conversions. */
        const val TAN_ASYMPTOTE_EPSILON = 1.0E-10

        /** Extra decimal digits added to the integer part length for sqrt precision. */
        const val SQRT_PRECISION_EXTRA_DIGITS = 50

        /** Hard cap on BigDecimal precision used in sqrt calculations. */
        const val MAX_SQRT_PRECISION = 1000
    }

    // Per-instance error state, set by the parser and math helpers during
    // evaluate(). A fresh Calculator is created for each evaluation and
    // evaluate() resets these at the start, so state never leaks across calls.
    private var divisionByZero = false
    private var domainError = false
    private var syntaxError = false
    private var isInfinity = false
    private var requireRealNumber = false

    private fun snapTrigZero(value: BigDecimal): BigDecimal =
        if (value.abs() < BigDecimal.valueOf(TRIG_SNAP_EPSILON)) {
            BigDecimal.ZERO
        } else {
            value
        }

    fun factorial(number: BigDecimal): BigDecimal {
        if (number >= BigDecimal(MAX_FACTORIAL_INPUT)) {
            isInfinity = true
            return BigDecimal.ZERO
        }
        return if (number < BigDecimal.ZERO) {
            domainError = true
            BigDecimal.ZERO
        } else {
            val decimalPartOfNumber = number.toDouble() - number.toInt()
            if (decimalPartOfNumber == 0.0) {
                var factorial = BigInteger("1")
                for (i in 1..number.toInt()) {
                    factorial *= i.toBigInteger()
                }
                factorial.toBigDecimal()
            } else gammaLanczos(number + BigDecimal.ONE)
        }
    }

    private fun gammaLanczos(x: BigDecimal): BigDecimal {
        // Lanczos approximation parameters
        val p = arrayOf(
            676.5203681218851,
            -1259.1392167224028,
            771.3234287776531,
            -176.6150291621406,
            12.507343278686905,
            -0.13857109526572012,
            9.984369578019572e-6,
            1.5056327351493116e-7
        )
        val g = 7.0
        val z = x.toDouble() - 1.0

        var a = 0.9999999999998099
        for (i in p.indices) {
            a += p[i] / (z + i + 1)
        }

        val t = z + g + 0.5
        val sqrtTwoPi = sqrt(2.0 * PI)
        val firstPart = sqrtTwoPi * t.pow(z + 0.5) * exp(-t)
        val result = firstPart * a

        return BigDecimal(result, MathContext.DECIMAL64)
    }

    private fun exponentiation(x: BigDecimal, parseFactor: BigDecimal): BigDecimal {
        var value = x
        val intPart = parseFactor.toInt()
        val decimalPart = parseFactor.subtract(BigDecimal(intPart))

        // if the number is null
        if (value == BigDecimal.ZERO) {
            syntaxError = false
            value = BigDecimal.ZERO
        } else {
            if (parseFactor > BigDecimal(MAX_EXPONENT)) {
                isInfinity = true
                value = BigDecimal.ZERO
            } else {
                // If the number is negative and the factor is a float ( e.g : (-5)^0.5 )
                if (value < BigDecimal.ZERO && decimalPart != BigDecimal.ZERO) {
                    requireRealNumber = true
                } // the factor is NOT a float
                else if (parseFactor > BigDecimal.ZERO) {

                    // To support bigdecimal exponent (e.g: 3.5)
                    value = value.pow(intPart, MathContext.UNLIMITED)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(decimalPart.toDouble())
                            )
                        )

                    // To fix sqrt(2)^2 = 2
                    val decimal = value.toInt()
                    val fractional = value.toDouble() - decimal
                    if (fractional > 0 && fractional < FRACTIONAL_SNAP_EPSILON) {
                        value = decimal.toBigDecimal()
                    }
                } else {
                    // To support negative factor
                    value = value.pow(-intPart, MathContext.DECIMAL64)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(-decimalPart.toDouble())
                            )
                        )

                    value = try {
                        BigDecimal.ONE.divide(value)
                    } catch (e: ArithmeticException) {
                        // if the result is a non-terminating decimal expansion
                        BigDecimal.ONE.divide(value, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                    }
                }
            }
        }
        return value
    }

    fun bigDecimalSqrtFormerAndroidVersion(value: BigDecimal, mathContext: MathContext): BigDecimal {
        // Newton's method for square root calculation with Android versions prior to API 33
        var x0 = BigDecimal(0)
        var x1 = value.divide(BigDecimal(2), mathContext)

        // != evaluated true when comparing 0 and 0.0
        // This allowed the passing of 0.0 (or more trailing zeroes) to be divided.
        while (x0 < x1 || x0 > x1) {
            x0 = x1
            x1 = value.divide(x0, mathContext).add(x0).divide(BigDecimal(2), mathContext)
        }

        return x1
    }

    private fun isTangentAsymptote(angle: BigDecimal, isDegreeModeActivated: Boolean): Boolean {
        val angleAsDouble = angle.toDouble()
        val asymptote = if (isDegreeModeActivated) 90.0 else PI / 2.0
        val period = if (isDegreeModeActivated) 180.0 else PI
        val normalizedDistance = Math.IEEEremainder(angleAsDouble - asymptote, period)

        return abs(normalizedDistance) <= TAN_ASYMPTOTE_EPSILON
    }

    fun evaluate(equation: String, isDegreeModeActivated: Boolean): CalculationResult {
        // Reset per-evaluation error state before parsing.
        divisionByZero = false
        domainError = false
        syntaxError = false
        isInfinity = false
        requireRealNumber = false

        val value = object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) {
                    syntaxError = true
                }
                return x
            }

            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm()) // addition
                    else if (eat('-'.code)) x = x.subtract(parseTerm()) // subtraction
                    else return x
                }
            }

            fun parseTerm(): BigDecimal {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x = x.multiply(parseFactor()) // Multiplication
                    else if (eat('#'.code)) { // Modulo
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator == BigDecimal.ZERO) {
                            divisionByZero = true
                            x = BigDecimal.ZERO
                        } else {
                            x = x.rem(fractionDenominator)
                        }
                    }
                    else if (eat('/'.code)) { // Division
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator.compareTo(BigDecimal.ZERO) == 0) {
                            divisionByZero = true
                            x = BigDecimal.ZERO
                        } else {
                            try {
                                x = x.divide(fractionDenominator)
                            } catch (e: ArithmeticException) { // if the result is a non-terminating decimal expansion
                                x = x.divide(fractionDenominator, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                                
                            }
                        }
                    }
                    else return x
                }
            }

            fun parseFactor(): BigDecimal {
                if (eat('+'.code)) return parseFactor().plus() // unary plus
                if (eat('-'.code)) return parseFactor().unaryMinus() // unary minus
                var x: BigDecimal
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) {
                        println("Missing ')'")
                        x = BigDecimal.ZERO
                        syntaxError = true
                    }
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val string = equation.substring(startPos, pos)
                    if (string.count { it == '.' } > 1) {
                        x = BigDecimal.ZERO
                        syntaxError = true
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            x = BigDecimal.ZERO
                            syntaxError = true
                        } else {
                            x = BigDecimal(string)
                        }
                    }
                } else if (eat('e'.code)) {
                    x = BigDecimal(Math.E)
                } else if (eat('π'.code)) {
                    x = BigDecimal(PI)
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func: String = equation.substring(startPos, pos)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    
                    when (func) {
                        "sqrt" -> {
                            if (x >= BigDecimal.ZERO) {
                                // Set the precision for the square root calculation
                                val integerPartLength = x.toString().length
                                val maxPrecision = (integerPartLength + SQRT_PRECISION_EXTRA_DIGITS).coerceAtMost(MAX_SQRT_PRECISION) // Maximum precision is 1000
                                val precision = MathContext(maxPrecision, RoundingMode.HALF_DOWN)
                                x = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Use default BigDecimal sqrt function (API 33)
                                    x.sqrt(precision)
                                } else { // Use Newton's method for square root calculation with Android versions prior to API 33
                                    bigDecimalSqrtFormerAndroidVersion(x, precision)
                                }
                            } else {
                                requireRealNumber = true
                            }

                        }
                        "factorial" -> {
                            x = factorial(x)
                        }
                        "cbrt" -> {
                            // Cube root is defined for negatives, so no domain guard is needed.
                            x = BigDecimal(Math.cbrt(x.toDouble()))
                        }
                        "ln" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domainError = true
                            } else {
                                x = BigDecimal(ln(x.toDouble()))
                            }
                        }
                        "logtwo" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domainError = true
                            } else {
                                x = BigDecimal(log2(x.toDouble()))
                            }
                        }
                        "logten" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domainError = true
                            } else {
                                x = BigDecimal(log10(x.toDouble()))
                            }
                        }
                        "xp" -> {
                            x = exponentiation(BigDecimal(Math.E), x)
                        }
                        "sin" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = sin(Math.toRadians(x.toDouble())).toBigDecimal()
                                // https://stackoverflow.com/questions/29516222/how-to-get-exact-value-of-trigonometric-functions-in-java
                            } else {
                                x = sin(x.toDouble()).toBigDecimal()
                            }
                            x = snapTrigZero(x)
                        }
                        "cos" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = cos(Math.toRadians(x.toDouble())).toBigDecimal()
                            } else {
                                x = cos(x.toDouble()).toBigDecimal()
                            }
                            x = snapTrigZero(x)
                        }
                        "tan" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if (isTangentAsymptote(x, isDegreeModeActivated)) {
                                // Tangent is defined for R\{90° + 180°k} in degrees,
                                // or R\{π/2 + πk} in radians, with k ∈ Z.
                                domainError = true
                                x = BigDecimal.ZERO
                            } else {
                                x = if (isDegreeModeActivated) {
                                    tan(Math.toRadians(x.toDouble())).toBigDecimal()
                                } else {
                                    tan(x.toDouble()).toBigDecimal()
                                }
                                x = snapTrigZero(x)
                            }
                        }
                        "arcsi" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domainError = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (asin(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                                } else {
                                    asin(x.toDouble()).toBigDecimal()
                                }
                                x = snapTrigZero(x)
                            }
                        }
                        "arcco" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domainError = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (acos(x.toDouble())*180/Math.PI).toBigDecimal()
                                } else {
                                    acos(x.toDouble()).toBigDecimal()
                                }
                                x = snapTrigZero(x)
                            }

                        }
                        "arcta" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else if  (isDegreeModeActivated) {
                                x = (atan(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                            } else {
                                x =atan(x.toDouble()).toBigDecimal()
                            }
                            x = snapTrigZero(x)
                        }
                        "sinh" -> {
                            val d = sinh(x.toDouble())
                            if (d.isInfinite()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else {
                                x = d.toBigDecimal()
                            }
                        }
                        "cosh" -> {
                            val d = cosh(x.toDouble())
                            if (d.isInfinite()) {
                                isInfinity = true
                                x = BigDecimal.ZERO
                            } else {
                                x = d.toBigDecimal()
                            }
                        }
                        "tanh" -> {
                            x = tanh(x.toDouble()).toBigDecimal()
                        }
                        "arsih" -> {
                            // Inverse hyperbolic sine (asinh); defined for all reals.
                            x = asinh(x.toDouble()).toBigDecimal()
                        }
                        "arcoh" -> {
                            // Inverse hyperbolic cosine (acosh); domain x >= 1.
                            if (x < BigDecimal.ONE) {
                                domainError = true
                                x = BigDecimal.ZERO
                            } else {
                                x = acosh(x.toDouble()).toBigDecimal()
                            }
                        }
                        "artah" -> {
                            // Inverse hyperbolic tangent (atanh); domain -1 < x < 1.
                            if (abs(x.toDouble()) >= 1) {
                                domainError = true
                                x = BigDecimal.ZERO
                            } else {
                                x = atanh(x.toDouble()).toBigDecimal()
                            }
                        }
                        else -> {
                            syntaxError = true
                        }
                    }
                } else {
                    x = BigDecimal.ZERO
                    syntaxError = true
                }
                if (eat('^'.code)) {
                    x = exponentiation(x, parseFactor())
                }
                return x
            }
        }.parse()

        // Precedence preserved from the original error-flag check order used in
        // MainActivity: syntax > domain > requireReal > divisionByZero > infinity.
        return when {
            syntaxError -> CalculationResult.Error.SyntaxError(value)
            domainError -> CalculationResult.Error.DomainError(value)
            requireRealNumber -> CalculationResult.Error.RequireRealNumber(value)
            divisionByZero -> CalculationResult.Error.DivisionByZero(value)
            isInfinity -> CalculationResult.Error.Infinity(value)
            else -> CalculationResult.Success(value)
        }
    }
}
