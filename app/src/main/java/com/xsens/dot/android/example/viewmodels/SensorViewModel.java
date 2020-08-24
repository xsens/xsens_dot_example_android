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
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCallback;
import com.xsens.dot.android.sdk.models.XsensDotDevice;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_DISCONNECTED;

public class SensorViewModel extends ViewModel implements XsensDotDeviceCallback {

    private static final String TAG = SensorViewModel.class.getSimpleName();

    public static SensorViewModel getInstance(@NonNull ViewModelStoreOwner owner) {

        return new ViewModelProvider(owner, new ViewModelProvider.NewInstanceFactory()).get(SensorViewModel.class);
    }

    private MutableLiveData<ArrayList<XsensDotDevice>> mSensorList = new MutableLiveData<>();
    private MutableLiveData<XsensDotDevice> mConnectionUpdatedSensor = new MutableLiveData<>();

    public XsensDotDevice getSensor(String address) {

        final ArrayList<XsensDotDevice> list = mSensorList.getValue();

        if (list != null) {

            for (XsensDotDevice device : list) {

                if (device.getAddress().equals(address)) return device;
            }
        }

        return null;
    }

    public void connectSensor(Context context, BluetoothDevice device) {

        XsensDotDevice xsDevice = new XsensDotDevice(context, device, this);
        xsDevice.connect();
        addDevice(xsDevice);
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

    private void addDevice(XsensDotDevice xsDevice) {

        if (mSensorList.getValue() == null) mSensorList.setValue(new ArrayList<XsensDotDevice>());

        final ArrayList<XsensDotDevice> list = mSensorList.getValue();
        boolean isExist = false;

        for (XsensDotDevice _xsDevice : list) {

            if (xsDevice.getAddress().equals(_xsDevice.getAddress())) {

                isExist = true;
                break;
            }
        }

        if (!isExist) list.add(xsDevice);
    }

    private void removeDevice(String address) {

        if (mSensorList.getValue() == null) {

            mSensorList.setValue(new ArrayList<XsensDotDevice>());
            return;
        }

        final ArrayList<XsensDotDevice> list = mSensorList.getValue();
        final XsensDotDevice xsDevice = getSensor(address);

        if (xsDevice != null) {

            for (XsensDotDevice _xsDevice : list) {

                if (xsDevice.getAddress().equals(_xsDevice.getAddress())) {

                    list.remove(_xsDevice);
                    break;
                }
            }
        }
    }

    public MutableLiveData<XsensDotDevice> getConnectionUpdatedDevice() {

        return mConnectionUpdatedSensor;
    }

    @Override
    public void onXsensDotConnectionChanged(String address, int state) {

        Log.i(TAG, "onXsensDotConnectionChanged() - address = " + address + ", state = " + state);

        final XsensDotDevice xsDevice = getSensor(address);
        if (xsDevice != null) mConnectionUpdatedSensor.postValue(xsDevice);

        switch (state) {

            case CONN_STATE_DISCONNECTED:
                removeDevice(address);
                break;
        }
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
    public void onXsensDotInitDone(String s) {

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
    public void onXsensDotPowerSavingTriggered(String s) {

    }
}