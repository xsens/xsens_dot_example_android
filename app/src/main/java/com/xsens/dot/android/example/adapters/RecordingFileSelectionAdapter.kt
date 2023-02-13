import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xsens.dot.android.example.R
import java.util.*

class RecordingFileSelectionAdapter(
    list: ArrayList<XsRecordingFileInfo>,
    checkedList: ArrayList<XsRecordingFileInfo>,
    callback: RecordingFileSelectionCallback
) : RecyclerView.Adapter<RecordingFileSelectionAdapter.ConnectedDevicesViewHolder>() {

    private val mConnectedDeviceList: ArrayList<XsRecordingFileInfo> = list
    internal val mCheckedDeviceList: ArrayList<XsRecordingFileInfo> = checkedList
    private var mCallback: RecordingFileSelectionCallback = callback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedDevicesViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_recording_file_selection, parent, false)
        return ConnectedDevicesViewHolder(v)
    }

    override fun onBindViewHolder(holder: ConnectedDevicesViewHolder, position: Int) {
        val info = mConnectedDeviceList[position]
        val fileName = info.fileName
        val size = info.size

        holder.checkBox.isChecked = mCheckedDeviceList.contains(info)

        holder.checkBox.setOnClickListener { v ->
            val isChecked = (v as CheckBox).isChecked
            if (isChecked) {
                mCheckedDeviceList.add(info)
            } else {
                mCheckedDeviceList.remove(info)
            }

            mCallback.onFileSelectionUpdate(mCheckedDeviceList.size)
        }

        holder.fileName.text = fileName
        holder.size.text = formatFileSize(size)
    }

    override fun getItemCount(): Int {
        return mConnectedDeviceList.size
    }

    private fun formatFileSize(size: Int): String {
        return when {
            size < 1024 -> {
                "$size B"
            }

            size < 1024 * 1024 -> {
                "${(size / 1024)} KB"
            }

            else -> {
                "${(size / (1024 * 1024))} MB"
            }
        }
    }

    class ConnectedDevicesViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val fileName: TextView = itemView.findViewById(R.id.textView_file_name)
        val size: TextView = itemView.findViewById(R.id.textView_file_size)
    }

    interface RecordingFileSelectionCallback {
        fun onFileSelectionUpdate(selectedCount: Int)
    }
}