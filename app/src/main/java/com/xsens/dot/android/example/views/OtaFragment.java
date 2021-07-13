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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelStoreOwner;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.databinding.FragmentOtaBinding;
import com.xsens.dot.android.example.viewmodels.SensorViewModel;
import com.xsens.dot.android.sdk.models.XsensDotDevice;
import com.xsens.dot.android.sdk.ota.XsensDotOtaManager;
import com.xsens.dot.android.sdk.ota.interfaces.XsensDotOtaCallback;

import java.util.ArrayList;

import static com.xsens.dot.android.example.views.MainActivity.FRAGMENT_TAG_OTA;

/**
 * A fragment for processing OTA, this is an example of Ota upgrade for a sensor.
 * If you want to support multiple upgrading, please implement by yourself.
 */
public class OtaFragment extends Fragment implements XsensDotOtaCallback {

    // The view binder of OtaFragment
    private FragmentOtaBinding mBinding;

    // The devices view model instance
    private SensorViewModel mSensorViewModel;

    // A device to do OTA
    private XsensDotDevice mOtaDevice;

    // A device OTA Manager
    private XsensDotOtaManager mOtaManager;

    // To control the test of ota file downloading
    private boolean mIsNewOtaFileDownloaded = false;

    /**
     * Get the instance of OtaFragment
     *
     * @return The instance of OtaFragment
     */
    public static OtaFragment newInstance() {
        return new OtaFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = FragmentOtaBinding.inflate(LayoutInflater.from(getContext()));
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        bindViewModel();

        ArrayList<XsensDotDevice> devices = mSensorViewModel.getAllSensors();
        // Simply process to get the first sensor to do OTA test
        mOtaDevice = devices.get(0);
        // Instantiate an OTA Manager
        mOtaManager = new XsensDotOtaManager(requireContext(), this, mOtaDevice, this);

        updateViews();
    }

    private void updateViews() {
        // Set basic info of device
        mBinding.deviceName.setText(TextUtils.isEmpty(mOtaDevice.getTag()) ? mOtaDevice.getName() : mOtaDevice.getTag());
        mBinding.deviceAddress.setText(mOtaDevice.getAddress());
        mBinding.firmwareVersion.setText(mOtaDevice.getFirmwareVersion());

        // For test 'checkOtaUpdates()' function
        mBinding.checkUpdate.setOnClickListener(v -> {
            clearOtaResult();

            mOtaManager.checkOtaUpdates();
        });

        // For test 'checkOtaUpdatesAndDownload()' function
        mBinding.checkUpdateAndDownload.setOnClickListener(v -> {
            clearOtaResult();

            mOtaManager.checkOtaUpdatesAndDownload();
        });

        // For test 'startOta()' function, the premise is that OTA file has been downloaded, the 'checkOtaUpdatesAndDownload()' method has been called.
        mBinding.startOta.setOnClickListener(v -> {
            if (mIsNewOtaFileDownloaded) {
                clearOtaResult();

                mOtaManager.startOta();
            } else {
                Toast.makeText(requireContext(), getString(R.string.tip_start_ota), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Show the result of calling OTA functions
     */
    private void setOtaResult(boolean result) {
        final Activity activity = getActivity();

        if (activity != null) {
            activity.runOnUiThread(() -> {
                int resultStringId = result ? R.string.ota_result_success : R.string.ota_result_fail;
                mBinding.updateState.setText(resultStringId);
            });
        }
    }

    /**
     * Before do the OTA function test, the last result will be cleared.
     */
    private void clearOtaResult() {
        mIsNewOtaFileDownloaded = false;
        mBinding.updateState.setText(R.string.empty_value);
        mBinding.otaProgress.setText(R.string.empty_value);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = FRAGMENT_TAG_OTA;
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        initViewModel((ViewModelStoreOwner) context);

        // Stop measurement for each sensor when entering this page.
        mSensorViewModel.setMeasurement(false);
        // It's necessary to update this status, because user may enter this page again.
        mSensorViewModel.updateStreamingStatus(false);
    }

    /**
     * Initialize ViewModel
     */
    private void initViewModel(ViewModelStoreOwner context) {
        if (mSensorViewModel == null) {
            mSensorViewModel = SensorViewModel.getInstance(context);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Please make sure to clear the OTA manager when destroying.
        mOtaManager.clear();
        mOtaManager = null;
    }

    /**
     * Initialize and observe view models.
     */
    private void bindViewModel() {
        if (getActivity() != null) {
            initViewModel(getActivity());

            mSensorViewModel.getTagChangedDevice().observe(getViewLifecycleOwner(), device -> {
                if (mOtaDevice != null && device.getAddress().equals(mOtaDevice.getAddress())) {
                    updateViews();
                }
            });
        }
    }

    @Override
    public void onOtaUpdates(String address, boolean result, String version, String releaseNotes) {
        final Activity activity = getActivity();

        // Show result
        if (activity != null) {
            activity.runOnUiThread(() -> {
                mIsNewOtaFileDownloaded = false;
                int resultStringId = result ? R.string.ota_result_success : R.string.ota_result_fail;
                String versionInfo = TextUtils.isEmpty(version) ?
                        getString(R.string.no_new_firmware_version)
                        : getString(R.string.new_firmware_version, version);
                mBinding.updateState.setText(getString(resultStringId) + versionInfo);
            });
        }
    }

    @Override
    public void onOtaRollback(String address, boolean result, String version, String releaseNotes) {
        mIsNewOtaFileDownloaded = false;
    }

    @Override
    public void onOtaFileMismatch(String address) {
        final Activity activity = getActivity();

        // Show result
        if (activity != null) {
            activity.runOnUiThread(() -> {
                mIsNewOtaFileDownloaded = false;
                mBinding.updateState.setText(R.string.tip_ota_file_mismatch);
            });
        }
    }

    @Override
    public void onOtaDownload(String address, boolean result) {
        // Show result
        mIsNewOtaFileDownloaded = result;
        setOtaResult(result);
    }

    @Override
    public void onOtaStart(String address, boolean result, int errorCode) {
        final Activity activity = getActivity();

        // Show result
        if (activity != null) {
            activity.runOnUiThread(() -> {
                int resultStringId = result ? R.string.ota_result_success : R.string.ota_result_fail;
                mBinding.updateState.setText("Start OTA " + getString(resultStringId) + getString(R.string.error_code, errorCode));
            });
        }
    }

    @Override
    public void onOtaProgress(String address, float progress, int errorCode) {
        final Activity activity = getActivity();

        // Show result
        if (activity != null) {
            activity.runOnUiThread(() -> mBinding.otaProgress.setText((int) progress + "%"));
        }
    }

    @Override
    public void onOtaEnd(String address, boolean result, int errorCode) {
        // Show result
        setOtaResult(result);
    }

    @Override
    public void onOtaUncharged(String address) {
        final Activity activity = getActivity();

        // Show result
        if (activity != null) {
            activity.runOnUiThread(() -> mBinding.updateState.setText(R.string.tip_dot_uncharged));
        }
    }
}
