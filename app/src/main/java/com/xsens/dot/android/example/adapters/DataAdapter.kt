//  Copyright (c) 2003-2020 Xsens Technologies B.V. or subsidiaries worldwide.
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

import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.adapters.DataAdapter.DataViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.xsens.dot.android.example.R
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.xsens.dot.android.example.adapters.DataAdapter
import com.xsens.dot.android.sdk.events.XsensDotData
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.widget.TextView
import java.util.ArrayList
import java.util.HashMap

/**
 * A view adapter for item view to present data.
 */
class DataAdapter
/**
 * Default constructor.
 *
 * @param context  The application context
 * @param dataList A list contains tag and data
 */(// The application context
    private val mContext: Context, // Put all data from sensors into one list
    private val mDataList: ArrayList<HashMap<String, Any?>>
) : RecyclerView.Adapter<DataViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return DataViewHolder(itemView)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val tag = mDataList!![position][KEY_TAG] as String?
        val xsData = mDataList[position][KEY_DATA] as XsensDotData?
        holder.sensorName.text = tag
        val eulerAngles = xsData!!.euler
        val eulerAnglesStr = String.format("%.6f", eulerAngles[0]) + ", " + String.format("%.6f", eulerAngles[1]) + ", " + String.format("%.6f", eulerAngles[2])
        holder.orientationData.text = eulerAnglesStr
        val freeAcc = xsData.freeAcc
        val freeAccStr = String.format("%.6f", freeAcc[0]) + ", " + String.format("%.6f", freeAcc[1]) + ", " + String.format("%.6f", freeAcc[2])
        holder.freeAccData.text = freeAccStr
    }

    override fun getItemCount(): Int {
        return mDataList?.size ?: 0
    }

    /**
     * A Customized class for ViewHolder of RecyclerView.
     */
    class DataViewHolder(var rootView: View) : ViewHolder(rootView) {
        var sensorName: TextView
        var orientationData: TextView
        var freeAccData: TextView

        init {
            sensorName = rootView.findViewById(R.id.sensor_name)
            orientationData = rootView.findViewById(R.id.orientation_data)
            freeAccData = rootView.findViewById(R.id.free_acc_data)
        }
    }

    companion object {
        private val TAG = DataAdapter::class.java.simpleName

        // The keys of HashMap
        const val KEY_ADDRESS = "address"
        const val KEY_TAG = "tag"
        const val KEY_DATA = "data"
    }
}