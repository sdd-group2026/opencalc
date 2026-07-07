package com.darkempire78.opencalculator.calculator

import java.math.BigDecimal
import java.math.RoundingMode

/** Outcome of a reciprocal (1/x) operation. */
sealed class ReciprocalResult {
    /** 1/x succeeded with [value]. */
    data class Success(val value: BigDecimal) : ReciprocalResult()

    /** x was zero, so 1/x is undefined. */
    object DivisionByZero : ReciprocalResult()
}

/**
 * Pure reciprocal (1/x) of a value.
 *
 * Operates on an already-evaluated result (the same input the memory keys use),
 * keeping the maths out of the Activity layer. Returns a sealed result so the
 * division-by-zero case is explicit, mirroring [CalculationResult].
 */
object Reciprocal {

    private const val DIVISION_SCALE = 12

    /** Computes 1/[value], or [ReciprocalResult.DivisionByZero] when [value] is 0. */
    fun of(value: BigDecimal): ReciprocalResult {
        if (value.signum() == 0) return ReciprocalResult.DivisionByZero
        val result = BigDecimal.ONE.divide(value, DIVISION_SCALE, RoundingMode.HALF_UP)
        return ReciprocalResult.Success(result.stripTrailingZeros())
    }
}
