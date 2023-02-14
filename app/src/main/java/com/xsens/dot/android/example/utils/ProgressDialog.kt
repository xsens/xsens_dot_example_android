/*
 * Copyright (c) 2003-2020 Movella Technologies B.V. or subsidiaries worldwide.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *   1.      Redistributions of source code must retain the above copyright notice,
 *            this list of conditions, and the following disclaimer.
 *
 *   2.      Redistributions in binary form must reproduce the above copyright notice,
 *            this list of conditions, and the following disclaimer in the documentation
 *            and/or other materials provided with the distribution.
 *
 *   3.      Neither the names of the copyright holders nor the names of their contributors
 *            may be used to endorse or promote products derived from this software without
 *            specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *   EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *   THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *   SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 *   OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *   HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR
 *   TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.THE LAWS OF THE NETHERLANDS
 *   SHALL BE EXCLUSIVELY APPLICABLE AND ANY DISPUTES SHALL BE FINALLY SETTLED UNDER THE RULES
 *   OF ARBITRATION OF THE INTERNATIONAL CHAMBER OF COMMERCE IN THE HAGUE BY ONE OR MORE
 *   ARBITRATORS APPOINTED IN ACCORDANCE WITH SAID RULES.
 */

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