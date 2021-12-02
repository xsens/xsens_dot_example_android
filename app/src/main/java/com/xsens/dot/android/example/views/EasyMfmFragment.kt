//  Copyright (c) 2003-2021 Xsens Technologies B.V. or subsidiaries worldwide.
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

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.adapters.EasyMfmAdapter
import com.xsens.dot.android.example.databinding.FragmentEasyMfmBinding
import com.xsens.dot.android.example.models.MfmInfo
import com.xsens.dot.android.example.models.MfmResult
import com.xsens.dot.android.example.viewmodels.SensorViewModel

import com.xsens.dot.android.sdk.mfm.XsensDotMfmManager
import com.xsens.dot.android.sdk.mfm.XsensDotMfmProcessor
import com.xsens.dot.android.sdk.mfm.interfaces.XsensDotMfmCallback
import com.xsens.dot.android.sdk.mfm.models.XsensDotMfmResult
import com.xsens.dot.android.sdk.models.XsensDotDevice
import com.xsens.dot.android.sdk.models.XsensDotDevice.MEASUREMENT_STATE_ON
import kotlinx.coroutines.*
import java.util.*

class EasyMfmFragment : Fragment(), XsensDotMfmCallback, EasyMfmAdapter.ReWriteClickListener,
    View.OnClickListener {

    companion object {

        val TAG: String = EasyMfmFragment::class.java.simpleName

        const val MFM_TIMEOUT_SECONDS = 300
        const val MSG_WHAT_MFM_TIMEOUT = 1001

        // The devices view model instance
        private lateinit var mSensorViewModel: SensorViewModel

        fun newInstance(): EasyMfmFragment {
            return EasyMfmFragment()
        }
    }

    enum class MfmStatus {
        NON_STARTED, // The same as ">= FINISHED"
        STARTING,
        STARTED,
        PROCESSING, // Can't cancel
        WRITING, // Can't cancel
        FINISHED,
        DISCONNECTED,
        STOPPED,
        START_FAILED,
        PROCESS_FAILED,
        WRITE_FAILED
    }

    enum class ButtonStatus {
        CANCELABLE,
        NOT_CANCELABLE,
        COMPLETED,
        EXIT
    }

    private lateinit var mBinding: FragmentEasyMfmBinding
    private lateinit var mAdapter: EasyMfmAdapter

    private lateinit var mTimeoutHandler: TimeoutHandler

    private lateinit var mMfmProcessor: XsensDotMfmProcessor
    private val mMfmManagers: HashMap<String, XsensDotMfmManager> = HashMap()
    private val mMfmInfos = ArrayList<MfmInfo>()

    private var mButtonStatus = ButtonStatus.COMPLETED

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentEasyMfmBinding.inflate(LayoutInflater.from(context))
        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        init()
        initView()
    }

    override fun onResume() {
        super.onResume()

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_MFM
        requireActivity().invalidateOptionsMenu()
    }

    private fun init() {
        mTimeoutHandler = TimeoutHandler(this)

        val devices = mSensorViewModel.allSensors
        for (device in devices) {
            device?.let {
                val info = MfmInfo(it, MfmStatus.NON_STARTED.ordinal, 0, null)
                mMfmInfos.add(info)
            }
        }

        mMfmProcessor = XsensDotMfmProcessor(context)
        mMfmProcessor.setXsensDotMfmCallback(this)
    }

    private fun initView() {
        mBinding.startMfm.setOnClickListener(this)

        context?.let {
            val layoutManager = LinearLayoutManager(it)
            layoutManager.orientation = RecyclerView.VERTICAL
            mBinding.list.layoutManager = layoutManager
            mBinding.list.itemAnimator = DefaultItemAnimator()
            mAdapter = EasyMfmAdapter(it, mMfmInfos, this)
            mBinding.list.adapter = mAdapter
        }
    }

    /**
     * Initialize view model.
     */
    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = SensorViewModel.getInstance(requireActivity())
            mSensorViewModel.setDataChangeCallback(null)
        }
    }

    override fun onDetach() {
        super.onDetach()

        // It's necessary to update this status, because user may enter this page again.
        mSensorViewModel.updateStreamingStatus(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mTimeoutHandler.removeAllMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelMfm()
        resetSensorsToCurrentProfile()
    }

    fun isCancelable(isExit: Boolean): Boolean {
        return if (mButtonStatus == ButtonStatus.EXIT) {
            if (isExit) {
                requireActivity().onBackPressed()
            }
            false
        } else {
            Toast.makeText(context, getString(R.string.mfm_processing_hint), Toast.LENGTH_SHORT)
                .show()
            true
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.start_mfm -> {
                    when (mButtonStatus) {
                        ButtonStatus.COMPLETED -> startMfm()
                        ButtonStatus.CANCELABLE -> cancelMfm()
                        ButtonStatus.NOT_CANCELABLE -> {
                        }
                        ButtonStatus.EXIT -> {
                            isCancelable(true)
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun startMfm() {
        mMfmManagers.clear()
        mMfmProcessor.clear()
        mButtonStatus = ButtonStatus.CANCELABLE

        for (info in mMfmInfos) {
            val address = info.dotDevice.address
            mAdapter.updateStatus(address, MfmStatus.STARTING.ordinal)
            mAdapter.updatePercentage(address, 0)

            mMfmManagers[address] = XsensDotMfmManager(requireContext(), info.dotDevice, this@EasyMfmFragment)
        }

        mBinding.tips.visibility = View.VISIBLE
        mBinding.startMfm.isEnabled = true
        mBinding.startMfm.text = getString(R.string.stop_mfm)

        for (info in mMfmInfos) {
            lifecycleScope.launch {
                val sleep = async(Dispatchers.IO) {
                    delay(30)
                }
                sleep.await()

                val address = info.dotDevice.address
                val isSuccess = mMfmManagers[address]?.startMfm() ?: false

                if (isSuccess)
                    mAdapter.updateStatus(address, MfmStatus.STARTED.ordinal)
                else
                    mAdapter.updateStatus(address, MfmStatus.START_FAILED.ordinal)
            }
        }

        mAdapter.notifyDataSetChanged()
        checkButtonStatus()

        mTimeoutHandler.removeAllMessages()
        mTimeoutHandler.sendEmptyMessageDelayed(MSG_WHAT_MFM_TIMEOUT, MFM_TIMEOUT_SECONDS * 1000L)
    }

    private fun stopMfm(address: String) {
        // Use XsensDotMfmProcessor to process data, so don't need to wait for result.
        mMfmManagers[address]?.stopMfm(false)
    }

    private fun cancelMfm() {
        mTimeoutHandler.removeAllMessages()

        for (info in mMfmInfos) {
            val device = info.dotDevice
            // It's better to update status here, because the stopMfm() in SDK takes too much time.
            // Cause the onMfmPercentageChanged() is triggered and make button status to a abnormal state.
            mAdapter.updateStatus(device.address, MfmStatus.STOPPED.ordinal)
        }

        for (info in mMfmInfos) {
            val device = info.dotDevice
            if (device.measurementState == MEASUREMENT_STATE_ON) {
                // No matter success or fail by sending this command, it'a all stopped.
                mMfmManagers[device.address]?.stopMfm(false)
            }

            mMfmManagers[device.address]?.clear()
        }

        mMfmManagers.clear()
        mMfmProcessor.clear()
        mAdapter.notifyDataSetChanged()
        checkButtonStatus()
    }

    private fun resetSensorsToCurrentProfile() {
        for (info in mMfmInfos) {
            val device = info.dotDevice

            resetSensorToCurrentProfile(device)
        }
    }

    /**
     * Reset single sensor's filter profile to current.
     */
    private fun resetSensorToCurrentProfile(device: XsensDotDevice): Boolean = runBlocking {
        var isSuccess = true

        withContext(Dispatchers.IO) {
            val profileIndex = device.currentFilterProfileIndex

            delay(30)

            isSuccess = device.setFilterProfile(profileIndex)
        }

        Log.d(TAG, "resetSensorToCurrentProfile() - address = ${device.address}, $isSuccess")

        isSuccess
    }

    private fun startProcessing(address: String) {
        val path = mMfmManagers[address]?.mtbFilePath ?: ""

        if (path.isNotEmpty()) {
            mMfmProcessor.addMtbFile(address, path)
            mMfmProcessor.startProcess()
        }
    }

    private fun writeMfmParams(address: String, mtb: ByteArray) {
        val isSuccess = mMfmManagers[address]?.writeMfmResultToDevice(mtb) ?: false

        if (isSuccess)
            mAdapter.updateStatus(address, MfmStatus.FINISHED.ordinal)
        else
            mAdapter.updateStatus(address, MfmStatus.WRITE_FAILED.ordinal)

        activity?.runOnUiThread { mAdapter.updatePosition(address) }
        checkButtonStatus()
    }

    private fun checkButtonStatus(): ButtonStatus {
        var cancelable = 0
        var notCancelable = 0
        var done = 0

        for (info in mMfmInfos) {
            when (info.status) {
                MfmStatus.STARTING.ordinal,
                MfmStatus.STARTED.ordinal -> cancelable++

                MfmStatus.PROCESSING.ordinal,
                MfmStatus.WRITING.ordinal -> notCancelable++

                MfmStatus.FINISHED.ordinal,
                MfmStatus.DISCONNECTED.ordinal,
                MfmStatus.STOPPED.ordinal,
                MfmStatus.START_FAILED.ordinal,
                MfmStatus.PROCESS_FAILED.ordinal,
                MfmStatus.WRITE_FAILED.ordinal -> done++
            }
        }

        requireActivity().runOnUiThread {
            mBinding.startMfm.text = getString(R.string.stop_mfm) // For re-write action.

            if (notCancelable != 0) {
                mBinding.startMfm.isEnabled = false
                mButtonStatus = ButtonStatus.NOT_CANCELABLE

            } else {
                mBinding.startMfm.isEnabled = true
                mButtonStatus = ButtonStatus.CANCELABLE
            }

            if (done == mMfmInfos.size) {
                mBinding.startMfm.isEnabled = true
                mBinding.startMfm.text = getString(R.string.exit_mfm)
                mButtonStatus = ButtonStatus.EXIT
            }
        }

        return mButtonStatus
    }

    private fun checkTimeout() {
        for (info in mMfmInfos) {
            val address = info.dotDevice.address
            val status = info.status

            if (status == MfmStatus.STARTED.ordinal) {
                // If the status isn't MfmStatus.PROCESSING after 5 mins, set it to fail.
                mAdapter.updateStatus(address, MfmStatus.PROCESS_FAILED.ordinal)
                activity?.runOnUiThread { mAdapter.updatePosition(address) }
                stopMfm(address)
            }
        }

        checkButtonStatus()
    }

    private fun onMfmPercentageChanged(address: String, percentage: Int) {
        Log.i(TAG, "onMfmPercentageChanged() - address = $address, percentage = $percentage")

        val currentStatus = mAdapter.getStatus(address)
        currentStatus?.let {
            // A workaround if the percentage is still updated after stopped.
            if (it == MfmStatus.STOPPED.ordinal) return
        }

        mAdapter.updatePercentage(address, percentage)

        if (percentage == 100) {
            activity?.runOnUiThread {
                stopMfm(address)
                // Need to run on UI thread.
                startProcessing(address)
            }

            mAdapter.updateStatus(address, MfmStatus.PROCESSING.ordinal)
        }

        // The item view will blinking when update single item.
        // activity?.runOnUiThread { mAdapter.updatePosition(address) }
        activity?.runOnUiThread { mAdapter.notifyDataSetChanged() }
        checkButtonStatus()
    }

    private fun onMtbDataChanged(result: MfmResult) {
        Log.i(TAG, "onMtbDataChanged() - address = ${result.address}")

        for (info in mMfmInfos) {
            if (info.dotDevice.address == result.address) {
                info.result = result
                break
            }
        }

        when (result.result) {
            XsensDotMfmResult.NOT_AVAILABLE,
            XsensDotMfmResult.FAILED,
            XsensDotMfmResult.BAD -> {
                mAdapter.updateStatus(result.address, MfmStatus.PROCESS_FAILED.ordinal)
                activity?.runOnUiThread { mAdapter.updatePosition(result.address) }
                checkButtonStatus()
            }

            XsensDotMfmResult.ACCEPTABLE,
            XsensDotMfmResult.GOOD -> {
                mAdapter.updateStatus(result.address, MfmStatus.WRITING.ordinal)
                activity?.runOnUiThread { mAdapter.updatePosition(result.address) }
                checkButtonStatus()
                writeMfmParams(result.address, result.mtbData!!)
            }
        }
    }

    override fun reWrite(address: String) {
        for (info in mMfmInfos) {
            if (info.dotDevice.address == address) {
                val result = info.result
                result?.let {

                    if (it.mtbData != null) {
                        // No need to notify changes.
                        mAdapter.updateStatus(it.address, MfmStatus.WRITING.ordinal)
                        checkButtonStatus()
                        writeMfmParams(address, it.mtbData)
                    }
                }
                break
            }
        }
    }

    class TimeoutHandler internal constructor(private val mFragment: EasyMfmFragment) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_WHAT_MFM_TIMEOUT -> mFragment.checkTimeout()
            }
        }

        fun removeAllMessages() {
            removeMessages(MSG_WHAT_MFM_TIMEOUT)
        }
    }

    override fun onXsensDotMfmCompleted(address: String?, result: Int, mtbData: ByteArray?) {
        address?.let {
            this.onMtbDataChanged(MfmResult(it, result, mtbData))
        }
    }

    override fun onXsensDotMfmProgressChanged(address: String?, percentage: Int) {
        address?.let {
            this.onMfmPercentageChanged(address, percentage)
        }
    }
}
