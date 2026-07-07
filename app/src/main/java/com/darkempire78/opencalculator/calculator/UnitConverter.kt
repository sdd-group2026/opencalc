package com.darkempire78.opencalculator.calculator

import java.math.BigDecimal
import java.math.RoundingMode

/** Physical quantity a [ConvertibleUnit] belongs to. */
enum class UnitCategory { LENGTH, MASS, VOLUME, AREA, SPEED, TIME, DATA, TEMPERATURE }

/**
 * A single unit within a [UnitCategory].
 *
 * @property nameKey stable identifier (e.g. "kilometer"); also drives the
 *   non-linear temperature branching.
 * @property symbol short display label (e.g. "km"); a universal abbreviation.
 * @property factorToBase multiplier converting one unit to the category's base
 *   unit. Ignored for [UnitCategory.TEMPERATURE], which is non-linear.
 */
data class ConvertibleUnit(
    val nameKey: String,
    val symbol: String,
    val factorToBase: BigDecimal
)

/** Outcome of a unit conversion. */
sealed class ConversionResult {
    /** Conversion succeeded with [value]. */
    data class Success(val value: BigDecimal) : ConversionResult()

    /** The supplied amount was not a valid number. */
    object InvalidNumber : ConversionResult()
}

/**
 * Pure, offline unit-conversion engine.
 *
 * Linear categories convert through a per-category base unit via factor ratios;
 * temperature is handled with explicit (non-linear) formulas. All data is local
 * (see [UnitCatalog]); there is no network access or external API.
 */
object UnitConverter {

    private const val DIVISION_SCALE = 12

    /**
     * Converts [amount] (a decimal string using '.') from [from] to [to] within
     * [category]. Returns [ConversionResult.InvalidNumber] when [amount] is not
     * a number.
     */
    fun convert(
        amount: String,
        category: UnitCategory,
        from: ConvertibleUnit,
        to: ConvertibleUnit
    ): ConversionResult {
        val value = amount.toBigDecimalOrNull() ?: return ConversionResult.InvalidNumber
        val result =
            if (category == UnitCategory.TEMPERATURE) convertTemperature(value, from.nameKey, to.nameKey)
            else convertLinear(value, from, to)
        return ConversionResult.Success(result.stripTrailingZeros())
    }

    /** Ratio conversion through the category's base unit. */
    private fun convertLinear(value: BigDecimal, from: ConvertibleUnit, to: ConvertibleUnit): BigDecimal =
        value.multiply(from.factorToBase).divide(to.factorToBase, DIVISION_SCALE, RoundingMode.HALF_UP)

    /** Temperature conversion via Celsius as the pivot. */
    private fun convertTemperature(value: BigDecimal, fromKey: String, toKey: String): BigDecimal =
        fromCelsius(toCelsius(value, fromKey), toKey)

    /** Converts a temperature [value] of unit [key] into Celsius. */
    private fun toCelsius(value: BigDecimal, key: String): BigDecimal = when (key) {
        "fahrenheit" -> value.subtract(BigDecimal(32)).multiply(BigDecimal(5))
            .divide(BigDecimal(9), DIVISION_SCALE, RoundingMode.HALF_UP)
        "kelvin" -> value.subtract(BigDecimal("273.15"))
        else -> value
    }

    /** Converts a Celsius value into the temperature unit [key]. */
    private fun fromCelsius(celsius: BigDecimal, key: String): BigDecimal = when (key) {
        "fahrenheit" -> celsius.multiply(BigDecimal(9))
            .divide(BigDecimal(5), DIVISION_SCALE, RoundingMode.HALF_UP).add(BigDecimal(32))
        "kelvin" -> celsius.add(BigDecimal("273.15"))
        else -> celsius
    }
}

/**
 * Local catalogue of units per category. Factors are compile-time constants —
 * no external data source is consulted.
 */
object UnitCatalog {

    /** All categories, in display order. */
    fun categories(): List<UnitCategory> = UnitCategory.entries

    /** Units belonging to [category], in display order (base unit factor = 1). */
    fun unitsFor(category: UnitCategory): List<ConvertibleUnit> = when (category) {
        UnitCategory.LENGTH -> length()
        UnitCategory.MASS -> mass()
        UnitCategory.VOLUME -> volume()
        UnitCategory.AREA -> area()
        UnitCategory.SPEED -> speed()
        UnitCategory.TIME -> time()
        UnitCategory.DATA -> data()
        UnitCategory.TEMPERATURE -> temperature()
    }

    private fun u(nameKey: String, symbol: String, factor: String) =
        ConvertibleUnit(nameKey, symbol, BigDecimal(factor))

    /** Base: metre. */
    private fun length() = listOf(
        u("kilometer", "km", "1000"), u("meter", "m", "1"),
        u("centimeter", "cm", "0.01"), u("millimeter", "mm", "0.001"),
        u("mile", "mi", "1609.344"), u("yard", "yd", "0.9144"),
        u("foot", "ft", "0.3048"), u("inch", "in", "0.0254")
    )

    /** Base: kilogram. */
    private fun mass() = listOf(
        u("tonne", "t", "1000"), u("kilogram", "kg", "1"),
        u("gram", "g", "0.001"), u("milligram", "mg", "0.000001"),
        u("pound", "lb", "0.45359237"), u("ounce", "oz", "0.028349523125")
    )

    /** Base: litre. */
    private fun volume() = listOf(
        u("cubicMeter", "m³", "1000"), u("liter", "L", "1"),
        u("milliliter", "mL", "0.001"), u("gallonUs", "gal", "3.785411784"),
        u("quartUs", "qt", "0.946352946"), u("pintUs", "pt", "0.473176473"),
        u("fluidOunceUs", "fl oz", "0.0295735295625")
    )

    /** Base: square metre. */
    private fun area() = listOf(
        u("squareKilometer", "km²", "1000000"), u("hectare", "ha", "10000"),
        u("squareMeter", "m²", "1"), u("squareCentimeter", "cm²", "0.0001"),
        u("acre", "ac", "4046.8564224"), u("squareMile", "mi²", "2589988.110336"),
        u("squareFoot", "ft²", "0.09290304"), u("squareInch", "in²", "0.00064516")
    )

    /** Base: metre per second. */
    private fun speed() = listOf(
        u("meterPerSecond", "m/s", "1"),
        u("kilometerPerHour", "km/h", "0.27777777777778"),
        u("milePerHour", "mph", "0.44704"),
        u("knot", "kn", "0.51444444444444"),
        u("footPerSecond", "ft/s", "0.3048")
    )

    /** Base: second. */
    private fun time() = listOf(
        u("millisecond", "ms", "0.001"), u("second", "s", "1"),
        u("minute", "min", "60"), u("hour", "h", "3600"),
        u("day", "d", "86400"), u("week", "wk", "604800")
    )

    /** Base: byte (binary multiples: 1 KB = 1024 B). */
    private fun data() = listOf(
        u("bit", "bit", "0.125"), u("byte", "B", "1"),
        u("kilobyte", "KB", "1024"), u("megabyte", "MB", "1048576"),
        u("gigabyte", "GB", "1073741824"), u("terabyte", "TB", "1099511627776")
    )

    /** Non-linear; factors are placeholders ([UnitConverter] uses formulas). */
    private fun temperature() = listOf(
        u("celsius", "°C", "1"), u("fahrenheit", "°F", "1"), u("kelvin", "K", "1")
    )
}
