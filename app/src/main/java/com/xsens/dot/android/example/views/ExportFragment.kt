package com.xsens.dot.android.example.views

import XsRecordingFileInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
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
import com.xsens.dot.android.sdk.models.XsensDotRecordingFileInfo
import com.xsens.dot.android.sdk.models.XsensDotRecordingState
import com.xsens.dot.android.sdk.recording.XsensDotRecordingManager
import java.util.ArrayList
import java.util.HashMap

/**
 * A simple [Fragment] subclass.
 * Use the [ExportFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExportFragment : Fragment(), XsensDotRecordingCallback, FileSelectionCallback {


    private lateinit var mExportBinding: FragmentExportBinding
    private var mFlashInfoCounter: Int = 0
    private val mRecordingDataList: ArrayList<RecordingData> = ArrayList()

    //Recording Manager
    private var mRecordingManagers: HashMap<String, RecordingData> = HashMap()

    // The devices view model instance
    private var mSensorViewModel: SensorViewModel? = null

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
            //do stuff here
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
            binding.rcvDevices.adapter = ExportAdapter(mRecordingDataList, this)
            binding.btnRquestFileInfo.setOnClickListener {
                requestRecordFileInfo()
            }
        }

        enableDataRecordingNotification()
    }

    override fun onFileSelectionClick(address: String) {
        getRecordingDataFromList(address)?.let {
            if (it.recordingFileInfoList.isNotEmpty()) {
                val checkedList: ArrayList<XsRecordingFileInfo> = ArrayList()
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
//                    recordingManager.clear()
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