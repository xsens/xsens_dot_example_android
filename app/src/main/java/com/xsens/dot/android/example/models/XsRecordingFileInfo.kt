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
