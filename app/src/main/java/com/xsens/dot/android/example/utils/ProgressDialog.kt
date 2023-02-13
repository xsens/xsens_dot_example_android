package com.xsens.dot.android.example.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.xsens.dot.android.example.R

class ProgressDialog(private val activity: Activity) {

    private val mAlertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(activity)
    private lateinit var mAlertDialog: AlertDialog
    private lateinit var txtTitle: TextView
    private lateinit var txtProgress: TextView
    private lateinit var btnStop: Button

    fun createDialog() {
        val inflater: LayoutInflater = activity.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_export_progress, null)
        txtTitle = dialogView.findViewById(R.id.txtTitle)
        txtProgress = dialogView.findViewById(R.id.txtProgress)
        btnStop = dialogView.findViewById(R.id.btnStop)

        txtTitle.text = "Export Files"
        txtProgress.text = "Please wait"
        mAlertDialogBuilder.let {
            it.setView(dialogView)
            it.create()
            it.setCancelable(false)
        }

        mAlertDialog = mAlertDialogBuilder.create()
    }

    fun show() {
        mAlertDialog.show()
    }

    fun setStopButtonListener(listener: () -> Unit) {
        btnStop.setOnClickListener {
            listener()
            dismiss()
        }
    }

    fun updateProgress(progress: Int, total: Int) {
        txtProgress.text = "$progress/$total"
    }

    fun dismiss() {
        if (mAlertDialog.isShowing) {
            mAlertDialog.dismiss()
        }
    }
}