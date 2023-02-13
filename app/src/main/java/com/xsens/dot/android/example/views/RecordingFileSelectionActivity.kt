package com.xsens.dot.android.example.views

import RecordingFileSelectionAdapter
import XsRecordingFileInfo
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.BuildConfig
import com.xsens.dot.android.example.R
import com.xsens.dot.android.example.databinding.ActivityRecordingFileSelectionBinding

class RecordingFileSelectionActivity : AppCompatActivity(), View.OnClickListener, RecordingFileSelectionAdapter.RecordingFileSelectionCallback {

    companion object {
        const val KEY_SELECT_FILE_LIST_RESULT = "select_file_list"
        const val KEY_TITLE = "key_title"
        const val KEY_ADDRESS = "key_address"
        const val KEY_FILE_INFO_LIST = "file_info_list"
        const val KEY_CHECKED_FILE_INFO_LIST = "checked_file_info_list"
    }

    private lateinit var mBinding: ActivityRecordingFileSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        mBinding = ActivityRecordingFileSelectionBinding.inflate(LayoutInflater.from(this))
        setContentView(mBinding.root)
        initView()
    }

    private fun initView() {
        mBinding.title.text = "Export"

        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(KEY_FILE_INFO_LIST, XsRecordingFileInfo::class.java)
        } else {
            intent.getParcelableArrayListExtra<XsRecordingFileInfo>(KEY_FILE_INFO_LIST)
        }
        var checkedList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(KEY_CHECKED_FILE_INFO_LIST, XsRecordingFileInfo::class.java)
        } else {
            intent.getParcelableArrayListExtra<XsRecordingFileInfo>(KEY_CHECKED_FILE_INFO_LIST)
        }
        checkedList = checkedList ?: ArrayList()

        updateFileSelectedString(checkedList.size)

        mBinding.textViewCancel.setOnClickListener(this)
        mBinding.textViewConfirm.setOnClickListener(this)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        mBinding.listViewDevice.layoutManager = layoutManager
        mBinding.listViewDevice.itemAnimator = DefaultItemAnimator()

        list?.let {
            mBinding.listViewDevice.adapter = RecordingFileSelectionAdapter(it, checkedList, this)
        }

    }

    private fun updateFileSelectedString(checkedCount: Int) {
        val fileSelectedString = if (checkedCount > 1) R.string.n_files_selected else R.string.n_file_selected
        mBinding.nFileSelected.text = getString(fileSelectedString, checkedCount)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.textView_cancel) {
            setResult(Activity.RESULT_CANCELED)
            finish()

            return
        } else if (v.id == R.id.textView_confirm) {
            val address = intent.getStringExtra(KEY_ADDRESS)

            val adapter = (mBinding.listViewDevice.adapter as RecordingFileSelectionAdapter)
            val selectList = adapter.mCheckedDeviceList

            val intent = Intent()
            intent.putExtra(KEY_ADDRESS, address)
            intent.putExtra(KEY_SELECT_FILE_LIST_RESULT, selectList)
            setResult(Activity.RESULT_OK, intent)
            finish()

            return
        }

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onFileSelectionUpdate(selectedCount: Int) {
        updateFileSelectedString(selectedCount)
    }
}