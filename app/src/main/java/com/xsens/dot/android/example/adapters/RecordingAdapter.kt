package com.xsens.dot.android.example.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.views.RecordingData

class RecordingAdapter(private var mDataList: ArrayList<RecordingData>) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val data = mDataList[position]
        holder.txtDeviceName.text = data.device.name
        holder.txtDeviceAddress.text = data.device.address
        holder.txtProgress.text = if (data.isRecording) "Recording" else "IDLE"
    }

    class RecordingViewHolder(private var rootView: View) : ViewHolder(rootView) {
        var txtDeviceName: TextView = rootView.findViewById(R.id.txtDeviceName)
        var txtDeviceAddress: TextView = rootView.findViewById(R.id.txtDeviceAddress)
        var txtProgress: TextView = rootView.findViewById(R.id.txtProgress)
    }
}