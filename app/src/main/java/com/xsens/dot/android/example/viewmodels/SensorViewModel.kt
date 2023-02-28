//  Copyright (c) 2003-2020 Movella Technologies B.V. or subsidiaries worldwide.
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
package com.xsens.dot.android.example.viewmodels

import androidx.lifecycle.ViewModel
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCallback
import com.xsens.dot.android.example.interfaces.BatteryChangedInterface
import com.xsens.dot.android.example.interfaces.DataChangeInterface
import androidx.lifecycle.MutableLiveData
import com.xsens.dot.android.sdk.models.XsensDotDevice
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.xsens.dot.android.example.viewmodels.SensorViewModel
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.models.FilterProfileInfo
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import java.util.ArrayList

/**
 * A view model class for notifying data to views and handle logic operations.
 */
class SensorViewModel : ViewModel(), XsensDotDeviceCallback {
    // A callback function to notify battery information
    private var mBatteryChangeInterface: BatteryChangedInterface? = null

    // A callback function to notify data changes event
    private var mDataChangeInterface: DataChangeInterface? = null

    // A list contains XsensDotDevice
    private val mSensorList = MutableLiveData<ArrayList<XsensDotDevice>?>()

    /**
     * Observe this function to listen which device's connection state is changed.
     *
     * @return The latest updated device
     */
    // A variable to notify the connection state
    val connectionChangedDevice = MutableLiveData<XsensDotDevice>()

    /**
     * Observe this function to listen which device's tag name is changed.
     *
     * @return The latest updated device
     */
    // A variable to notify the tag name
    val tagChangedDevice = MutableLiveData<XsensDotDevice>()

    // A variable to notify the streaming status
    private val mIsStreaming = MutableLiveData<Boolean?>()

    /**
     * Initialize data changes interface.
     *
     * @param callback The class which implemented DataChangeInterface
     */
    fun setDataChangeCallback(callback: DataChangeInterface?) {
        mDataChangeInterface = callback
    }

    /**
     * Initialize battery changes interface.
     *
     * @param callback The class which implemented setBatteryChangedCallback
     */
    fun setBatteryChangedCallback(callback: BatteryChangedInterface?) {
        mBatteryChangeInterface = callback
    }

    /**
     * Get the XsensDotDevice object from list by mac address.
     *
     * @param address The mac address of device
     * @return The XsensDotDevice object
     */
    fun getSensor(address: String): XsensDotDevice? {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                if (device.address == address) return device
            }
        }
        return null
    }

    /**
     * Get all XsensDotDevice objects from list.
     *
     * @return The list contains all devices
     */
    val allSensors: ArrayList<XsensDotDevice>?
        get() = if (mSensorList.value == null) ArrayList() else mSensorList.value

    /**
     * Initialize, connect the XsensDotDevice and put it into a list.
     *
     * @param context The application context
     * @param device  The scanned Bluetooth device
     */
    fun connectSensor(context: Context?, device: BluetoothDevice?) {
        val xsDevice = XsensDotDevice(context, device, this)
        addDevice(xsDevice)
        xsDevice.connect()
    }

    /**
     * Disconnect one device by mac address.
     *
     * @param address The mac address of device
     */
    fun disconnectSensor(address: String) {
        if (mSensorList.value != null) {
            for (device in mSensorList.value!!) {
                if (device.address == address) {
                    device.disconnect()
                    break
                }
            }
        }
    }

    /**
     * Disconnect all devices which are exist in the list.
     */
    fun disconnectAllSensors() {
        if (mSensorList.value != null) {
            synchronized(LOCKER) {
                val it: Iterator<XsensDotDevice> = mSensorList.value!!.iterator()
                while (it.hasNext()) {

                    // Use Iterator to make sure it's thread safety.
                    val device = it.next()
                    device.disconnect()
                }
            }
        }
    }

    /**
     * Cancel reconnection of one sensor.
     *
     * @param address The mac address of device
     */
    fun cancelReconnection(address: String) {
        if (mSensorList.value != null) {
            for (device in mSensorList.value!!) {
                if (device.address == address) {
                    device.cancelReconnecting()
                    break
                }
            }
        }
    }

    /**
     * Check the connection state of all sensors.
     *
     * @return True - If all sensors are connected
     */
    fun checkConnection(): Boolean {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                val state = device.connectionState
                if (state != XsensDotDevice.CONN_STATE_CONNECTED) return false
            }
        } else {
            return false
        }
        return true
    }

    /**
     * Get the tag name from sensor.
     *
     * @param address The mac address of device
     * @return The tag name
     */
    fun getTag(address: String): String {
        val device = getSensor(address)
        if (device != null) {
            val tag = device.tag
            return tag ?: device.name
        }
        return ""
    }

    /**
     * Set the plotting and logging states for each device.
     *
     * @param plot The plot state
     * @param log  The log state
     */
    fun setStates(plot: Int, log: Int) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                device.plotState = plot
                device.logState = log
            }
        }
    }

    /**
     * Set the measurement mode to all sensors.
     *
     * @param mode The measurement mode
     */
    fun setMeasurementMode(mode: Int) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                device.measurementMode = mode
            }
        }
    }

    /**
     * Set one sensor for root of synchronization.
     *
     * @param isRoot True - If set to root
     */
    fun setRootDevice(isRoot: Boolean) {
        val devices = mSensorList.value
        if (devices != null && devices.size > 0) devices[0].isRootDevice = isRoot
    }

    /**
     * Start/Stop measuring for each sensor.
     *
     * @param enabled True - Start outputting data
     */
    fun setMeasurement(enabled: Boolean) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                if (enabled) device.startMeasuring() else device.stopMeasuring()
            }
        }
    }

    /**
     * Add the XsensDotDevice to a list, the UID is mac address.
     *
     * @param xsDevice The XsensDotDevice object
     */
    private fun addDevice(xsDevice: XsensDotDevice) {
        if (mSensorList.value == null) mSensorList.value = ArrayList()
        val devices = mSensorList.value
        var isExist = false
        for (_xsDevice in devices!!) {
            if (xsDevice.address == _xsDevice.address) {
                isExist = true
                break
            }
        }
        if (!isExist) devices.add(xsDevice)
    }

    /**
     * If device is disconnected by user means don't need to reconnect. So remove this device from list by mac address.
     *
     * @param address The mac address of device
     */
    fun removeDevice(address: String) {
        if (mSensorList.value == null) {
            mSensorList.value = ArrayList()
            return
        }
        synchronized(LOCKER) {
            val it = mSensorList.value!!.iterator()
            while (it.hasNext()) {

                // Use Iterator to make sure it's thread safety.
                val device = it.next()
                if (device.address == address) {
                    it.remove()
                    break
                }
            }
        }
    }

    /**
     * Remove all sensor from device list directly.
     */
    fun removeAllDevice() {
        if (mSensorList.value != null) {
            synchronized(LOCKER) { mSensorList.value!!.clear() }
        }
    }

    /**
     * Observe this function to listen the streaming status.
     *
     * @return The latest streaming status
     */
    val isStreaming: MutableLiveData<Boolean?>
        get() {
            if (mIsStreaming.value == null) mIsStreaming.value = false
            return mIsStreaming
        }

    /**
     * Notify the streaming status to activity/fragment
     *
     * @param status The status of streaming
     */
    fun updateStreamingStatus(status: Boolean) {
        mIsStreaming.postValue(status)
    }

    override fun onXsensDotConnectionChanged(address: String, state: Int) {
        Log.i(TAG, "onXsensDotConnectionChanged() - address = $address, state = $state")
        val xsDevice = getSensor(address)
        if (xsDevice != null) connectionChangedDevice.postValue(xsDevice)
        when (state) {
            XsensDotDevice.CONN_STATE_DISCONNECTED -> synchronized(this) { removeDevice(address) }
            XsensDotDevice.CONN_STATE_CONNECTING -> {}
            XsensDotDevice.CONN_STATE_CONNECTED -> {}
            XsensDotDevice.CONN_STATE_RECONNECTING -> {}
        }
    }

    override fun onXsensDotServicesDiscovered(address: String, status: Int) {
        Log.i(TAG, "onXsensDotServicesDiscovered() - address = $address, status = $status")
    }

    override fun onXsensDotFirmwareVersionRead(address: String, version: String) {
        Log.i(TAG, "onXsensDotFirmwareVersionRead() - address = $address, version = $version")
    }

    override fun onXsensDotTagChanged(address: String, tag: String) {
        // This callback function will be triggered in the connection precess.
        Log.i(TAG, "onXsensDotTagChanged() - address = $address, tag = $tag")

        // The default value of tag is an empty string.
        if (tag != "") {
            val device = getSensor(address)
            if (device != null) tagChangedDevice.postValue(device)
        }
    }

    override fun onXsensDotBatteryChanged(address: String, status: Int, percentage: Int) {
        // This callback function will be triggered in the connection precess.
        Log.i(TAG, "onXsensDotBatteryChanged() - address = $address, status = $status, percentage = $percentage")

        // The default value of status and percentage is -1.
        if (status != -1 && percentage != -1) {
            // Use callback function instead of LiveData to notify the battery information.
            // Because when user removes the USB cable from housing, this function will be triggered 5 times.
            // Use LiveData will lose some notification.
            if (mBatteryChangeInterface != null) mBatteryChangeInterface!!.onBatteryChanged(address, status, percentage)
        }
    }

    override fun onXsensDotDataChanged(address: String, data: XsensDotData) {
        Log.i(TAG, "onXsensDotDataChanged() - address = $address")

        // Don't use LiveData variable to transfer data to activity/fragment.
        // The main (UI) thread isn't fast enough to store data by 60Hz.
        if (mDataChangeInterface != null) mDataChangeInterface!!.onDataChanged(address, data)
    }

    override fun onXsensDotInitDone(address: String) {
        Log.i(TAG, "onXsensDotInitDone() - address = $address")
    }

    override fun onXsensDotButtonClicked(address: String, timestamp: Long) {
        Log.i(TAG, "onXsensDotButtonClicked() - address = $address, timestamp = $timestamp")
    }

    override fun onXsensDotPowerSavingTriggered(address: String) {
        Log.i(TAG, "onXsensDotPowerSavingTriggered() - address = $address")
    }

    override fun onReadRemoteRssi(address: String, rssi: Int) {
        Log.i(TAG, "onReadRemoteRssi() - address = $address, rssi = $rssi")
    }

    override fun onXsensDotOutputRateUpdate(address: String, outputRate: Int) {
        Log.i(TAG, "onXsensDotOutputRateUpdate() - address = $address, outputRate = $outputRate")
    }

    override fun onXsensDotFilterProfileUpdate(address: String, filterProfileIndex: Int) {
        Log.i(TAG, "onXsensDotFilterProfileUpdate() - address = $address, filterProfileIndex = $filterProfileIndex")
    }

    override fun onXsensDotGetFilterProfileInfo(address: String, filterProfileInfoList: ArrayList<FilterProfileInfo>) {
        Log.i(TAG, "onXsensDotGetFilterProfileInfo() - address = " + address + ", size = " + filterProfileInfoList.size)
    }

    override fun onSyncStatusUpdate(address: String, isSynced: Boolean) {
        Log.i(TAG, "onSyncStatusUpdate() - address = $address, isSynced = $isSynced")
    }

    companion object {
        private val TAG = SensorViewModel::class.java.simpleName

        /**
         * Get the instance of SensorViewModel
         *
         * @param owner The life cycle owner from activity/fragment
         * @return The SensorViewModel
         */
        @JvmStatic
        fun getInstance(owner: ViewModelStoreOwner): SensorViewModel {
            return ViewModelProvider(owner, NewInstanceFactory()).get(SensorViewModel::class.java)
        }

        // A variable to queue multiple threads.
        private val LOCKER = Any()
    }



}