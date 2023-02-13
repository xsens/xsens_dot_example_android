package com.xsens.dot.android.example.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.interfaces.FileSelectionCallback
import com.xsens.dot.android.example.views.RecordingData

class ExportAdapter(private var mDataList: ArrayList<RecordingData>, private var fileSelectionCallback: FileSelectionCallback) :
    RecyclerView.Adapter<ExportAdapter.ExportViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_export, parent, false)
        return ExportViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: ExportViewHolder, position: Int) {
        val data = mDataList[position]
        holder.clItem.setOnClickListener {
            fileSelectionCallback.onFileSelectionClick(data.device.address)
        }
        holder.txtDeviceName.text = data.device.name
        holder.txtDeviceAddress.text = data.device.address
        holder.txtFileCount.text = if (data.recordingFileInfoList.isEmpty()) "0 Files" else "${data.recordingFileInfoList.size} Files"
    }

    class ExportViewHolder(rootView: View) : ViewHolder(rootView) {
        var clItem: ConstraintLayout = rootView.findViewById(R.id.clItem)
        var txtDeviceName: TextView = rootView.findViewById(R.id.txtDeviceName)
        var txtDeviceAddress: TextView = rootView.findViewById(R.id.txtDeviceAddress)
        var txtFileCount: TextView = rootView.findViewById(R.id.txtFileCount)
    }
}