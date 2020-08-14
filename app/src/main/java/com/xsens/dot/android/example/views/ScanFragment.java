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

package com.xsens.dot.android.example.views;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.adapters.ScanAdapter;
import com.xsens.dot.android.example.databinding.FragmentScanBinding;
import com.xsens.dot.android.example.interfaces.SensorClickInterface;
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel;
import com.xsens.dot.android.sdk.interfaces.XsensDotScannerCb;
import com.xsens.dot.android.sdk.utils.XsensDotScanner;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

public class ScanFragment extends Fragment implements XsensDotScannerCb, SensorClickInterface {

    private static final String TAG = ScanFragment.class.getSimpleName();

    private FragmentScanBinding mBinding;
    private BluetoothViewModel mBluetoothViewModel;

    private ScanAdapter mScanAdapter;
    private ArrayList<BluetoothDevice> mScannedSensorList = new ArrayList<>();

    private XsensDotScanner mXsDotScanner;
    private boolean mIsScanning = false;

    public static ScanFragment newInstance() {

        return new ScanFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);

        bindViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = FragmentScanBinding.inflate(LayoutInflater.from(getContext()));
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mScanAdapter = new ScanAdapter(mScannedSensorList);
        mScanAdapter.setSensorClickListener(this);
        mBinding.sensorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.sensorRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mBinding.sensorRecyclerView.setAdapter(mScanAdapter);

        mBinding.scanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!mIsScanning) {

                    mScannedSensorList.clear();
                    mScanAdapter.notifyDataSetChanged();

                    mIsScanning = mXsDotScanner.startScan();

                } else {
                    // If success for stopping, it will return True from SDK. So use !(not) here.
                    mIsScanning = !mXsDotScanner.stopScan();
                }

                mBinding.scanButton.setText(mIsScanning ? getString(R.string.stop_scan) : getString(R.string.start_scan));
            }
        });
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mXsDotScanner != null) mXsDotScanner.stopScan();
    }

    @Override
    public void onSensorClick(View v, int position) {

    }

    @Override
    public void onXsensDotScanned(BluetoothDevice device) {

        Log.i(TAG, "onXsensDotScanned() - Name: " + device.getName() + ", Address: " + device.getAddress());

        if (isAdded()) {

            if (!mScannedSensorList.contains(device)) {

                mScannedSensorList.add(device);
                mScanAdapter.notifyItemInserted(mScannedSensorList.size() - 1);
            }
        }
    }

    private void bindViewModel() {

        if (getActivity() != null) {

            mBluetoothViewModel = BluetoothViewModel.getInstance(getActivity());

            mBluetoothViewModel.isBluetoothEnabled().observe(this, new Observer<Boolean>() {

                @Override
                public void onChanged(Boolean enabled) {

                    Log.d(TAG, "isBluetoothEnabled = " + enabled);

                    if (enabled) initXsDotScanner();
                    else mIsScanning = false;

                    mBinding.scanButton.setEnabled(enabled);
                    mBinding.scanButton.setText(enabled ? getString(R.string.start_scan) : getString(R.string.stop_scan));
                }
            });
        }
    }

    private void initXsDotScanner() {

        if (mXsDotScanner == null) {

            mXsDotScanner = new XsensDotScanner(getContext(), this);
            mXsDotScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        }
    }
}
