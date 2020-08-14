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

package com.xsens.dot.android.example.viewmodels;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.xsens.dot.android.sdk.events.XsensDotData;
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCb;
import com.xsens.dot.android.sdk.models.XsensDotDevice;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

public class SensorViewModel extends ViewModel implements XsensDotDeviceCb {

    private static final String TAG = SensorViewModel.class.getSimpleName();

    public static SensorViewModel getInstance(@NonNull ViewModelStoreOwner owner) {

        return new ViewModelProvider(owner, new ViewModelProvider.NewInstanceFactory()).get(SensorViewModel.class);
    }

    private MutableLiveData<ArrayList<XsensDotDevice>> mSensorList = new MutableLiveData<>();

    public void connectSensor(Context context, BluetoothDevice device) {

        XsensDotDevice xsDevice = new XsensDotDevice(context, device, this);
        xsDevice.connect();

        if (mSensorList.getValue() == null) mSensorList.setValue(new ArrayList<XsensDotDevice>());
        mSensorList.getValue().add(xsDevice);
    }

    public void disconnectSensor(String address) {

        if (mSensorList.getValue() != null) {

            for (XsensDotDevice device : mSensorList.getValue()) {

                if (device.getAddress().equals(address)) {

                    device.disconnect();
                    break;
                }
            }
        }
    }

    public void disconnectAllSensors() {

        if (mSensorList.getValue() != null) {

            for (XsensDotDevice device : mSensorList.getValue()) {

                device.disconnect();
            }
        }
    }

    @Override
    public void onXsensDotConnectionChanged(String address, int state) {

        Log.i(TAG, "onXsensDotConnectionChanged() - address = " + address + ", state = " + state);
    }

    @Override
    public void onXsensDotServicesDiscovered(String address, int status) {

        Log.i(TAG, "onXsensDotServicesDiscovered() - address = " + address + ", status = " + status);
    }

    @Override
    public void onXsensDotFirmwareVersionRead(String address, String version) {

        Log.i(TAG, "onXsensDotFirmwareVersionRead() - address = " + address + ", version = " + version);
    }

    @Override
    public void onXsensDotTagChanged(String s, String s1) {

    }

    @Override
    public void onXsensDotBatteryChanged(String s, int i, int i1) {

    }

    @Override
    public void onXsensDotDataChanged(String s, XsensDotData xsensDotData) {

    }

    @Override
    public void onXsensDotCalibrationResult(String s, int i, int i1, int i2) {

    }

    @Override
    public void onXsensDotOtaChecked(String s, boolean b, String s1, String s2) {

    }

    @Override
    public void onXsensDotOtaRollback(String s, boolean b, String s1, String s2) {

    }

    @Override
    public void onXsensDotOtaFileMismatch(String s) {

    }

    @Override
    public void onXsensDotOtaDownloaded(String s, int i) {

    }

    @Override
    public void onXsensDotOtaUpdated(String s, int i, int i1, int i2, int i3, int i4) {

    }

    @Override
    public void onXsensDotNewFirmwareVersion(String s, boolean b, String s1, String s2) {

    }

    @Override
    public void onXsensDotOtaDischarge(String s) {

    }
}