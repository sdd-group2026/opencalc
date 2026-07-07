package com.darkempire78.opencalculator.calculator

/** A calendar date in the proleptic Gregorian calendar (month and day are 1-based). */
data class CalendarDate(val year: Int, val month: Int, val day: Int)

/** Outcome of a date-difference computation. */
sealed class DateDifferenceResult {
    /** Difference broken into [years]/[months]/[days] plus the absolute [totalDays]. */
    data class Success(
        val years: Int,
        val months: Int,
        val days: Int,
        val totalDays: Long
    ) : DateDifferenceResult()

    /** One of the supplied dates was not a real calendar date. */
    object InvalidDate : DateDifferenceResult()
}

/**
 * Pure, dependency-free date arithmetic.
 *
 * Avoids java.time so it is safe on every supported API level, and keeps all the
 * calendar maths out of the UI layer. The years/months/days breakdown matches
 * java.time.Period semantics; [totalDays] is the absolute day count.
 */
object DateCalculator {

    /** Absolute difference between [start] and [end] (order-independent). */
    fun difference(start: CalendarDate, end: CalendarDate): DateDifferenceResult {
        if (!isValid(start) || !isValid(end)) return DateDifferenceResult.InvalidDate
        val from = if (toEpochDay(start) <= toEpochDay(end)) start else end
        val to = if (from === start) end else start
        val totalDays = toEpochDay(to) - toEpochDay(from)
        var totalMonths = (to.year * 12 + to.month) - (from.year * 12 + from.month)
        var days = to.day - from.day
        if (days < 0) {
            totalMonths -= 1
            days = (toEpochDay(to) - toEpochDay(addMonths(from, totalMonths))).toInt()
        }
        return DateDifferenceResult.Success(totalMonths / 12, totalMonths % 12, days, totalDays)
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun isValid(date: CalendarDate): Boolean =
        date.month in 1..12 && date.day in 1..daysInMonth(date.year, date.month)

    /** Adds [months] to [date], clamping the day to the target month's length. */
    private fun addMonths(date: CalendarDate, months: Int): CalendarDate {
        val total = date.year * 12 + (date.month - 1) + months
        val year = Math.floorDiv(total, 12)
        val month = Math.floorMod(total, 12) + 1
        return CalendarDate(year, month, minOf(date.day, daysInMonth(year, month)))
    }

    /** Days since 1970-01-01 (proleptic Gregorian); Howard Hinnant's algorithm. */
    private fun toEpochDay(date: CalendarDate): Long {
        val y = if (date.month <= 2) date.year - 1 else date.year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = (y - era * 400).toLong()
        val doy = (153 * ((date.month + 9) % 12) + 2) / 5 + date.day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097 + doe - 719468
    }
}
