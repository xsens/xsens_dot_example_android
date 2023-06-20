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
package com.xsens.dot.android.example.views

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.adapters.ScanAdapter
import com.xsens.dot.android.example.databinding.FragmentScanBinding
import com.xsens.dot.android.example.interfaces.BatteryChangedInterface
import com.xsens.dot.android.example.interfaces.ScanClickInterface
import com.xsens.dot.android.example.interfaces.SensorClickInterface
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel
import com.xsens.dot.android.example.viewmodels.SensorViewModel
import com.xsens.dot.android.sdk.interfaces.DotScannerCallback
import com.xsens.dot.android.sdk.models.DotDevice
import com.xsens.dot.android.sdk.utils.DotScanner

/**
 * A fragment for scanned item.
 */
class ScanFragment : Fragment(), DotScannerCallback, SensorClickInterface, ScanClickInterface, BatteryChangedInterface {
    // The view binder of ScanFragment
    private var mBinding: FragmentScanBinding? = null

    // The Bluetooth view model instance
    private var mBluetoothViewModel: BluetoothViewModel? = null

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null

    // The adapter for scanned device item
    private var mScanAdapter: ScanAdapter? = null

    // A list contains scanned Bluetooth device
    private val mScannedSensorList = ArrayList<HashMap<String, Any>>()

    // The XsensDotScanner object
    private var mXsDotScanner: DotScanner? = null

    // A variable for scanning flag
    private var mIsScanning = false

    // A dialog during the connection
    private var mConnectionDialog: AlertDialog? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = FragmentScanBinding.inflate(LayoutInflater.from(context))
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mScanAdapter = ScanAdapter(requireContext(), mScannedSensorList)
        mScanAdapter!!.setSensorClickListener(this)
        mBinding!!.sensorRecyclerView.layoutManager = LinearLayoutManager(context)
        mBinding!!.sensorRecyclerView.itemAnimator = DefaultItemAnimator()
        mBinding!!.sensorRecyclerView.adapter = mScanAdapter
        val connectionDialogBuilder = AlertDialog.Builder(activity)
        connectionDialogBuilder.setTitle(getString(R.string.connecting))
        connectionDialogBuilder.setMessage(getString(R.string.hint_connecting))
        mConnectionDialog = connectionDialogBuilder.create()

        // Set the SensorClickInterface instance to main activity.
        if (activity != null) (activity as MainActivity?)!!.setScanTriggerListener(this)
    }

    override fun onResume() {
        super.onResume()

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_SCAN
        if (activity != null) requireActivity().invalidateOptionsMenu()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop scanning to let other apps to use scan function.
        if (mXsDotScanner != null) mXsDotScanner!!.stopScan()
        mBluetoothViewModel!!.updateScanState(false)
    }

    override fun onDetach() {
        super.onDetach()

        // Release all connections when app is destroyed.
        mSensorViewModel!!.disconnectAllSensors()
    }

    override fun onScanTriggered(triggered: Boolean) {
        if (triggered) {
            // Disconnect to all sensors to make sure the connection has been released.
            mSensorViewModel!!.disconnectAllSensors()
            // This line is for connecting and reconnecting device.
            // Because they don't triggered onXsensDotConnectionChanged() function to remove sensor from list.
            mSensorViewModel!!.removeAllDevice()
            mScannedSensorList.clear()
            mScanAdapter!!.notifyDataSetChanged()

            //This permission is only required in Android S or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1000)
            } else {
                mIsScanning = if (mXsDotScanner == null) false else mXsDotScanner!!.startScan()
            }
        } else {
            // If success for stopping, it will return True from SDK. So use !(not) here.
            mIsScanning = !mXsDotScanner!!.stopScan()
        }
        mBluetoothViewModel!!.updateScanState(mIsScanning)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mIsScanning = mXsDotScanner!!.startScan()
            }
        }
    }

    override fun onDotScanned(device: BluetoothDevice, rssi: Int) {

//        Log.i(TAG, "onXsensDotScanned() - Name: " + device.getName() + ", Address: " + device.getAddress());
        if (isAdded) {

            // Use the mac address as UID to filter the same scan result.
            var isExist = false
            for (map in mScannedSensorList) {
                if ((map[ScanAdapter.KEY_DEVICE] as BluetoothDevice?)!!.address == device.address) isExist = true
            }
            if (!isExist) {

                // The original connection state is Disconnected.
                // Also set tag, battery state, battery percentage to default value.
                val map = HashMap<String, Any>()
                map[ScanAdapter.KEY_DEVICE] = device
                map[ScanAdapter.KEY_CONNECTION_STATE] = DotDevice.CONN_STATE_DISCONNECTED
                map[ScanAdapter.KEY_TAG] = ""
                map[ScanAdapter.KEY_BATTERY_STATE] = -1
                map[ScanAdapter.KEY_BATTERY_PERCENTAGE] = -1
                mScannedSensorList.add(map)
                mScanAdapter!!.notifyItemInserted(mScannedSensorList.size - 1)
            }
        }
    }

    override fun onSensorClick(v: View?, position: Int) {

        // If success for stopping, it will return True from SDK. So use !(not) here.
        mIsScanning = !mXsDotScanner!!.stopScan()
        // Notify main activity to update the scan button.
        mBluetoothViewModel!!.updateScanState(false)
        val state = mScanAdapter!!.getConnectionState(position)
        val device = mScanAdapter!!.getDevice(position)
        when (state) {
            DotDevice.CONN_STATE_DISCONNECTED -> {
                mConnectionDialog!!.show()
                // The sensor isn't exist in the mSensorList(SensorViewModel), try to connect and add it.
                mSensorViewModel!!.connectSensor(context, device)
            }
            DotDevice.CONN_STATE_CONNECTING -> {
                mScanAdapter!!.updateConnectionState(position, DotDevice.CONN_STATE_DISCONNECTED)
                mScanAdapter!!.notifyItemChanged(position)
                // This line is necessary to close Bluetooth gatt.
                mSensorViewModel!!.disconnectSensor(device!!.address)
                // Remove this sensor from device list.
                mSensorViewModel!!.removeDevice(device.address)
            }
            DotDevice.CONN_STATE_CONNECTED -> mSensorViewModel!!.disconnectSensor(device!!.address)
            DotDevice.CONN_STATE_RECONNECTING -> {
                mScanAdapter!!.updateConnectionState(position, DotDevice.CONN_STATE_DISCONNECTED)
                mScanAdapter!!.notifyItemChanged(position)
                // This line is necessary to close Bluetooth gatt.
                mSensorViewModel!!.cancelReconnection(device!!.address)
                // Remove this sensor from device list.
                mSensorViewModel!!.removeDevice(device.address)
            }
        }
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        if (activity != null) {
            mBluetoothViewModel = BluetoothViewModel.getInstance(requireActivity())
            mSensorViewModel = SensorViewModel.getInstance(requireActivity())
            mBluetoothViewModel!!.isBluetoothEnabled.observe(this) { enabled ->
                Log.d(TAG, "isBluetoothEnabled = $enabled")
                if (enabled) {
                    initXsDotScanner()
                } else {
                    mIsScanning = false
                    mBluetoothViewModel!!.updateScanState(false)
                }
            }
            mSensorViewModel!!.connectionChangedDevice.observe(this) { device ->
                val address = device.address
                val state = device.connectionState
                Log.d(TAG, "getConnectionChangedDevice() - address = $address, state = $state")
                for (map in mScannedSensorList) {
                    val _device = map[ScanAdapter.KEY_DEVICE] as BluetoothDevice?
                    if (_device != null) {
                        val _address = _device.address
                        // Update connection state by the same mac address.
                        if (_address == address) {
                            map[ScanAdapter.KEY_CONNECTION_STATE] = state
                            mScanAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
                when (state) {
                    DotDevice.CONN_STATE_CONNECTED -> if (mConnectionDialog!!.isShowing) mConnectionDialog!!.dismiss()
                }
            }
            mSensorViewModel!!.tagChangedDevice.observe(this) { device ->
                val address = device.address
                val tag = device.tag
                mScanAdapter!!.updateTag(address, tag)
                mScanAdapter!!.notifyDataSetChanged()
                Log.d(TAG, "getTagChangedDevice() - address = $address, tag = $tag")
            }
            mSensorViewModel!!.setBatteryChangedCallback(this)
        }
    }

    /**
     * Setup for Movella DOT scanner.
     */
    private fun initXsDotScanner() {
        if (mXsDotScanner == null) {
            mXsDotScanner = DotScanner(context, this)
            mXsDotScanner!!.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        }
    }

    override fun onBatteryChanged(address: String?, state: Int, percentage: Int) {
        Log.d(TAG, "onBatteryChanged() - address = $address, state = $state, percentage = $percentage")
        mScanAdapter!!.updateBattery(address!!, state, percentage)
        if (activity != null) {
            // This event is coming from background thread, use UI thread to update item.
            requireActivity().runOnUiThread { mScanAdapter!!.notifyDataSetChanged() }
        }
    }

    companion object {
        private val TAG = ScanFragment::class.java.simpleName

        /**
         * Get the instance of ScanFragment
         *
         * @return The instance of ScanFragment
         */
        @JvmStatic
        fun newInstance(): ScanFragment {
            return ScanFragment()
        }
    }
}