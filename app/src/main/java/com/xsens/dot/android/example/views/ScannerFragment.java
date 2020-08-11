package com.xsens.dot.android.example.views;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xsens.dot.android.example.adapters.ScannerAdapter;
import com.xsens.dot.android.example.databinding.FragmentScannerBinding;
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel;
import com.xsens.dot.android.sdk.interfaces.XsensDotScannerCb;
import com.xsens.dot.android.sdk.utils.XsensDotScanner;

import java.util.ArrayList;

public class ScannerFragment extends Fragment implements XsensDotScannerCb {

    private static final String TAG = ScannerFragment.class.getSimpleName();

    private FragmentScannerBinding mBinding;

    private XsensDotScanner mXsDotScanner;
    private boolean mIsScanning = false;
    private BluetoothViewModel mBluetoothViewModel;

    private ScannerAdapter mScannerAdapter;
    private ArrayList<BluetoothDevice> mScannedSensorList = new ArrayList<>();

    static ScannerFragment newInstance() {
        return new ScannerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentScannerBinding.inflate(LayoutInflater.from(getContext()));

        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBinding.startOrStopScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mIsScanning) {
                    mScannedSensorList.clear();
                    mScannerAdapter.notifyDataSetChanged();

                    mIsScanning = mXsDotScanner.startScan();
                } else {
                    mIsScanning = !mXsDotScanner.stopScan();
                }

                mBinding.startOrStopScanning.setText(mIsScanning ? "Stop Scanning" : "Start Scanning");
            }
        });

        mBinding.scannerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mScannerAdapter = new ScannerAdapter(mScannedSensorList);
        mBinding.scannerRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mBinding.scannerRecyclerView.setAdapter(mScannerAdapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        bindViewModel();
    }

    @Override
    public void onDestroy() {
        if (mXsDotScanner != null) {
            mXsDotScanner.stopScan();
        }

        super.onDestroy();
    }

    @Override
    public void onXsensDotScanned(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "onXsensDotScanned " + bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress());

        if (isAdded()) {
            if (!mScannedSensorList.contains(bluetoothDevice)) {
                mScannedSensorList.add(bluetoothDevice);
                mScannerAdapter.notifyItemInserted(mScannedSensorList.size() - 1);
            }
        }
    }

    private void initXsDotScanner() {
        if (mXsDotScanner == null) {
            mXsDotScanner = new XsensDotScanner(getContext(), this);
            mXsDotScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        }
    }

    private void bindViewModel() {
        if (getActivity() == null) {
            return;
        }

        mBluetoothViewModel = BluetoothViewModel.getInstance(getActivity());

        mBluetoothViewModel.isBluetoothAdapterEnabled().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isBluetoothAdapterEnabled) {
                Log.d(TAG, "isBluetoothAdapterEnabled " + isBluetoothAdapterEnabled);

                if (!isBluetoothAdapterEnabled && mIsScanning && mXsDotScanner != null) {
                    mIsScanning = !mXsDotScanner.stopScan();
                    mBinding.startOrStopScanning.setText(mIsScanning ? "Stop Scanning" : "Start Scanning");
                }

                mBinding.startOrStopScanning.setEnabled(isBluetoothAdapterEnabled);

                if (isBluetoothAdapterEnabled) {
                    initXsDotScanner();
                }
            }
        });
    }
}
