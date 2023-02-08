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
package com.xsens.dot.android.example.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.databinding.ActivityMainBinding
import com.xsens.dot.android.example.interfaces.RecordingClickListener
import com.xsens.dot.android.example.interfaces.ScanClickInterface
import com.xsens.dot.android.example.interfaces.StreamingClickInterface
import com.xsens.dot.android.example.utils.Utils.isBluetoothAdapterEnabled
import com.xsens.dot.android.example.utils.Utils.isLocationPermissionGranted
import com.xsens.dot.android.example.utils.Utils.requestEnableBluetooth
import com.xsens.dot.android.example.utils.Utils.requestLocationPermission
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel
import com.xsens.dot.android.example.viewmodels.SensorViewModel

/**
 * The main activity.
 */
class MainActivity : AppCompatActivity() {
    // The view binder of MainActivity
    private var mBinding: ActivityMainBinding? = null

    // The Bluetooth view model instance
    private var mBluetoothViewModel: BluetoothViewModel? = null

    // The sensor view model instance
    private var mSensorViewModel: SensorViewModel? = null

    // A variable for scanning flag
    private var mIsScanning = false

    // Send the start/stop scan click event to fragment
    private var mScanListener: ScanClickInterface? = null

    // Send the start/stop streaming click event to fragment
    private var mStreamingListener: StreamingClickInterface? = null

    // Send the start/stop recording click event to fragment
    private var mRecordingListener: RecordingClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(mBinding!!.root)
        setupFragmentContainer()
        bindViewModel()
        checkBluetoothAndPermission()

        // Register this action to monitor Bluetooth status.
        registerReceiver(mBluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onPostResume() {
        super.onPostResume()
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBluetoothStateReceiver)
    }

    override fun onBackPressed() {
        val manager = supportFragmentManager

        // If the fragment count > 0 in the stack, try to resume the previous page.
        if (manager.backStackEntryCount > 0) manager.popBackStack() else super.onBackPressed()
    }

    // region Bluetooth permission check
    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        } else {
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }
    //endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult() - requestCode = $requestCode, resultCode = $resultCode")
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) checkBluetoothAndPermission() else Toast.makeText(this, getString(R.string.hint_turn_on_bluetooth), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult() - requestCode = $requestCode")
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            for (i in grantResults.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) checkBluetoothAndPermission() else Toast.makeText(
                        this,
                        getString(R.string.hint_allow_location),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val scanItem = menu.findItem(R.id.action_scan)
        val streamingItem = menu.findItem(R.id.action_streaming)
        val measureItem = menu.findItem(R.id.action_measure)
        val recordItem = menu.findItem(R.id.action_recording)
        if (mIsScanning) scanItem.title = getString(R.string.menu_stop_scan) else scanItem.title = getString(R.string.menu_start_scan)
        val isStreaming = mSensorViewModel!!.isStreaming.value!!
        if (isStreaming) streamingItem.title = getString(R.string.menu_stop_streaming) else streamingItem.title = getString(R.string.menu_start_streaming)
        when (sCurrentFragment) {
            FRAGMENT_TAG_SCAN -> {
                scanItem.isVisible = true
                streamingItem.isVisible = false
                measureItem.isVisible = true
                recordItem.isVisible = true
            }
            FRAGMENT_TAG_DATA -> {
                scanItem.isVisible = false
                recordItem.isVisible = false
                streamingItem.isVisible = true
                measureItem.isVisible = false
            }
            FRAGMENT_TAG_RECORD -> {
                scanItem.isVisible = false
                recordItem.isVisible = false
                streamingItem.isVisible = false
                measureItem.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_scan -> if (mScanListener != null && checkBluetoothAndPermission()) {
                // Make sure th location permission is granted then start/stop scanning.
                if (mIsScanning) mScanListener!!.onScanTriggered(false) else mScanListener!!.onScanTriggered(true)
            }
            R.id.action_streaming ->                 // When the streaming button is clicked, notify to DataFragment and wait for the syncing result.
                mStreamingListener?.onStreamingTriggered()
            R.id.action_measure -> {
                // Change to DataFragment and put ScanFragment to the back stack.
                val dataFragment: Fragment = DataFragment.newInstance()
                addFragment(dataFragment, FRAGMENT_TAG_DATA)
            }
            R.id.action_recording -> {
                if (!mSensorViewModel!!.checkConnection()) {
                    Toast.makeText(this, getString(R.string.hint_check_connection), Toast.LENGTH_LONG).show()
                } else {
                    // Change to DataFragment and put ScanFragment to the back stack.
                    val recordingFragment: Fragment = RecordingFragment.newInstance()
                    addFragment(recordingFragment, FRAGMENT_TAG_RECORD)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Use ScanFragment as default page.
     */
    private fun setupFragmentContainer() {
        if (null != intent) {
            val fragment = ScanFragment.newInstance()
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment, FRAGMENT_TAG_SCAN).commit()
        }
    }

    /**
     * Add a fragment to full the screen.
     *
     * @param fragment The instance of fragment
     * @param tag      The tag of fragment
     */
    private fun addFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, tag).addToBackStack(null).commit()
    }

    /**
     * Check the state of Bluetooth adapter and location permission.
     */
    private fun checkBluetoothAndPermission(): Boolean {
        val isBluetoothEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else isBluetoothAdapterEnabled(this)
        val isPermissionGranted = isLocationPermissionGranted(this)
        if (isBluetoothEnabled) {
            if (!isPermissionGranted) requestLocationPermission(this, REQUEST_PERMISSION_LOCATION)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                requestEnableBluetooth(this, REQUEST_ENABLE_BLUETOOTH)
            }
        }
        val status = isBluetoothEnabled && isPermissionGranted
        Log.i(TAG, "checkBluetoothAndPermission() - $status")
        mBluetoothViewModel!!.updateBluetoothEnableState(status)
        return status
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        mBluetoothViewModel = BluetoothViewModel.getInstance(this)
        mBluetoothViewModel!!.isScanning.observe(this) { scanning -> // If the status of scanning is changed, try to refresh the menu.
            mIsScanning = scanning
            invalidateOptionsMenu()
        }
        mSensorViewModel = SensorViewModel.getInstance(this)
        mSensorViewModel!!.isStreaming.observe(this) { // If the status of streaming is changed, try to refresh the menu.
            invalidateOptionsMenu()
        }
    }

    /**
     * Set the trigger of scan button.
     *
     * @param listener The class which implemented ScanClickInterface
     */
    fun setScanTriggerListener(listener: ScanClickInterface?) {
        mScanListener = listener
    }

    /**
     * Set the trigger of streaming button.
     *
     * @param listener The class which implemented StreamingClickInterface
     */
    fun setStreamingTriggerListener(listener: StreamingClickInterface?) {
        mStreamingListener = listener
    }

    /**
     * Set the trigger of recording button.
     *
     * @param listener The class which implemented RecordingClickListener
     */
    fun setRecordingTriggerListener(listener: RecordingClickListener?) {
        mRecordingListener = listener
    }

    /**
     * A receiver for Bluetooth status.
     */
    private val mBluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> mBluetoothViewModel!!.updateBluetoothEnableState(false)
                        BluetoothAdapter.STATE_ON -> mBluetoothViewModel!!.updateBluetoothEnableState(true)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        // The code of request
        private const val REQUEST_ENABLE_BLUETOOTH = 1001
        private const val REQUEST_PERMISSION_LOCATION = 1002

        // The tag of fragments
        const val FRAGMENT_TAG_SCAN = "scan"
        const val FRAGMENT_TAG_DATA = "data"
        const val FRAGMENT_TAG_RECORD = "record"

        // A variable to keep the current fragment id
        var sCurrentFragment = FRAGMENT_TAG_SCAN
    }
}