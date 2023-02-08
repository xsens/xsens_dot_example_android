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

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.xsens.dot.android.example.BuildConfig
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.adapters.DataAdapter
import com.xsens.dot.android.example.databinding.FragmentDataBinding
import com.xsens.dot.android.example.interfaces.DataChangeInterface
import com.xsens.dot.android.example.interfaces.RecordingClickListener
import com.xsens.dot.android.example.interfaces.StreamingClickInterface
import com.xsens.dot.android.example.viewmodels.SensorViewModel
import com.xsens.dot.android.example.viewmodels.SensorViewModel.Companion.getInstance
import com.xsens.dot.android.example.views.DataFragment
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotRecordingCallback
import com.xsens.dot.android.sdk.interfaces.XsensDotSyncCallback
import com.xsens.dot.android.sdk.models.*
import com.xsens.dot.android.sdk.recording.XsensDotRecordingManager
import com.xsens.dot.android.sdk.utils.XsensDotLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * A fragment for presenting the data and storing to file.
 */
class DataFragment : Fragment(), StreamingClickInterface, DataChangeInterface, XsensDotSyncCallback {
    // The view binder of DataFragment
    private var mBinding: FragmentDataBinding? = null

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null

    // The adapter for data item
    private var mDataAdapter: DataAdapter? = null

    // A list contains tag and data from each sensor
    private val mDataList: ArrayList<HashMap<String, Any?>> = ArrayList<HashMap<String, Any?>>()

    // A list contains mac address and XsensDotLogger object.
    private val mLoggerList: MutableList<HashMap<String, Any>> = ArrayList()

    // A variable for data logging flag
    private var mIsLogging = false

    // A dialog during the synchronization
    private var mSyncingDialog: AlertDialog? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = FragmentDataBinding.inflate(LayoutInflater.from(context))
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSensorViewModel!!.setStates(XsensDotDevice.PLOT_STATE_ON, XsensDotDevice.LOG_STATE_ON)
        mDataAdapter = DataAdapter(context!!, mDataList)
        mBinding!!.dataRecyclerView.layoutManager = LinearLayoutManager(context)
        mBinding!!.dataRecyclerView.itemAnimator = DefaultItemAnimator()
        mBinding!!.dataRecyclerView.adapter = mDataAdapter
        val syncingDialogBuilder = AlertDialog.Builder(activity)
        syncingDialogBuilder.setView(R.layout.dialog_syncing)
        syncingDialogBuilder.setCancelable(false)
        mSyncingDialog = syncingDialogBuilder.create()
        mSyncingDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
            val bar = mSyncingDialog!!.findViewById<ProgressBar>(R.id.syncing_progress)
            // Reset progress to 0 for next time to use.
            if (bar != null) bar.progress = 0
        })

        // Set the StreamingClickInterface instance to main activity.
        if (activity != null) (activity as MainActivity?)!!.setStreamingTriggerListener(this)
    }

    override fun onResume() {
        super.onResume()

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_DATA
        if (activity != null) activity!!.invalidateOptionsMenu()
    }

    override fun onDetach() {
        super.onDetach()

        // Stop measurement for each sensor when exiting this page.
        mSensorViewModel!!.setMeasurement(false)
        // It's necessary to update this status, because user may enter this page again.
        mSensorViewModel!!.updateStreamingStatus(false)
        closeFiles()
    }

    override fun onStreamingTriggered() {
        if (mSensorViewModel!!.isStreaming.value!!) {
            // To stop.
            mSensorViewModel!!.setMeasurement(false)
            mSensorViewModel!!.updateStreamingStatus(false)
            XsensDotSyncManager.getInstance(this).stopSyncing()
            closeFiles()
        } else {
            // To start.
            resetPage()
            if (!mSensorViewModel!!.checkConnection()) {
                Toast.makeText(context, getString(R.string.hint_check_connection), Toast.LENGTH_LONG).show()
                return
            }

            // Set first device to root.
            mSensorViewModel!!.setRootDevice(true)
            val devices = mSensorViewModel!!.allSensors
            // Devices will disconnect during the syncing, and do reconnection automatically.
            XsensDotSyncManager.getInstance(this).startSyncing(devices!!, SYNCING_REQUEST_CODE)
            if (!mSyncingDialog!!.isShowing) mSyncingDialog!!.show()
        }
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = getInstance(activity!!)
            // Implement DataChangeInterface and override onDataChanged() function to receive data.
            mSensorViewModel!!.setDataChangeCallback(this)
        }
    }

    /**
     * Reset page UI to default.
     */
    private fun resetPage() {
        mBinding!!.syncResult.text = "-"
        mDataList.clear()
        mDataAdapter!!.notifyDataSetChanged()
    }

    /**
     * Get the filter profile name.
     *
     * @param device The XsensDotDevice object
     * @return The filter profile name, "General" by default
     */
    private fun getFilterProfileName(device: XsensDotDevice): String {
        val index = device.currentFilterProfileIndex
        val list = device.filterProfileInfoList
        for (info in list) {
            if (info.index == index) return info.name
        }
        return "General"
    }

    /**
     * Create data logger for each sensor.
     */
    private fun createFiles() {

        // Remove XsensDotLogger objects from list before start data logging.
        mLoggerList.clear()
        val devices = mSensorViewModel!!.allSensors
        for (device in devices!!) {
            val appVersion = BuildConfig.VERSION_NAME
            val fwVersion = device.firmwareVersion
            val address = device.address
            val tag = if (device.tag.isEmpty()) device.name else device.tag
            var filename = ""
            if (context != null) {

                // Store log file in app internal folder.
                // Don't need user to granted the storage permission.
                val dir = context!!.getExternalFilesDir(null)
                if (dir != null) {

                    // This filename contains full file path.
                    filename = dir.absolutePath +
                            File.separator +
                            tag + "_" +
                            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date()) +
                            ".csv"
                }
            }
            Log.d(TAG, "createFiles() - $filename")
            val logger = XsensDotLogger(
                context,
                XsensDotLogger.TYPE_CSV,
                XsensDotPayload.PAYLOAD_TYPE_COMPLETE_EULER,
                filename,
                tag,
                fwVersion,
                device.isSynced,
                device.currentOutputRate,
                getFilterProfileName(device),
                appVersion
            )

            // Use mac address as a key to find logger object.
            val map = HashMap<String, Any>()
            map[DataAdapter.KEY_ADDRESS] = address
            map[KEY_LOGGER] = logger
            mLoggerList.add(map)
        }
        mIsLogging = true
    }

    /**
     * Update data to specific file.
     *
     * @param address The mac address of device
     * @param data    The XsensDotData packet
     */
    private fun updateFiles(address: String?, data: XsensDotData?) {
        for (map in mLoggerList) {
            val _address = map[DataAdapter.KEY_ADDRESS] as String?
            if (_address != null) {
                if (_address == address) {
                    val logger = map[KEY_LOGGER] as XsensDotLogger?
                    if (logger != null && mIsLogging) logger.update(data)
                }
            }
        }
    }

    /**
     * Close the data output stream.
     */
    private fun closeFiles() {
        mIsLogging = false
        for (map in mLoggerList) {
            // Call stop() function to flush and close the output stream.
            // Data is kept in the stream buffer and write to file when the buffer is full.
            // Call this function to write data to file whether the buffer is full or not.
            val logger = map[KEY_LOGGER] as XsensDotLogger?
            logger?.stop()
        }
    }

    override fun onSyncingStarted(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(TAG, "onSyncingStarted() - address = $address, isSuccess = $isSuccess, requestCode = $requestCode")
    }

    override fun onSyncingProgress(progress: Int, requestCode: Int) {
        Log.i(TAG, "onSyncingProgress() - progress = $progress, requestCode = $requestCode")
        if (requestCode == SYNCING_REQUEST_CODE) {
            if (mSyncingDialog!!.isShowing) {
                if (activity != null) {
                    activity!!.runOnUiThread { // Find the view of progress bar in dialog layout and update.
                        val bar = mSyncingDialog!!.findViewById<ProgressBar>(R.id.syncing_progress)
                        if (bar != null) bar.progress = progress
                    }
                }
            }
        }
    }

    override fun onSyncingResult(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(TAG, "onSyncingResult() - address = $address, isSuccess = $isSuccess, requestCode = $requestCode")
    }

    override fun onSyncingDone(syncingResultMap: HashMap<String, Boolean>, isSuccess: Boolean, requestCode: Int) {
        Log.i(TAG, "onSyncingDone() - isSuccess = $isSuccess, requestCode = $requestCode")
        if (requestCode == SYNCING_REQUEST_CODE) {
            if (activity != null) {
                activity!!.runOnUiThread {
                    if (mSyncingDialog!!.isShowing) mSyncingDialog!!.dismiss()
                    mSensorViewModel!!.setRootDevice(false)
                    if (isSuccess) {
                        mBinding!!.syncResult.setText(R.string.sync_result_success)

                        // Syncing precess is success, choose one measurement mode to start measuring.
                        mSensorViewModel!!.setMeasurementMode(XsensDotPayload.PAYLOAD_TYPE_COMPLETE_EULER)
                        createFiles()
                        mSensorViewModel!!.setMeasurement(true)
                        // Notify the current streaming status to MainActivity to refresh the menu.
                        mSensorViewModel!!.updateStreamingStatus(true)
                    } else {
                        mBinding!!.syncResult.setText(R.string.sync_result_fail)

                        // If the syncing result is fail, show a message to user
                        Toast.makeText(context, getString(R.string.hint_syncing_failed), Toast.LENGTH_LONG).show()
                        for ((address, value) in syncingResultMap) {
                            if (!value) {
                                // Get the key of this failed device.
                                // It's preferred to stop measurement of all sensors.
                                mSensorViewModel!!.setMeasurement(false)
                                // Notify the current streaming status to MainActivity to refresh the menu.
                                mSensorViewModel!!.updateStreamingStatus(false)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSyncingStopped(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(TAG, "onSyncingStopped() - address = $address, isSuccess = $isSuccess, requestCode = $requestCode")
    }

    override fun onDataChanged(address: String?, data: XsensDotData?) {
        Log.i(TAG, "onDataChanged() - address = $address")
        var isExist = false
        for (map in mDataList) {
            val _address = map[DataAdapter.KEY_ADDRESS] as String?
            if (_address == address) {
                // If the data is exist, try to update it.
                map[DataAdapter.KEY_DATA] = data
                isExist = true
                break
            }
        }
        if (!isExist) {
            // It's the first data of this sensor, create a new set and add it.
            val map: HashMap<String, Any?> = HashMap<String, Any?>()
            map[DataAdapter.KEY_ADDRESS] = address
            map[DataAdapter.KEY_TAG] = mSensorViewModel!!.getTag(address!!)
            map[DataAdapter.KEY_DATA] = data
            mDataList.add(map)
        }
        updateFiles(address, data)
        if (activity != null) {
            activity!!.runOnUiThread { // The data is coming from background thread, change to UI thread for updating.
                mDataAdapter!!.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private val TAG = DataFragment::class.java.simpleName

        // The code of request
        private const val SYNCING_REQUEST_CODE = 1001

        // The keys of HashMap
        const val KEY_LOGGER = "logger"

        /**
         * Get the instance of DataFragment
         *
         * @return The instance of DataFragment
         */
        @JvmStatic
        fun newInstance(): DataFragment {
            return DataFragment()
        }
    }
}