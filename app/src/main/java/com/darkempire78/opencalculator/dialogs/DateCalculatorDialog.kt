package com.darkempire78.opencalculator.dialogs

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.calculator.CalendarDate
import com.darkempire78.opencalculator.calculator.DateCalculator
import com.darkempire78.opencalculator.calculator.DateDifferenceResult
import java.util.Calendar

/**
 * Menu-launched date/age calculator dialog (same lightweight pattern as
 * [DonationDialog] — no Activity, no manifest entry). The dialog only wires the
 * date pickers and renders the result; all date maths lives in [DateCalculator].
 */
class DateCalculatorDialog(
    private val context: Context,
    private val layoutInflater: LayoutInflater
) {

    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var result: TextView
    private var fromDate = today()
    private var toDate = today()

    /** Builds and shows the date calculator dialog. */
    fun open() {
        val view = layoutInflater.inflate(R.layout.dialog_date_calculator, null)
        fromButton = view.findViewById(R.id.dateCalcFromButton)
        toButton = view.findViewById(R.id.dateCalcToButton)
        result = view.findViewById(R.id.dateCalcResult)
        fromButton.setOnClickListener { pickDate(fromDate) { fromDate = it; refresh() } }
        toButton.setOnClickListener { pickDate(toDate) { toDate = it; refresh() } }
        refresh()
        AlertDialog.Builder(context)
            .setView(view)
            .setTitle(R.string.menu_date_calculator)
            .setPositiveButton(R.string.date_calc_close) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun today(): CalendarDate {
        val now = Calendar.getInstance()
        return CalendarDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
    }

    private fun pickDate(current: CalendarDate, onPicked: (CalendarDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onPicked(CalendarDate(year, month + 1, day)) },
            current.year, current.month - 1, current.day
        ).show()
    }

    private fun refresh() {
        fromButton.text = format(fromDate)
        toButton.text = format(toDate)
        result.text = when (val diff = DateCalculator.difference(fromDate, toDate)) {
            is DateDifferenceResult.Success ->
                context.getString(R.string.date_calc_result, diff.years, diff.months, diff.days, diff.totalDays)
            DateDifferenceResult.InvalidDate -> ""
        }
    }

    private fun format(date: CalendarDate): String =
        "%04d-%02d-%02d".format(date.year, date.month, date.day)
}
