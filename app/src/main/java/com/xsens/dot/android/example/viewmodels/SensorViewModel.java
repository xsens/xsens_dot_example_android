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

import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_CONNECTED;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_CONNECTING;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_DISCONNECTED;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_RECONNECTING;

/**
 * A view model class for notifying data to views and handle logic operations.
 */
public class SensorViewModel extends ViewModel implements XsensDotDeviceCallback {

    private static final String TAG = SensorViewModel.class.getSimpleName();

    /**
     * Get the instance of SensorViewModel
     *
     * @param owner The life cycle owner from activity/fragment
     * @return The SensorViewModel
     */
    public static SensorViewModel getInstance(@NonNull ViewModelStoreOwner owner) {

        return new ViewModelProvider(owner, new ViewModelProvider.NewInstanceFactory()).get(SensorViewModel.class);
    }

    // A list contains XsensDotDevice
    private MutableLiveData<ArrayList<XsensDotDevice>> mSensorList = new MutableLiveData<>();
    // A variable to notify the connection state
    private MutableLiveData<XsensDotDevice> mConnectionUpdatedSensor = new MutableLiveData<>();
    // A variable to notify the streaming status
    private MutableLiveData<Boolean> mIsStreaming = new MutableLiveData<>();

    /**
     * Get the XsensDotDevice object from list by mac address.
     *
     * @param address The mac address of device
     * @return The XsensDotDevice object
     */
    public XsensDotDevice getSensor(String address) {

        final ArrayList<XsensDotDevice> list = mSensorList.getValue();

        if (list != null) {

            for (XsensDotDevice device : list) {

                if (device.getAddress().equals(address)) return device;
            }
        }

        return null;
    }

    /**
     * Get all XsensDotDevice objects from list.
     *
     * @return The list contains all devices
     */
    public ArrayList<XsensDotDevice> getAllSensors() {

        if (mSensorList.getValue() == null) return new ArrayList<>();
        else return mSensorList.getValue();
    }

    /**
     * Initialize, connect the XsensDotDevice and put it into a list.
     *
     * @param context The application context
     * @param device  The scanned Bluetooth device
     */
    public void connectSensor(Context context, BluetoothDevice device) {

        XsensDotDevice xsDevice = new XsensDotDevice(context, device, this);
        xsDevice.connect();
        addDevice(xsDevice);
    }

    /**
     * Disconnect one device by mac address.
     *
     * @param address The mac address of device
     */
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

    /**
     * Disconnect all devices which are exist in the list.
     */
    public void disconnectAllSensors() {

        if (mSensorList.getValue() != null) {

            for (XsensDotDevice device : mSensorList.getValue()) {

                device.disconnect();
            }
        }
    }

    /**
     * Add the XsensDotDevice to a list, the UID is mac address.
     *
     * @param xsDevice The XsensDotDevice object
     */
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

    /**
     * If device is disconnected by user means don't need to reconnect. So remove this device from list by mac address.
     *
     * @param address The mac address of device
     */
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

    /**
     * Observe this function to listen which device's connection state is updated.
     *
     * @return The latest updated device
     */
    public MutableLiveData<XsensDotDevice> getConnectionUpdatedDevice() {

        return mConnectionUpdatedSensor;
    }

    /**
     * Observe this function to listen the streaming status.
     *
     * @return The latest streaming status
     */
    public MutableLiveData<Boolean> isStreaming() {

        if (mIsStreaming.getValue() == null) mIsStreaming.setValue(false);
        return mIsStreaming;
    }

    /**
     * Notify the streaming status to activity/fragment
     *
     * @param status The status of streaming
     */
    public void updateStreamingStatus(boolean status) {

        mIsStreaming.postValue(status);
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

            case CONN_STATE_CONNECTING:

                break;

            case CONN_STATE_CONNECTED:

                break;

            case CONN_STATE_RECONNECTING:

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
    public void onXsensDotInitDone(String address) {

        Log.i(TAG, "onXsensDotInitDone() - address = " + address);
    }

    @Override
    public void onXsensDotTagChanged(String address, String tag) {

        Log.i(TAG, "onXsensDotTagChanged() - address = " + address + ", tag = " + tag);
    }

    @Override
    public void onXsensDotBatteryChanged(String address, int status, int percentage) {

        Log.i(TAG, "onXsensDotBatteryChanged() - address = " + address + ", status = " + status + ", percentage = " + percentage);
    }

    @Override
    public void onXsensDotDataChanged(String address, XsensDotData data) {

        Log.i(TAG, "onXsensDotDataChanged() - address = " + address);
    }

    @Override
    public void onXsensDotPowerSavingTriggered(String address) {

        Log.i(TAG, "onXsensDotPowerSavingTriggered() - address = " + address);
    }
}