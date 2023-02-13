package com.xsens.dot.android.example.views

import XsRecordingFileInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.adapters.ExportAdapter
import com.xsens.dot.android.example.databinding.FragmentExportBinding
import com.xsens.dot.android.example.interfaces.FileSelectionCallback
import com.xsens.dot.android.example.viewmodels.SensorViewModel
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotRecordingCallback
import com.xsens.dot.android.sdk.models.XsensDotDevice
import com.xsens.dot.android.sdk.models.XsensDotRecordingFileInfo
import com.xsens.dot.android.sdk.models.XsensDotRecordingState
import com.xsens.dot.android.sdk.recording.XsensDotRecordingManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [ExportFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExportFragment : Fragment(), XsensDotRecordingCallback, FileSelectionCallback {


    private var isExporting: Boolean = false
    private var mSensorsInProgressCount: Int = 0
    private var mSensorsTotalExportingCount: Int = 0
    private var mCurrentExportingDir: String = ""
    private var mSelectExportedDataIds: ByteArray? = null
    private val mExportingDeviceList: ArrayList<XsensDotDevice> = ArrayList()
    private val mExportingInProgressDeviceList: ArrayList<String> = ArrayList()
    private val mExportingFailedDeviceList: ArrayList<String> = ArrayList()
    private lateinit var mExportBinding: FragmentExportBinding
    private var mFlashInfoCounter: Int = 0
    private val mRecordingDataList: ArrayList<RecordingData> = ArrayList()
    private val mCheckedFileInfoMap = HashMap<String, ArrayList<XsRecordingFileInfo>>()

    //Recording Manager
    private var mRecordingManagers: HashMap<String, RecordingData> = HashMap()

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val address = data.getStringExtra(KEY_ADDRESS)
            val selectList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableArrayListExtra(KEY_SELECT_FILE_LIST_RESULT, XsRecordingFileInfo::class.java)
            } else {
                data.getParcelableArrayListExtra<XsRecordingFileInfo>(KEY_SELECT_FILE_LIST_RESULT)
            }
            address?.let {
                mCheckedFileInfoMap[it] = selectList ?: ArrayList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mExportBinding = FragmentExportBinding.inflate(LayoutInflater.from(context))

        initView()
        return mExportBinding!!.root
    }

    private fun initView() {
        mExportBinding?.let { binding ->
            binding.rcvDevices.layoutManager = LinearLayoutManager(context)
            binding.rcvDevices.adapter = ExportAdapter(mRecordingDataList, mCheckedFileInfoMap, this)
            binding.btnStartStopExport.setOnClickListener {
                if (!isExporting) {
                    startExporting()
                } else {
                    stopExporting()
                }
            }
        }
        enableDataRecordingNotification()
    }

    override fun onFileSelectionClick(address: String) {
        getRecordingDataFromList(address)?.let {
            if (it.recordingFileInfoList.isNotEmpty()) {
                val checkedList: ArrayList<XsRecordingFileInfo> = mCheckedFileInfoMap[address] ?: ArrayList()
                val intent = Intent(context, RecordingFileSelectionActivity::class.java)
                intent.putExtra(KEY_TITLE, tag)
                intent.putExtra(KEY_ADDRESS, address)
                intent.putExtra(KEY_FILE_INFO_LIST, it.recordingFileInfoList)
                intent.putExtra(KEY_CHECKED_FILE_INFO_LIST, checkedList)
                startForResult.launch(intent)
            }
        }
    }

    companion object {
        val TAG: String = "EXPORT_FRAGMENT"
        const val KEY_SELECT_FILE_LIST_RESULT = "select_file_list"
        const val KEY_TITLE = "key_title"
        const val KEY_ADDRESS = "key_address"
        const val KEY_FILE_INFO_LIST = "file_info_list"
        const val KEY_CHECKED_FILE_INFO_LIST = "checked_file_info_list"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ExportFragment.
         */
        @JvmStatic
        fun newInstance() = ExportFragment().apply {
            arguments = Bundle().apply {}
        }
    }

    //region Override methods
    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearRecordingManagers()
    }

    override fun onResume() {
        super.onResume()
        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = MainActivity.FRAGMENT_TAG_EXPORT
        if (activity != null) requireActivity().invalidateOptionsMenu()
    }
    //endregion

    //region List methods

    private fun getXsensDotDeviceByAddress(address: String): XsensDotDevice? {
        getRecordingDataFromList(address)?.let { data ->
            return data.device
        }
        return null
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
                    mExportBinding.rcvDevices.adapter?.notifyItemChanged(index)
                }
            }
        }
    }
    //endregion

    //region Private methods
    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = SensorViewModel.getInstance(activity!!)
        }
    }

    private fun clearRecordingManagers() {
        for ((_, manager) in mRecordingManagers) {
            manager.recordingManager.clear()
        }
        mRecordingManagers.clear()
    }

    private fun enableDataRecordingNotification() {

        if (!mSensorViewModel!!.checkConnection()) {
            Toast.makeText(context, getString(R.string.hint_check_connection), Toast.LENGTH_LONG).show()
            return
        } else {
            mSensorViewModel!!.allSensors?.let {
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

                mExportBinding.rcvDevices.adapter?.notifyDataSetChanged()
            }
        }
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

    //endregion

    //region Exporting
    private fun startExporting() {
        context?.let {
            mExportingDeviceList.clear()


            if (mSelectExportedDataIds == null) {
                mSelectExportedDataIds = byteArrayOf(
                    XsensDotRecordingManager.RECORDING_DATA_ID_TIMESTAMP,
                    XsensDotRecordingManager.RECORDING_DATA_ID_EULER_ANGLES,
                    XsensDotRecordingManager.RECORDING_DATA_ID_CALIBRATED_ACC,
                    XsensDotRecordingManager.RECORDING_DATA_ID_CALIBRATED_GYR
                )
            }

            mSelectExportedDataIds?.let { ids ->
                val isSuccess = selectExportedData(ids)

                if (isSuccess) {
                    val map = HashMap<String, ArrayList<XsensDotRecordingFileInfo>>()

                    for ((key, value) in mCheckedFileInfoMap.entries) {
                        val infoList = ArrayList<XsensDotRecordingFileInfo>()
                        for (info in value) {
                            val newInfo = XsensDotRecordingFileInfo(info.id, info.fileName, info.size)
                            newInfo.startRecordingTimestamp = info.startRecordingTimestamp
                            infoList.add(newInfo)
                        }

                        map[key] = infoList

                        // Find device if it selected files to export
                        if (infoList.size > 0) {
                            val device = getXsensDotDeviceByAddress(key)
                            device?.let { xsDevice ->
                                mExportingDeviceList.add(xsDevice)
                            }
                        }
                    }

                    if (mExportingDeviceList.size == 0) return

                    mCurrentExportingDir = makeExportDir()

                    if (mCurrentExportingDir.isEmpty()) return

                    // Start exporting
                    for ((address, recordingData) in mRecordingManagers.entries) {
                        val manager = recordingData.recordingManager
                        map[address]?.let { fileList ->
                            if (fileList.size > 0) {
                                mSensorsTotalExportingCount++

                                var isStartSuccess = manager.startExporting(fileList)

                                Log.d(TAG, "startExporting $address, success $isSuccess")

                                if (!isStartSuccess) {
                                    SystemClock.sleep(30)
                                    isStartSuccess = manager.startExporting(fileList)

                                    Log.d(TAG, "startExporting retry $address, success $isSuccess")
                                }

                                if (isStartSuccess) {
                                    mSensorsInProgressCount++
                                    mExportingInProgressDeviceList.add(address)
                                } else {
                                    mExportingFailedDeviceList.add(address)
                                }
                            }
                        }
                    }

                } else {
//                    mExportingState = EXPORTING_STATE_IDLE
//                    updateUI()
                }
            }
        }
    }

    private fun stopExporting() {
        for ((address, data) in mRecordingManagers.entries) {
            val manager = data.recordingManager
            var isSuccess = manager.stopExporting()

            Log.d(TAG, "stopExporting $address, success $isSuccess")

            if (!isSuccess) {
                SystemClock.sleep(30)
                isSuccess = manager.stopExporting()

                Log.d(TAG, "stopExporting retry $address, success $isSuccess")
            }

            if (!isSuccess) {
//                mBinding.listViewExportingDevice.adapter?.let {
//                    (it as DataExportingDevicesAdapter).updateStopExportingFailed(address)
//                }
            }
        }
    }

    private fun makeExportDir(): String {
        context?.let { ctx ->
            val root = ctx.applicationContext.getExternalFilesDir(null)

            if (root != null) {
                val dir = File(
                    root.toString() + File.separator + "recordings"
                            + File.separator + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
                )

                if (mkdirs(dir.toString())) {
                    return dir.toString()
                }
            }
        }

        return ""
    }

    private fun mkdirs(path: String): Boolean {
        val file = File(path)
        return if (!file.exists()) file.mkdirs() else true
    }

    private fun selectExportedData(ids: ByteArray): Boolean {
        var hasSuccess = false

        for ((address, recordingData) in mRecordingManagers.entries) {
            val manager = recordingData.recordingManager
            var isSuccess = manager.selectExportedData(ids)

            Log.d(TAG, "selectExportedData $address, success $isSuccess")

            if (!isSuccess) {
                SystemClock.sleep(30)
                isSuccess = manager.selectExportedData(ids)

                Log.d(TAG, "selectExportedData retry $address, success $isSuccess")
            }

            if (isSuccess) {
                hasSuccess = isSuccess
            }
        }

        return hasSuccess
    }
    //endregion


    //region Callback Methods

    override fun onXsensDotRecordingNotification(address: String?, isEnabled: Boolean) {
        address?.let {
            mRecordingManagers[it]?.isNotificationEnabled = isEnabled
            if (isEnabled) {
                SystemClock.sleep(30)
                mRecordingManagers[it]?.recordingManager?.requestFlashInfo()
            }
        }
    }

    override fun onXsensDotEraseDone(p0: String?, p1: Boolean) {

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
                requestRecordFileInfo()

                activity?.let { act ->
                    act.runOnUiThread {
//
                    }
                }
            }
        }
    }

    override fun onXsensDotRecordingAck(p0: String?, p1: Int, p2: Boolean, p3: XsensDotRecordingState?) {

    }

    override fun onXsensDotGetRecordingTime(p0: String?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onXsensDotRequestFileInfoDone(address: String?, fileList: ArrayList<XsensDotRecordingFileInfo>?, isSuccess: Boolean) {
        address?.let {
            getRecordingDataFromList(it)?.let { recordingData ->
                val recordingFileInfoList: ArrayList<XsRecordingFileInfo> = ArrayList()
                fileList?.let { list ->
                    for (i in (list.size - 1) downTo 0) {
                        val element = list[i]
                        recordingFileInfoList.add(XsRecordingFileInfo(address, element.fileId, element.fileName, element.dataSize, element.startRecordingTimestamp))
                    }
                }
                recordingData.recordingFileInfoList = recordingFileInfoList
            }
            updateRecyclerViewItemByAddress(it)
        }
    }

    override fun onXsensDotDataExported(address: String?, fileInfo: XsensDotRecordingFileInfo?, p2: XsensDotData?) {
        address?.let {
            fileInfo?.let { }
        }
    }

    override fun onXsensDotDataExported(address: String?, fileInfo: XsensDotRecordingFileInfo?) {
        address?.let {
            fileInfo?.let { }
        }
    }

    override fun onXsensDotAllDataExported(address: String?) {
        address?.let {
        }
    }

    override fun onXsensDotStopExportingData(address: String?) {
        address?.let { }
    }
    //endregion
}