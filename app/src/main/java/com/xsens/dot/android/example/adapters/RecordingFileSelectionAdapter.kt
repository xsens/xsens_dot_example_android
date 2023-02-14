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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.R
import java.util.*

class RecordingFileSelectionAdapter(
    list: ArrayList<XsRecordingFileInfo>,
    checkedList: ArrayList<XsRecordingFileInfo>,
    callback: RecordingFileSelectionCallback
) : RecyclerView.Adapter<RecordingFileSelectionAdapter.ConnectedDevicesViewHolder>() {

    private val mConnectedDeviceList: ArrayList<XsRecordingFileInfo> = list
    internal val mCheckedDeviceList: ArrayList<XsRecordingFileInfo> = checkedList
    private var mCallback: RecordingFileSelectionCallback = callback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedDevicesViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_recording_file_selection, parent, false)
        return ConnectedDevicesViewHolder(v)
    }

    override fun onBindViewHolder(holder: ConnectedDevicesViewHolder, position: Int) {
        val info = mConnectedDeviceList[position]
        val fileName = info.fileName
        val size = info.size

        holder.checkBox.isChecked = mCheckedDeviceList.contains(info)

        holder.checkBox.setOnClickListener { v ->
            val isChecked = (v as CheckBox).isChecked
            if (isChecked) {
                mCheckedDeviceList.add(info)
            } else {
                mCheckedDeviceList.remove(info)
            }

            mCallback.onFileSelectionUpdate(mCheckedDeviceList.size)
        }

        holder.fileName.text = fileName
        holder.size.text = formatFileSize(size)
    }

    override fun getItemCount(): Int {
        return mConnectedDeviceList.size
    }

    private fun formatFileSize(size: Int): String {
        return when {
            size < 1024 -> {
                "$size B"
            }

            size < 1024 * 1024 -> {
                "${(size / 1024)} KB"
            }

            else -> {
                "${(size / (1024 * 1024))} MB"
            }
        }
    }

    class ConnectedDevicesViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val fileName: TextView = itemView.findViewById(R.id.textView_file_name)
        val size: TextView = itemView.findViewById(R.id.textView_file_size)
    }

    interface RecordingFileSelectionCallback {
        fun onFileSelectionUpdate(selectedCount: Int)
    }
}