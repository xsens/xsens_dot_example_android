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

import XsRecordingFileInfo
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.adapters.RecordingAdapter
import com.xsens.dot.android.example.databinding.FragmentRecordingBinding
import com.xsens.dot.android.example.viewmodels.SensorViewModel
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotRecordingCallback
import com.xsens.dot.android.sdk.models.XsensDotDevice
import com.xsens.dot.android.sdk.models.XsensDotRecordingFileInfo
import com.xsens.dot.android.sdk.models.XsensDotRecordingState
import com.xsens.dot.android.sdk.recording.XsensDotRecordingManager
import java.util.HashMap
import kotlin.collections.ArrayList

class RecordingFragment : Fragment(), XsensDotRecordingCallback {

    private var recordingObserver: Observer<Boolean>? = null
    private var mRecordingBinding: FragmentRecordingBinding? = null
    private var mFlashInfoCounter: Int = 0
    private var mIsRecording: Boolean = false
    private var isRecording: MutableLiveData<Boolean> = MutableLiveData(false)
    private val mRecordingDataList: ArrayList<RecordingData> = ArrayList()
    private var mRecordingManagers: HashMap<String, RecordingData> = HashMap()

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null


    //region Fragment method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        mRecordingBinding = FragmentRecordingBinding.inflate(LayoutInflater.from(context))

        initView()
        return mRecordingBinding!!.root
    }

    private fun initView() {
        recordingObserver = Observer {
            mIsRecording = it
            if (mIsRecording) {
                mRecordingBinding?.btnStartStopRecording?.text = getString(R.string.stop_recording)
            } else {
                mRecordingBinding?.btnStartStopRecording?.text = getString(R.string.start_recording)
            }
        }
        mRecordingBinding?.let { binding ->
            isRecording.observeForever(recordingObserver!!)
            binding.btnStartStopRecording.isEnabled = false
            binding.rcvDevices.layoutManager = LinearLayoutManager(activity)
            binding.rcvDevices.adapter = RecordingAdapter(mRecordingDataList)
            binding.btnStartStopRecording.setOnClickListener {
                if (!mIsRecording) {
                    checkIfCanStartRecording()
                } else {
                    stopRecording()
                }
            }

            binding.btnRequestFileInfo.setOnClickListener {

                clearRecordingManagers()
                val fragmentManager = activity!!.supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.container, ExportFragment(), ExportFragment.TAG)
                fragmentTransaction.setReorderingAllowed(true)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            }
        }
        enableDataRecordingNotification()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording.removeObserver(recordingObserver!!)
        stopRecording()
        clearRecordingManagers()
    }

    override fun onResume() {
        super.onResume()
        // Notify main activity to refresh menu.
        enableDataRecordingNotification()
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_RECORD
        if (activity != null) requireActivity().invalidateOptionsMenu()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RecordingFragment()
    }

    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = SensorViewModel.getInstance(activity!!)
        }
    }
    //endregion

    //region Recording

    private fun enableDataRecordingNotification() {

        if (!mSensorViewModel!!.checkConnection()) {
            Toast.makeText(context, getString(R.string.hint_check_connection), Toast.LENGTH_LONG).show()
            return
        } else {
            mSensorViewModel!!.allSensors?.let {
                mFlashInfoCounter = 0
                clearRecordingManagers()
                mRecordingDataList.clear()
                for (device in it) {
                    val recordingManager = XsensDotRecordingManager(context!!, device, this)
                    val recordingData = RecordingData(device = device, canRecord = false, recordingManager = recordingManager, isNotificationEnabled = false, isRecording = false)

                    mRecordingManagers[device.address] = recordingData
                    mRecordingDataList.add(recordingData)

                    SystemClock.sleep(30)
                    recordingManager.enableDataRecordingNotification()
                }

                mRecordingBinding?.rcvDevices?.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun startRecording() {
        for ((address, data) in mRecordingManagers) {
            data?.let {
                SystemClock.sleep(30)
                var success = data.recordingManager.startRecording()
                if (!success) {
                    SystemClock.sleep(40)
                    data.recordingManager.startRecording()
                }
            }
        }
        isRecording.postValue(true)
    }

    private fun stopRecording() {
        for ((address, data) in mRecordingManagers) {
            data?.let {
                data.recordingManager.stopRecording()
            }
        }
        isRecording.postValue(false)
    }

    private fun clearRecordingManagers() {
        for ((_, manager) in mRecordingManagers) {
            manager.recordingManager.clear()
        }
        mRecordingManagers.clear()
    }

    private fun checkIfCanStartRecording() {
        var canStart = true
        for ((address, data) in mRecordingManagers) {
            data?.let {
                if (!it.canRecord) {
                    canStart = false
                }
            }
        }
        if (canStart) {
            startRecording()
        }
    }
    //endregion

    //region List methods

    private fun getRecordingDataFromList(address: String): RecordingData? {
        if (mRecordingManagers.containsKey(address)) {
            return mRecordingManagers[address]
        }
        return null
    }

    private fun getItemPositionByAddress(address: String): Int {
        return mRecordingDataList.indexOfFirst {
            it.device.address == address
        }
    }


    private fun updateRecyclerViewItemByAddress(address: String) {
        val index = getItemPositionByAddress(address)
        updateRecyclerViewItem(index)
    }

    private fun updateRecyclerViewItem(index: Int) {
        if (index != -1) {
            activity?.let { act ->
                act.runOnUiThread {
                    mRecordingBinding?.rcvDevices?.adapter?.notifyItemChanged(index)
                }
            }
        }
    }
    //endregion

    //region Recording Callbacks
    override fun onXsensDotRecordingNotification(address: String?, isEnabled: Boolean) {
        //Callback from the recordingManager.enableRecordingNotification() method
        address?.let {
            mRecordingManagers[it]?.isNotificationEnabled = isEnabled
            if (isEnabled) {
                SystemClock.sleep(30)
                mRecordingManagers[it]?.recordingManager?.requestFlashInfo()
            }
        }
    }

    override fun onXsensDotRequestFlashInfoDone(address: String?, usedFlashSpace: Int, totalFlashSpace: Int) {
        ///Callback from the recordginManager.requestFlashInfo() method
        // get usedFlashSpace & totalFlashSpace, if the available flash space <= 10%, it cannot start recording
        address?.let {
            val storagePercent = if (totalFlashSpace != 0) (((totalFlashSpace - usedFlashSpace) / totalFlashSpace.toFloat()) * 100).toInt() else 0
            val isStorageFull = if (totalFlashSpace != 0) (storagePercent <= 10) else false
            val canStartRecording = !isStorageFull
            mRecordingManagers[it]?.canRecord = canStartRecording
            mFlashInfoCounter++

            if (mFlashInfoCounter == mRecordingManagers.size) {
                mFlashInfoCounter = 0
                activity?.let { act ->
                    act.runOnUiThread {
                        mRecordingBinding?.btnStartStopRecording?.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onXsensDotEraseDone(address: String?, isSuccess: Boolean) {

    }

    override fun onXsensDotRecordingAck(address: String?, recordingId: Int, isSuccess: Boolean, recordingState: XsensDotRecordingState?) {
        if (recordingId == XsensDotRecordingManager.RECORDING_ID_START_RECORDING) {
            // start recording result, check recordingState, it should be success or fail.
            address?.let {
                getRecordingDataFromList(it)?.let { data ->
                    data.isRecording = recordingState == XsensDotRecordingState.success
                    updateRecyclerViewItemByAddress(address)
                }
            }
        } else if (recordingId == XsensDotRecordingManager.RECORDING_ID_STOP_RECORDING) {
            // stop recording result, check recordingState, it should be success or fail.

            address?.let {
                getRecordingDataFromList(it)?.let { data ->
                    data.isRecording = !(recordingState == XsensDotRecordingState.success)
                    updateRecyclerViewItemByAddress(address)
                }
            }
        } else if (recordingId == XsensDotRecordingManager.RECORDING_ID_GET_STATE) {
            //Get recording status using mRecordingManager.requestRecordingState()
            if (recordingState == XsensDotRecordingState.onErasing
                || recordingState == XsensDotRecordingState.onExportFlashInfo
                || recordingState == XsensDotRecordingState.onRecording
                || recordingState == XsensDotRecordingState.onExportRecordingFileInfo
                || recordingState == XsensDotRecordingState.onExportRecordingFileData
            ) {

            }
        }
    }

    override fun onXsensDotGetRecordingTime(address: String?, startUTCSeconds: Int, totalRecordingSeconds: Int, remainingRecordingSeconds: Int) {
        address?.let {
            Log.d("TIME ::::: ", "$totalRecordingSeconds ___ $remainingRecordingSeconds")
        }
    }

    override fun onXsensDotRequestFileInfoDone(address: String?, list: ArrayList<XsensDotRecordingFileInfo>?, isSuccess: Boolean) {
        // A list of file information can be obtained, one message contains: fileId, fileName, dataSize
        address?.let { }
    }

    override fun onXsensDotDataExported(p0: String?, p1: XsensDotRecordingFileInfo?, p2: XsensDotData?) {
    }

    override fun onXsensDotDataExported(p0: String?, p1: XsensDotRecordingFileInfo?) {
    }

    override fun onXsensDotAllDataExported(p0: String?) {
    }

    override fun onXsensDotStopExportingData(p0: String?) {
    }

    //endregion
}


data class RecordingData(
    var device: XsensDotDevice,
    var canRecord: Boolean,
    var isNotificationEnabled: Boolean = false,
    var recordingManager: XsensDotRecordingManager,
    var isRecording: Boolean = false
) {
    var recordingFileInfoList: ArrayList<XsRecordingFileInfo> = ArrayList()
}