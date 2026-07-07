package com.darkempire78.opencalculator.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.calculator.Variable
import com.darkempire78.opencalculator.calculator.VariableResolver

/**
 * Menu-launched dialog to manage persistent named variables and insert them into
 * the calculator input. Persistence goes through MyPreferences/Gson; this class
 * only does view wiring and delegates the maths/validation elsewhere.
 */
class VariablesDialog(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val onInsert: (String) -> Unit
) {

    private val preferences = MyPreferences(context)
    private lateinit var list: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var valueInput: EditText
    private var dialog: AlertDialog? = null

    /** Builds and shows the variables dialog. */
    fun open() {
        val view = layoutInflater.inflate(R.layout.dialog_variables, null)
        list = view.findViewById(R.id.variablesList)
        nameInput = view.findViewById(R.id.variableName)
        valueInput = view.findViewById(R.id.variableValue)
        view.findViewById<Button>(R.id.variableAddButton).setOnClickListener { addVariable() }
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(R.string.menu_variables)
            .setPositiveButton(R.string.variables_close) { d, _ -> d.dismiss() }
            .create()
        renderList()
        dialog?.show()
    }

    private fun renderList() {
        list.removeAllViews()
        preferences.getVariables().forEach { list.addView(buildRow(it)) }
    }

    private fun buildRow(variable: Variable): View {
        val row = layoutInflater.inflate(R.layout.item_variable, list, false)
        val chip = row.findViewById<Button>(R.id.variableChip)
        chip.text = context.getString(R.string.variables_chip, variable.name, variable.value)
        chip.setOnClickListener { onInsert(variable.name); dialog?.dismiss() }
        row.findViewById<Button>(R.id.variableDelete).setOnClickListener {
            preferences.saveVariables(preferences.getVariables().filterNot { it.name == variable.name })
            renderList()
        }
        return row
    }

    private fun addVariable() {
        val name = nameInput.text.toString().trim()
        val value = valueInput.text.toString().trim()
        if (!isValidName(name)) {
            nameInput.error = context.getString(R.string.variables_invalid_name)
            return
        }
        if (value.toBigDecimalOrNull() == null) {
            valueInput.error = context.getString(R.string.variables_invalid_value)
            return
        }
        val updated = preferences.getVariables().filterNot { it.name == name }.toMutableList()
        updated.add(Variable(name, value))
        preferences.saveVariables(updated)
        nameInput.text.clear()
        valueInput.text.clear()
        renderList()
    }

    /** A valid name is alphabetic-leading, alphanumeric, and not a reserved token. */
    private fun isValidName(name: String): Boolean =
        name.matches(Regex("[A-Za-z][A-Za-z0-9]*")) && name !in VariableResolver.RESERVED
}
