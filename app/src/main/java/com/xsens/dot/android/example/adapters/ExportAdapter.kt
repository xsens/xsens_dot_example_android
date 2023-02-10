package com.xsens.dot.android.example.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.views.RecordingData

class ExportAdapter(private var mDataList: ArrayList<RecordingData>) : RecyclerView.Adapter<ExportAdapter.ExportViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return ExportViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: ExportViewHolder, position: Int) {
        val data = mDataList[position]
        holder.txtDeviceName.text = data.device.name
        holder.txtDeviceAddress.text = data.device.address
        holder.txtFileCount.text = if (data.fileList.isNullOrEmpty()) "0 Files" else "${data.fileList!!.size} Files"
    }

    class ExportViewHolder(var rootView: View) : ViewHolder(rootView) {
        var txtDeviceName: TextView = rootView.findViewById(R.id.txtDeviceName)
        var txtDeviceAddress: TextView = rootView.findViewById(R.id.txtDeviceAddress)
        var txtFileCount: TextView = rootView.findViewById(R.id.txtProgress)
    }
}