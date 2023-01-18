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
package com.xsens.dot.android.example.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import androidx.lifecycle.ViewModelStoreOwner

/**
 * A view model class for notifying data to views.
 */
class BluetoothViewModel : ViewModel() {
    /**
     * Observe this function to listen the status of Bluetooth adapter.
     *
     * @return The latest status
     */
    // A variable to notify the Bluetooth status
    val isBluetoothEnabled = MutableLiveData<Boolean>()

    /**
     * Observe this function to listen the scanning status.
     *
     * @return The latest scan status
     */
    // A variable to notify the scanning status
    val isScanning = MutableLiveData<Boolean>()

    /**
     * Notify the Bluetooth adapter status to activity/fragment
     *
     * @param enabled he status of Bluetooth
     */
    fun updateBluetoothEnableState(enabled: Boolean) {
        isBluetoothEnabled.postValue(enabled)
    }

    /**
     * Notify the scan status to activity/fragment
     *
     * @param scanning The status of scanning
     */
    fun updateScanState(scanning: Boolean) {
        isScanning.postValue(scanning)
    }

    companion object {
        private val TAG = BluetoothViewModel::class.java.simpleName

        /**
         * Get the instance of BluetoothViewModel
         *
         * @param owner The life cycle owner from activity/fragment
         * @return The BluetoothViewModel
         */
        @JvmStatic
        fun getInstance(owner: ViewModelStoreOwner): BluetoothViewModel {
            return ViewModelProvider(owner, NewInstanceFactory()).get(BluetoothViewModel::class.java)
        }
    }
}