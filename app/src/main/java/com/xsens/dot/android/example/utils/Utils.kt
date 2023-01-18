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
package com.xsens.dot.android.example.utils

import android.Manifest
import android.os.Looper
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager

/**
 * This class is for some additional feature, such as: check Bluetooth adapter, check location premission...etc.
 */
object Utils {
    /**
     * Check the current thread is main thread or background thread.
     *
     * @return True - If running on main thread
     */
    val isMainThread: Boolean
        get() = Looper.myLooper() == Looper.getMainLooper()

    /**
     * Check the Bluetooth adapter is enabled or not.
     *
     * @param context The application context
     * @return True - if the Bluetooth adapter is on
     */
    @JvmStatic
    fun isBluetoothAdapterEnabled(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager != null) {
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null) return bluetoothAdapter.isEnabled
        }
        return false
    }

    /**
     * If the Bluetooth adapter is disabled, popup a system dialog for user to enable it.
     *
     * @param activity    The main activity
     * @param requestCode The request code for this intent
     */
    @JvmStatic
    fun requestEnableBluetooth(activity: Activity, requestCode: Int) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Above Android 6.0+, user have to  allow app to access location information then scan BLE device.
     *
     * @param activity The activity class
     * @return True - if the permission is granted
     */
    @JvmStatic
    fun isLocationPermissionGranted(activity: Activity): Boolean {
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * If the location permission isn't granted, popup a system dialog for user to enable it.
     *
     * @param activity    The main activity
     * @param requestCode The request code for this action
     */
    @JvmStatic
    fun requestLocationPermission(activity: Activity, requestCode: Int) {
        activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
    }
}