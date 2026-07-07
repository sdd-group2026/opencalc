package com.darkempire78.opencalculator.calculator

import java.math.BigDecimal

/**
 * Outcome of recalling the calculator memory.
 *
 * Modelled as a sealed type so the "nothing stored yet" case is explicit and
 * the caller is forced to handle it, mirroring the [CalculationResult]
 * convention used by the evaluator.
 */
sealed class MemoryResult {
    /** Memory holds [amount]. */
    data class Value(val amount: BigDecimal) : MemoryResult()

    /** Memory is empty (never set, or cleared). */
    object Empty : MemoryResult()
}

/**
 * Pure arithmetic backing the memory keys (MC / MR / M+ / M− / MS).
 *
 * The persisted value lives in `MyPreferences`; this object only computes the
 * next memory value (or interprets the current one), keeping all numeric logic
 * out of the Activity layer. A `null` [stored] value means memory is empty and
 * is treated as zero for accumulation.
 */
object Memory {

    /** Interprets the [stored] value as a [MemoryResult]. */
    fun recall(stored: BigDecimal?): MemoryResult =
        if (stored == null) MemoryResult.Empty else MemoryResult.Value(stored)

    /** Returns the new memory value after adding [operand] (M+). */
    fun add(stored: BigDecimal?, operand: BigDecimal): BigDecimal =
        (stored ?: BigDecimal.ZERO).add(operand)

    /** Returns the new memory value after subtracting [operand] (M−). */
    fun subtract(stored: BigDecimal?, operand: BigDecimal): BigDecimal =
        (stored ?: BigDecimal.ZERO).subtract(operand)
}
