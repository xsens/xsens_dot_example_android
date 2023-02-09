package com.xsens.dot.android.example.views

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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RecordingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecordingFragment : Fragment(), XsensDotRecordingCallback {

    private var recordingObserver: Observer<Boolean>? = null
    private var mRecordingBinding: FragmentRecordingBinding? = null
    private var mFlashInfoCounter: Int = 0
    private var mIsRecording: Boolean = false
    private var isRecording: MutableLiveData<Boolean> = MutableLiveData(false)
    private val mRecordingDataList: ArrayList<RecordingData> = ArrayList()

    //Recording Manager
    private var mRecordingManagers: HashMap<String, RecordingData> = HashMap()

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null

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
            binding.btnEnableNotification.setOnClickListener {
                enableDataRecordingNotification()
            }
            binding.btnEnableNotification.visibility = View.GONE
            binding.rcvDevices.layoutManager = LinearLayoutManager(activity)
            binding.rcvDevices.adapter = RecordingAdapter(mRecordingDataList)
            binding.btnStartStopRecording.setOnClickListener {
                if (!mIsRecording) {
                    checkIfCanStartRecording()
                } else {
                    stopRecording()
                }
            }

            binding.btnRquestFileInfo.setOnClickListener {
                requestRecordFileInfo()
            }
        }
        enableDataRecordingNotification()
    }

    private fun requestRecordFileInfo() {
        for ((address, data) in mRecordingManagers) {
            data?.let {
                if (it.isNotificationEnabled) {
                    SystemClock.sleep(30)
                    it.recordingManager.requestFileInfo()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording.removeObserver(recordingObserver!!)
        stopRecording()
    }

    override fun onResume() {
        super.onResume()
        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_RECORD
        if (activity != null) requireActivity().invalidateOptionsMenu()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment RecordingFragment.
         */
        @JvmStatic
        fun newInstance() =
            RecordingFragment()
    }

    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = SensorViewModel.getInstance(activity!!)
        }
    }

    //region Recording Callbacks

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

    private fun clearRecordingManagers() {
        for ((_, manager) in mRecordingManagers) {
            manager.recordingManager.clear()
        }
        mRecordingManagers.clear()
    }

    override fun onXsensDotRecordingNotification(address: String?, isEnabled: Boolean) {
        address?.let {
            mRecordingManagers[it]?.isNotificationEnabled = isEnabled
            if (isEnabled) {
                SystemClock.sleep(30)
                mRecordingManagers[it]?.recordingManager?.requestFlashInfo()
            }
        }
    }

    override fun onXsensDotRequestFlashInfoDone(address: String?, usedFlashSpace: Int, totalFlashSpace: Int) {
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
//        TODO("Not yet implemented")
    }

    override fun onXsensDotDataExported(p0: String?, p1: XsensDotRecordingFileInfo?) {
//        TODO("Not yet implemented")
    }

    override fun onXsensDotAllDataExported(p0: String?) {
//        TODO("Not yet implemented")
    }

    override fun onXsensDotStopExportingData(p0: String?) {
//        TODO("Not yet implemented")
    }

    //endregion
}


data class RecordingData(
    var device: XsensDotDevice,
    var canRecord: Boolean,
    var isNotificationEnabled: Boolean = false,
    var recordingManager: XsensDotRecordingManager,
    var isRecording: Boolean = false
)