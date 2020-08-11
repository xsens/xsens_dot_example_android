package com.xsens.dot.android.example.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

public class Utils {

    public static boolean isBluetoothAdapterEnabled(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

            if (bluetoothAdapter != null) {
                return bluetoothAdapter.isEnabled();
            }
        }

        return false;
    }

}
