package com.orange.obd.test.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.ViewGroup
import com.orange.obd.test.databinding.DialogAddCommandBinding


object DialogController {
    fun showHint(
        activity: Activity,
        viewInit: (DialogAddCommandBinding, AlertDialog) -> Unit,
        dismiss: () -> Unit = {})
    {
        val dialogView = DialogAddCommandBinding.inflate(activity.layoutInflater)

        val alertDialog = AlertDialog.Builder(activity)
            .setView(dialogView.root)
            .setCancelable(true)
            .show()

        viewInit(dialogView, alertDialog)
        val window = alertDialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        alertDialog.setOnDismissListener { dismiss() }
    }
}