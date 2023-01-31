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

import android.Manifest
import com.xsens.dot.android.example.interfaces.SensorClickInterface
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.adapters.ScanAdapter.ScanViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.xsens.dot.android.example.R
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.xsens.dot.android.example.adapters.ScanAdapter
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.view.View
import com.xsens.dot.android.sdk.models.XsensDotDevice
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.widget.TextView
import java.util.ArrayList
import java.util.HashMap

/**
 * A view adapter for item view of scanned BLE device.
 */
class ScanAdapter
/**
 * Default constructor.
 *
 * @param context           The application context
 * @param scannedSensorList The scanned devices list
 */(// The application context
    private val mContext: Context, // Put all scanned devices into one list
    private val mSensorList: ArrayList<HashMap<String, Any>>?
) : RecyclerView.Adapter<ScanViewHolder>() {
    // Send the click event to fragment
    private var mListener: SensorClickInterface? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return ScanViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, index: Int) {
        val position = holder.adapterPosition
        val device = mSensorList!![position][KEY_DEVICE] as BluetoothDevice?
        if (device != null) {
            val tag = mSensorList[position][KEY_TAG] as String?
            if (tag != null) holder.sensorName.text = if (tag.isEmpty()) device.name else tag else holder.sensorName.text = device.name
            val batteryPercentage = mSensorList[position][KEY_BATTERY_PERCENTAGE] as Int
            val batteryState = mSensorList[position][KEY_BATTERY_STATE] as Int
            var batteryStr = ""
            if (batteryPercentage != -1) batteryStr = "$batteryPercentage% "
            if (batteryState == XsensDotDevice.BATT_STATE_CHARGING) batteryStr = batteryStr + mContext.getString(R.string.batt_state_charging)
            holder.sensorBattery.text = batteryStr
            holder.sensorMacAddress.text = device.address
        }
        val state = mSensorList[position][KEY_CONNECTION_STATE] as Int
        when (state) {
            XsensDotDevice.CONN_STATE_DISCONNECTED -> {
                holder.sensorState.visibility = View.GONE
                holder.sensorState.text = mContext.resources.getString(R.string.disconnected)
            }
            XsensDotDevice.CONN_STATE_CONNECTING -> {
                holder.sensorState.visibility = View.VISIBLE
                holder.sensorState.text = mContext.resources.getString(R.string.connecting)
            }
            XsensDotDevice.CONN_STATE_CONNECTED -> {
                holder.sensorState.visibility = View.VISIBLE
                holder.sensorState.text = mContext.resources.getString(R.string.connected)
            }
            XsensDotDevice.CONN_STATE_RECONNECTING -> {
                holder.sensorState.visibility = View.VISIBLE
                holder.sensorState.text = mContext.resources.getString(R.string.reconnecting)
            }
        }
        holder.rootView.setOnClickListener(View.OnClickListener { v -> // Notify the position of click event to fragment.
            if (mListener != null) mListener!!.onSensorClick(v, position)
        })
    }

    override fun getItemCount(): Int {
        return mSensorList?.size ?: 0
    }

    /**
     * Get the Bluetooth device.
     *
     * @param position The position of item view
     * @return The scanned Bluetooth device
     */
    fun getDevice(position: Int): BluetoothDevice? {
        return if (mSensorList != null) {
            mSensorList.get(position).get(KEY_DEVICE) as BluetoothDevice?
        } else null
    }

    /**
     * Get the connection state of device.
     *
     * @param position The position of item view
     * @return The connection state
     */
    fun getConnectionState(position: Int): Int {
        return if (mSensorList != null) {
            mSensorList.get(position).get(KEY_CONNECTION_STATE) as Int
        } else XsensDotDevice.CONN_STATE_DISCONNECTED
    }

    /**
     * Update the connection state to list.
     *
     * @param position The position of item view
     * @param state    The connection state
     */
    fun updateConnectionState(position: Int, state: Int) {
        if (mSensorList != null) {
            mSensorList.get(position)[KEY_CONNECTION_STATE] = state
        }
    }

    /**
     * Update tag name to the list.
     *
     * @param address The mac address of device
     * @param tag     The device tag
     */
    fun updateTag(address: String, tag: String) {
        if (mSensorList != null) {
            for (map: HashMap<String, Any> in mSensorList) {
                val device = map[KEY_DEVICE] as BluetoothDevice?
                if (device != null) {
                    val _address = device.address
                    if ((_address == address)) {
                        map[KEY_TAG] = tag
                    }
                }
            }
        }
    }

    /**
     * Update battery information to the list.
     *
     * @param address    The mac address of device
     * @param state      This state can be one of BATT_STATE_NOT_CHARGING or BATT_STATE_CHARGING
     * @param percentage The range of battery level is 0 to 100
     */
    fun updateBattery(address: String, state: Int, percentage: Int) {
        if (mSensorList != null) {
            for (map: HashMap<String, Any> in mSensorList) {
                val device = map[KEY_DEVICE] as BluetoothDevice?
                if (device != null) {
                    val _address = device.address
                    if ((_address == address)) {
                        map[KEY_BATTERY_STATE] = state
                        map[KEY_BATTERY_PERCENTAGE] = percentage
                    }
                }
            }
        }
    }

    /**
     * Initialize click listener of item view.
     *
     * @param listener The fragment which implemented SensorClickInterface
     */
    fun setSensorClickListener(listener: SensorClickInterface?) {
        mListener = listener
    }

    /**
     * A Customized class for ViewHolder of RecyclerView.
     */
    class ScanViewHolder(var rootView: View) : ViewHolder(rootView) {
        var sensorName: TextView
        var sensorMacAddress: TextView
        var sensorBattery: TextView
        var sensorState: TextView

        init {
            sensorName = rootView.findViewById(R.id.sensor_name)
            sensorMacAddress = rootView.findViewById(R.id.sensor_mac_address)
            sensorBattery = rootView.findViewById(R.id.sensor_battery)
            sensorState = rootView.findViewById(R.id.sensor_state)
        }
    }

    companion object {
        private val TAG = ScanAdapter::class.java.simpleName

        // The keys of HashMap
        @JvmField
        val KEY_DEVICE = "device"
        @JvmField
        val KEY_CONNECTION_STATE = "state"
        @JvmField
        val KEY_TAG = "tag"
        @JvmField
        val KEY_BATTERY_STATE = "battery_state"
        @JvmField
        val KEY_BATTERY_PERCENTAGE = "battery_percentage"
    }
}