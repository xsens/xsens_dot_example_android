
import android.os.Parcel
import android.os.Parcelable

data class XsRecordingFileInfo(
        var address: String? = "",
        var id: Int,
        var fileName: String? = "",
        var size: Int,
        var startRecordingTimestamp: Long
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address)
        parcel.writeInt(id)
        parcel.writeString(fileName)
        parcel.writeInt(size)
        parcel.writeLong(startRecordingTimestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<XsRecordingFileInfo> {
        override fun createFromParcel(parcel: Parcel): XsRecordingFileInfo {
            return XsRecordingFileInfo(parcel)
        }

        override fun newArray(size: Int): Array<XsRecordingFileInfo?> {
            return arrayOfNulls(size)
        }
    }

}
