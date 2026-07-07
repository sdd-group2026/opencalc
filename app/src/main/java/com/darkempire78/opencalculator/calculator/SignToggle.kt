package com.darkempire78.opencalculator.calculator

/**
 * Result of toggling the sign of the operand at the cursor.
 *
 * @property text the updated expression string to display in the input field.
 * @property cursorPosition where the caret should sit after the toggle.
 */
data class SignToggleResult(val text: String, val cursorPosition: Int)

/**
 * Pure sign-toggle ("±") logic for the calculator input.
 *
 * Toggling adds or removes a *unary* minus in front of the number the caret is
 * currently on. The recursive-descent parser in [Calculator] accepts a leading
 * unary minus after an operator, the start of the expression, or an opening
 * parenthesis (e.g. `5×-3`, `-3`, `(-3)`), so no parenthesis wrapping is needed.
 *
 * Every function is side-effect free, so the sign logic stays out of the
 * Activity layer and can be unit-tested in isolation.
 */
object SignToggle {

    /** Characters after which a minus is interpreted as unary (negation). */
    private const val UNARY_CONTEXT = "+-×÷^("

    /**
     * Toggles the sign of the operand located immediately left of [cursorPosition].
     *
     * @param input current (display-formatted) expression.
     * @param cursorPosition caret index within [input].
     * @param decimalSeparator locale decimal separator, treated as part of a number.
     * @param groupingSeparator locale grouping separator, treated as part of a number.
     */
    fun toggle(
        input: String,
        cursorPosition: Int,
        decimalSeparator: String,
        groupingSeparator: String
    ): SignToggleResult {
        if (input.isEmpty()) return SignToggleResult("-", 1)
        val caret = cursorPosition.coerceIn(0, input.length)
        val operandStart = findOperandStart(input, caret, numberChars(decimalSeparator, groupingSeparator))
        return if (hasUnaryMinusBefore(input, operandStart)) {
            removeMinus(input, operandStart, caret)
        } else {
            insertMinus(input, operandStart, caret)
        }
    }

    /** Builds the set of characters that make up a number for the active locale. */
    private fun numberChars(decimalSeparator: String, groupingSeparator: String): Set<Char> {
        val chars = ('0'..'9').toMutableSet()
        decimalSeparator.firstOrNull()?.let(chars::add)
        groupingSeparator.firstOrNull()?.let(chars::add)
        return chars
    }

    /** Scans left from [caret] to the first character of the current number. */
    private fun findOperandStart(input: String, caret: Int, numberChars: Set<Char>): Int {
        var start = caret
        while (start > 0 && input[start - 1] in numberChars) {
            start -= 1
        }
        return start
    }

    /** True when the character before [operandStart] is a unary minus. */
    private fun hasUnaryMinusBefore(input: String, operandStart: Int): Boolean {
        if (operandStart <= 0 || input[operandStart - 1] != '-') return false
        return operandStart - 1 == 0 || input[operandStart - 2] in UNARY_CONTEXT
    }

    /** Removes the unary minus sitting in front of the operand. */
    private fun removeMinus(input: String, operandStart: Int, caret: Int): SignToggleResult {
        val text = input.removeRange(operandStart - 1, operandStart)
        val newCaret = if (caret >= operandStart) caret - 1 else caret
        return SignToggleResult(text, newCaret.coerceIn(0, text.length))
    }

    /** Inserts a unary minus in front of the operand. */
    private fun insertMinus(input: String, operandStart: Int, caret: Int): SignToggleResult {
        val text = input.substring(0, operandStart) + "-" + input.substring(operandStart)
        val newCaret = if (caret >= operandStart) caret + 1 else caret
        return SignToggleResult(text, newCaret.coerceIn(0, text.length))
    }
}
