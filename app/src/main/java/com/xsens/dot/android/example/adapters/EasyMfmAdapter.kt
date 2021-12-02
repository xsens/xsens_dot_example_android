//  Copyright (c) 2003-2021 Xsens Technologies B.V. or subsidiaries worldwide.
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification,
//  are permitted provided that the following conditions are met:
//
//  1.      Redistributions of source code must retain the above copyright notice,
//           this list of conditions, and the following disclaimer.
//
//  2.      Redistributions in binary form must reproduce the above copyright notice,
//           this list of conditions, and the following disclaimer in the documentation
//           and/or other materials provided with the distribution.
//
//  3.      Neither the names of the copyright holders nor the names of their contributors
//           may be used to endorse or promote products derived from this software without
//           specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
//  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//  THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
//  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
//  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR
//  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.THE LAWS OF THE NETHERLANDS
//  SHALL BE EXCLUSIVELY APPLICABLE AND ANY DISPUTES SHALL BE FINALLY SETTLED UNDER THE RULES
//  OF ARBITRATION OF THE INTERNATIONAL CHAMBER OF COMMERCE IN THE HAGUE BY ONE OR MORE
//  ARBITRATORS APPOINTED IN ACCORDANCE WITH SAID RULES.
//

package com.xsens.dot.android.example.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.models.MfmInfo
import com.xsens.dot.android.example.views.EasyMfmFragment.MfmStatus.*
import java.util.*

class EasyMfmAdapter(context: Context,
                     list: ArrayList<MfmInfo>,
                     listener: ReWriteClickListener)
    : RecyclerView.Adapter<EasyMfmAdapter.ConnectedDevicesViewHolder>() {

    private val mContext: Context = context
    private val mInfos: ArrayList<MfmInfo> = list
    private val mListener: ReWriteClickListener = listener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedDevicesViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_easy_mfm, parent, false)
        return ConnectedDevicesViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ConnectedDevicesViewHolder, position: Int) {
        val device = mInfos[position].dotDevice
        val status = mInfos[position].status
        val percentage = mInfos[position].percentage

        holder.name.text = if (device.tag.isEmpty()) device.name else device.tag
        holder.address.text = device.address
        holder.percentage.text = "$percentage%"
        holder.progress.progress = percentage
        holder.rewrite.setOnClickListener { mListener.reWrite(device.address) }

        when (status) {
            STARTING.ordinal,
            STARTED.ordinal -> {
                // Wait for percentage updating.
                holder.percentage.visibility = View.VISIBLE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.visibility = View.GONE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            PROCESSING.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.VISIBLE
                holder.processing.visibility = View.VISIBLE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.visibility = View.GONE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            WRITING.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.setTextColor(mContext.getColor(android.R.color.white))
                holder.result.text = mContext.getString(R.string.mfm_write)
                holder.result.visibility = View.VISIBLE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            FINISHED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.done.visibility = View.VISIBLE
                holder.doneLabel.visibility = View.VISIBLE
                holder.result.visibility = View.GONE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            DISCONNECTED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.result.setTextColor(mContext.getColor(android.R.color.holo_red_light))
                holder.result.text = mContext.getString(R.string.disconnected)
                holder.result.visibility = View.VISIBLE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            STOPPED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.setTextColor(mContext.getColor(android.R.color.white))
                holder.result.text = mContext.getString(R.string.mfm_stopped)
                holder.result.visibility = View.VISIBLE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            START_FAILED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.result.setTextColor(mContext.getColor(android.R.color.holo_red_light))
                holder.result.text = mContext.getString(R.string.mfm_start_fail)
                holder.result.visibility = View.VISIBLE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            PROCESS_FAILED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.setTextColor(mContext.getColor(android.R.color.holo_red_light))
                holder.result.text = mContext.getString(R.string.mfm_process_fail)
                holder.result.visibility = View.VISIBLE
                holder.rewriteLabel.visibility = View.GONE
                holder.rewrite.visibility = View.GONE
            }

            WRITE_FAILED.ordinal -> {
                holder.percentage.visibility = View.GONE
                holder.processingLabel.visibility = View.GONE
                holder.processing.visibility = View.GONE
                holder.doneLabel.visibility = View.GONE
                holder.done.visibility = View.GONE
                holder.result.visibility = View.GONE
                holder.rewriteLabel.visibility = View.VISIBLE
                holder.rewrite.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int {
        return mInfos.size
    }

    internal fun updateStatus(address: String, status: Int) {
        for (info in mInfos) {
            val device = info.dotDevice
            if (device.address == address) {

                info.status = status
                break
            }
        }
    }

    internal fun getStatus(address: String): Int? {
        for (info in mInfos) {
            val device = info.dotDevice
            if (device.address == address) return info.status
        }

        return null
    }

    internal fun updatePercentage(address: String, percentage: Int) {
        for (info in mInfos) {
            val device = info.dotDevice
            if (device.address == address) {
                info.percentage = percentage
                break
            }
        }
    }

    internal fun updatePosition(address: String): Int {
        var position = 0

        for (info in mInfos) {
            val device = info.dotDevice
            if (device.address == address) {
                notifyItemChanged(position)
                break
            }

            position++
        }

        return position
    }

    class ConnectedDevicesViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val address: TextView = itemView.findViewById(R.id.address)
        val percentage: TextView = itemView.findViewById(R.id.percentage)
        val result: TextView = itemView.findViewById(R.id.result)
        val rewriteLabel: TextView = itemView.findViewById(R.id.rewrite_label)
        val rewrite: TextView = itemView.findViewById(R.id.rewrite)
        val processingLabel: TextView = itemView.findViewById(R.id.processing_label)
        val processing: ProgressBar = itemView.findViewById(R.id.processing)
        val progress: ProgressBar = itemView.findViewById(R.id.progress)
        val doneLabel: TextView = itemView.findViewById(R.id.done_label)
        val done: ImageView = itemView.findViewById(R.id.done)
    }

    interface ReWriteClickListener {
        fun reWrite(address: String)
    }
}