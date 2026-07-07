package com.darkempire78.opencalculator.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.darkempire78.opencalculator.R

/**
 * Menu-launched dialog that shows the order-of-operations trace for the current
 * expression (same lightweight pattern as [DonationDialog] — no Activity, no
 * manifest entry). All maths is done by the pure StepEvaluator before this is
 * built; the dialog only renders the supplied [steps].
 *
 * @param steps the reduction lines, an empty list for a bare number, or null when
 *   the expression isn't supported by the tracer.
 */
class StepsDialog(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val steps: List<String>?
) {

    /** Builds and shows the steps dialog. */
    fun open() {
        val view = layoutInflater.inflate(R.layout.dialog_steps, null)
        view.findViewById<TextView>(R.id.stepsText).text = renderText()
        AlertDialog.Builder(context)
            .setView(view)
            .setTitle(R.string.menu_steps)
            .setPositiveButton(R.string.steps_close) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun renderText(): String = when {
        steps == null -> context.getString(R.string.steps_unsupported)
        steps.isEmpty() -> context.getString(R.string.steps_none)
        else -> steps.joinToString("\n") { "→ $it" }
    }
}
