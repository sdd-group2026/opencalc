package com.darkempire78.opencalculator.calculator

/** A user-defined named value that can be reused inside expressions. */
data class Variable(val name: String, val value: String)

/** Outcome of expanding variable names within an expression. */
sealed class VariableResolveResult {
    /** All names resolved; [expression] is ready for the parser. */
    data class Success(val expression: String) : VariableResolveResult()

    /** [name] is an alphabetic token that is neither a built-in nor a variable. */
    data class UnknownVariable(val name: String) : VariableResolveResult()
}

/**
 * Pure resolver that substitutes user-defined variable names with their values
 * before evaluation, keeping the substitution logic out of the UI layer.
 *
 * Substitution is whole-word only, so a variable name can never match inside a
 * built-in function token (e.g. a variable "co" never matches inside "cos").
 * Any leftover alphabetic token that is not a known built-in is reported as an
 * [VariableResolveResult.UnknownVariable] so the caller can surface an error.
 */
object VariableResolver {

    /** Alphabetic tokens the engine already understands (functions/constants). */
    val RESERVED: Set<String> = setOf(
        "sin", "cos", "tan", "sinh", "cosh", "tanh", "ln", "log", "exp", "e", "E"
    )

    /** Expands every [variables] name in [expression] to its parenthesised value. */
    fun resolve(expression: String, variables: List<Variable>): VariableResolveResult {
        var expanded = expression
        for (variable in variables) {
            expanded = substitute(expanded, variable)
        }
        val unknown = firstUnknownIdentifier(expanded)
        return if (unknown == null) {
            VariableResolveResult.Success(expanded)
        } else {
            VariableResolveResult.UnknownVariable(unknown)
        }
    }

    /** Replaces whole-word occurrences of [variable]'s name with its value. */
    private fun substitute(expression: String, variable: Variable): String {
        val pattern = Regex("(?<![A-Za-z0-9])${Regex.escape(variable.name)}(?![A-Za-z0-9])")
        return expression.replace(pattern, "(${variable.value})")
    }

    /** First maximal alphabetic token that is not a reserved built-in, or null. */
    private fun firstUnknownIdentifier(expression: String): String? =
        Regex("[A-Za-z]+").findAll(expression)
            .map { it.value }
            .firstOrNull { it !in RESERVED }
}
