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

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.xsens.dot.android.example.interfaces.BatteryChangedInterface;
import com.xsens.dot.android.example.interfaces.DataChangeInterface;
import com.xsens.dot.android.sdk.events.XsensDotData;
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCallback;
import com.xsens.dot.android.sdk.models.FilterProfileInfo;
import com.xsens.dot.android.sdk.models.XsensDotDevice;

import java.util.ArrayList;
import java.util.Iterator;

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

    // A variable to queue multiple threads.
    private static final Object LOCKER = new Object();

    // A callback function to notify battery information
    private BatteryChangedInterface mBatteryChangeInterface;
    // A callback function to notify data changes event
    private DataChangeInterface mDataChangeInterface;

    // A list contains XsensDotDevice
    private MutableLiveData<ArrayList<XsensDotDevice>> mSensorList = new MutableLiveData<>();
    // A variable to notify the connection state
    private MutableLiveData<XsensDotDevice> mConnectionChangedSensor = new MutableLiveData<>();
    // A variable to notify the tag name
    private MutableLiveData<XsensDotDevice> mTagChangedSensor = new MutableLiveData<>();
    // A variable to notify the streaming status
    private MutableLiveData<Boolean> mIsStreaming = new MutableLiveData<>();

    /**
     * Initialize data changes interface.
     *
     * @param callback The class which implemented DataChangeInterface
     */
    public void setDataChangeCallback(DataChangeInterface callback) {

        mDataChangeInterface = callback;
    }

    /**
     * Initialize battery changes interface.
     *
     * @param callback The class which implemented setBatteryChangedCallback
     */
    public void setBatteryChangedCallback(BatteryChangedInterface callback) {

        mBatteryChangeInterface = callback;
    }

    /**
     * Get the XsensDotDevice object from list by mac address.
     *
     * @param address The mac address of device
     * @return The XsensDotDevice object
     */
    public XsensDotDevice getSensor(String address) {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null) {

            for (XsensDotDevice device : devices) {

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
        addDevice(xsDevice);
        xsDevice.connect();
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

            synchronized (LOCKER) {

                for (Iterator<XsensDotDevice> it = mSensorList.getValue().iterator(); it.hasNext(); ) {
                    // Use Iterator to make sure it's thread safety.
                    XsensDotDevice device = it.next();
                    device.disconnect();
                }
            }
        }
    }

    /**
     * Cancel reconnection of one sensor.
     *
     * @param address The mac address of device
     */
    public void cancelReconnection(String address) {

        if (mSensorList.getValue() != null) {

            for (XsensDotDevice device : mSensorList.getValue()) {

                if (device.getAddress().equals(address)) {

                    device.cancelReconnecting();
                    break;
                }
            }
        }
    }

    /**
     * Check the connection state of all sensors.
     *
     * @return True - If all sensors are connected
     */
    public boolean checkConnection() {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null) {

            for (XsensDotDevice device : devices) {

                final int state = device.getConnectionState();
                if (state != CONN_STATE_CONNECTED) return false;
            }

        } else {

            return false;
        }

        return true;
    }

    /**
     * Get the tag name from sensor.
     *
     * @param address The mac address of device
     * @return The tag name
     */
    public String getTag(String address) {

        XsensDotDevice device = getSensor(address);

        if (device != null) {

            String tag = device.getTag();
            return tag == null ? device.getName() : tag;
        }

        return "";
    }

    /**
     * Set the plotting and logging states for each device.
     *
     * @param plot The plot state
     * @param log  The log state
     */
    public void setStates(int plot, int log) {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null) {

            for (XsensDotDevice device : devices) {

                device.setPlotState(plot);
                device.setLogState(log);
            }
        }
    }

    /**
     * Set the measurement mode to all sensors.
     *
     * @param mode The measurement mode
     */
    public void setMeasurementMode(int mode) {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null) {

            for (XsensDotDevice device : devices) {

                device.setMeasurementMode(mode);
            }
        }
    }

    /**
     * Set one sensor for root of synchronization.
     *
     * @param isRoot True - If set to root
     */
    public void setRootDevice(boolean isRoot) {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null && devices.size() > 0) devices.get(0).setRootDevice(isRoot);
    }

    /**
     * Start/Stop measuring for each sensor.
     *
     * @param enabled True - Start outputting data
     */
    public void setMeasurement(boolean enabled) {

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();

        if (devices != null) {

            for (XsensDotDevice device : devices) {

                if (enabled) device.startMeasuring();
                else device.stopMeasuring();
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

        final ArrayList<XsensDotDevice> devices = mSensorList.getValue();
        boolean isExist = false;

        for (XsensDotDevice _xsDevice : devices) {

            if (xsDevice.getAddress().equals(_xsDevice.getAddress())) {

                isExist = true;
                break;
            }
        }

        if (!isExist) devices.add(xsDevice);
    }

    /**
     * If device is disconnected by user means don't need to reconnect. So remove this device from list by mac address.
     *
     * @param address The mac address of device
     */
    public void removeDevice(String address) {

        if (mSensorList.getValue() == null) {

            mSensorList.setValue(new ArrayList<XsensDotDevice>());
            return;
        }

        synchronized (LOCKER) {

            for (Iterator<XsensDotDevice> it = mSensorList.getValue().iterator(); it.hasNext(); ) {
                // Use Iterator to make sure it's thread safety.
                XsensDotDevice device = it.next();
                if (device.getAddress().equals(address)) {

                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Remove all sensor from device list directly.
     */
    public void removeAllDevice() {

        if (mSensorList.getValue() != null) {

            synchronized (LOCKER) {

                mSensorList.getValue().clear();
            }
        }
    }

    /**
     * Observe this function to listen which device's connection state is changed.
     *
     * @return The latest updated device
     */
    public MutableLiveData<XsensDotDevice> getConnectionChangedDevice() {

        return mConnectionChangedSensor;
    }

    /**
     * Observe this function to listen which device's tag name is changed.
     *
     * @return The latest updated device
     */
    public MutableLiveData<XsensDotDevice> getTagChangedDevice() {

        return mTagChangedSensor;
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
        if (xsDevice != null) mConnectionChangedSensor.postValue(xsDevice);

        switch (state) {

            case CONN_STATE_DISCONNECTED:

                synchronized (this) {
                    removeDevice(address);
                }
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
    public void onXsensDotTagChanged(String address, String tag) {
        // This callback function will be triggered in the connection precess.
        Log.i(TAG, "onXsensDotTagChanged() - address = " + address + ", tag = " + tag);

        // The default value of tag is an empty string.
        if (!tag.equals("")) {

            XsensDotDevice device = getSensor(address);
            if (device != null) mTagChangedSensor.postValue(device);
        }
    }

    @Override
    public void onXsensDotBatteryChanged(String address, int status, int percentage) {
        // This callback function will be triggered in the connection precess.
        Log.i(TAG, "onXsensDotBatteryChanged() - address = " + address + ", status = " + status + ", percentage = " + percentage);

        // The default value of status and percentage is -1.
        if (status != -1 && percentage != -1) {
            // Use callback function instead of LiveData to notify the battery information.
            // Because when user removes the USB cable from housing, this function will be triggered 5 times.
            // Use LiveData will lose some notification.
            if (mBatteryChangeInterface != null) mBatteryChangeInterface.onBatteryChanged(address, status, percentage);
        }
    }

    @Override
    public void onXsensDotDataChanged(String address, XsensDotData data) {

        Log.i(TAG, "onXsensDotDataChanged() - address = " + address);

        // Don't use LiveData variable to transfer data to activity/fragment.
        // The main (UI) thread isn't fast enough to store data by 60Hz.
        if (mDataChangeInterface != null) mDataChangeInterface.onDataChanged(address, data);
    }

    @Override
    public void onXsensDotInitDone(String address) {

        Log.i(TAG, "onXsensDotInitDone() - address = " + address);
    }

    @Override
    public void onXsensDotButtonClicked(String address, long timestamp) {

        Log.i(TAG, "onXsensDotButtonClicked() - address = " + address + ", timestamp = " + timestamp);
    }

    @Override
    public void onXsensDotPowerSavingTriggered(String address) {

        Log.i(TAG, "onXsensDotPowerSavingTriggered() - address = " + address);
    }

    @Override
    public void onReadRemoteRssi(String address, int rssi) {

        Log.i(TAG, "onReadRemoteRssi() - address = " + address + ", rssi = " + rssi);
    }

    @Override
    public void onXsensDotOutputRateUpdate(String address, int outputRate) {

        Log.i(TAG, "onXsensDotOutputRateUpdate() - address = " + address + ", outputRate = " + outputRate);
    }

    @Override
    public void onXsensDotFilterProfileUpdate(String address, int filterProfileIndex) {

        Log.i(TAG, "onXsensDotFilterProfileUpdate() - address = " + address + ", filterProfileIndex = " + filterProfileIndex);
    }

    @Override
    public void onXsensDotGetFilterProfileInfo(String address, ArrayList<FilterProfileInfo> filterProfileInfoList) {

        Log.i(TAG, "onXsensDotGetFilterProfileInfo() - address = " + address + ", size = " + filterProfileInfoList.size());
    }

    @Override
    public void onSyncStatusUpdate(String address, boolean isSynced) {

        Log.i(TAG, "onSyncStatusUpdate() - address = " + address + ", isSynced = " + isSynced);
    }
}