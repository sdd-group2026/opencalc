package com.darkempire78.opencalculator.dialogs

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.calculator.ConversionResult
import com.darkempire78.opencalculator.calculator.ConvertibleUnit
import com.darkempire78.opencalculator.calculator.UnitCatalog
import com.darkempire78.opencalculator.calculator.UnitCategory
import com.darkempire78.opencalculator.calculator.UnitConverter

/**
 * Menu-launched unit converter dialog (same lightweight pattern as
 * [DonationDialog] — no Activity, no manifest entry). The dialog only wires
 * views together; all conversion maths lives in [UnitConverter].
 */
class UnitConverterDialog(
    private val context: Context,
    private val layoutInflater: LayoutInflater
) {

    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var input: EditText
    private lateinit var output: TextView
    private var category: UnitCategory = UnitCatalog.categories().first()
    private var units: List<ConvertibleUnit> = emptyList()

    /** Builds and shows the converter dialog. */
    fun open() {
        val view = layoutInflater.inflate(R.layout.dialog_unit_converter, null)
        bindViews(view)
        setupCategorySpinner(view.findViewById(R.id.converterCategorySpinner))
        attachRecomputeListeners()
        populateUnitSpinners()
        AlertDialog.Builder(context)
            .setView(view)
            .setTitle(R.string.menu_unit_converter)
            .setPositiveButton(R.string.unit_converter_close) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun bindViews(view: View) {
        fromSpinner = view.findViewById(R.id.converterFromSpinner)
        toSpinner = view.findViewById(R.id.converterToSpinner)
        input = view.findViewById(R.id.converterInput)
        output = view.findViewById(R.id.converterOutput)
    }

    private fun setupCategorySpinner(spinner: Spinner) {
        spinner.adapter = adapterOf(UnitCatalog.categories().map(::categoryLabel))
        spinner.onItemSelectedListener = onSelected { position ->
            category = UnitCatalog.categories()[position]
            populateUnitSpinners()
        }
    }

    private fun populateUnitSpinners() {
        units = UnitCatalog.unitsFor(category)
        val symbols = units.map { it.symbol }
        fromSpinner.adapter = adapterOf(symbols)
        toSpinner.adapter = adapterOf(symbols)
        if (units.size > 1) toSpinner.setSelection(1)
        recompute()
    }

    private fun attachRecomputeListeners() {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = recompute()
            override fun afterTextChanged(s: Editable?) {}
        })
        val listener = onSelected { recompute() }
        fromSpinner.onItemSelectedListener = listener
        toSpinner.onItemSelectedListener = listener
    }

    private fun recompute() {
        if (units.isEmpty()) return
        val from = units[fromSpinner.selectedItemPosition]
        val to = units[toSpinner.selectedItemPosition]
        output.text = when (val result = UnitConverter.convert(input.text.toString(), category, from, to)) {
            is ConversionResult.Success -> result.value.toPlainString()
            ConversionResult.InvalidNumber -> ""
        }
    }

    private fun adapterOf(items: List<String>): ArrayAdapter<String> =
        ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun onSelected(action: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action(position)
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun categoryLabel(category: UnitCategory): String = when (category) {
        UnitCategory.LENGTH -> context.getString(R.string.category_length)
        UnitCategory.MASS -> context.getString(R.string.category_mass)
        UnitCategory.VOLUME -> context.getString(R.string.category_volume)
        UnitCategory.AREA -> context.getString(R.string.category_area)
        UnitCategory.SPEED -> context.getString(R.string.category_speed)
        UnitCategory.TIME -> context.getString(R.string.category_time)
        UnitCategory.DATA -> context.getString(R.string.category_data)
        UnitCategory.TEMPERATURE -> context.getString(R.string.category_temperature)
    }
}
