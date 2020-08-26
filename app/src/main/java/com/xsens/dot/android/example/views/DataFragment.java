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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.databinding.FragmentDataBinding;
import com.xsens.dot.android.example.interfaces.StreamingClickInterface;
import com.xsens.dot.android.example.viewmodels.SensorViewModel;
import com.xsens.dot.android.sdk.interfaces.XsensDotSyncCallback;
import com.xsens.dot.android.sdk.models.XsensDotDevice;
import com.xsens.dot.android.sdk.models.XsensDotSyncManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.xsens.dot.android.example.views.MainActivity.FRAGMENT_TAG_DATA;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_CONNECTED;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.LOG_STATE_ON;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.PLOT_STATE_ON;
import static com.xsens.dot.android.sdk.models.XsensDotPayload.PAYLOAD_TYPE_COMPLETE_EULER;

/**
 * A fragment for presenting the raw-data and storing to file.
 */
public class DataFragment extends Fragment implements StreamingClickInterface, XsensDotSyncCallback {

    private static final String TAG = DataFragment.class.getSimpleName();

    // The code of request
    private static final int SYNCING_REQUEST_CODE = 1001;

    // The view binder of DataFragment
    private FragmentDataBinding mBinding;

    // The devices view model instance
    private SensorViewModel mSensorViewModel;

    // A dialog during the synchronization
    private AlertDialog mSyncingDialog;

    /**
     * Get the instance of DataFragment
     *
     * @return The instance of DataFragment
     */
    public static DataFragment newInstance() {

        return new DataFragment();
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

        mBinding = FragmentDataBinding.inflate(LayoutInflater.from(getContext()));
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        setStates(PLOT_STATE_ON, LOG_STATE_ON);

        AlertDialog.Builder syncingDialogBuilder = new AlertDialog.Builder(getActivity());
        syncingDialogBuilder.setView(R.layout.dialog_syncing);
        syncingDialogBuilder.setCancelable(false);
        mSyncingDialog = syncingDialogBuilder.create();

        // Set the StreamingClickInterface instance to main activity.
        if (getActivity() != null) ((MainActivity) getActivity()).setStreamingTriggerListener(this);
    }

    @Override
    public void onResume() {

        super.onResume();

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = FRAGMENT_TAG_DATA;
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onStreamingTriggered() {

        if (mSensorViewModel.isStreaming().getValue()) {
            // To stop.
            setMeasurement(false);
            mSensorViewModel.updateStreamingStatus(false);

        } else {
            // To start.
            final ArrayList<XsensDotDevice> devices = mSensorViewModel.getAllSensors();
            if (!checkConnection(devices)) return;

            // Set first device to root.
            setRootDevice(devices, true);
            // Devices will disconnect during the syncing, and do reconnection automatically.
            XsensDotSyncManager.getInstance(this).startSyncing(devices, SYNCING_REQUEST_CODE);
            if (!mSyncingDialog.isShowing()) mSyncingDialog.show();
        }
    }

    /**
     * Initialize and observe view models.
     */
    private void bindViewModel() {

        if (getActivity() != null) {

            mSensorViewModel = SensorViewModel.getInstance(getActivity());
        }
    }

    /**
     * Set the plotting and logging states for each device.
     *
     * @param plot
     * @param log
     */
    private void setStates(int plot, int log) {

        List<XsensDotDevice> devices = mSensorViewModel.getAllSensors();

        for (XsensDotDevice device : devices) {

            device.setPlotState(plot);
            device.setLogState(log);
        }
    }

    /**
     * Check the connection state of all sensors.
     *
     * @param devices A list contains all devices
     * @return True - If all sensors are connected
     */
    private boolean checkConnection(List<XsensDotDevice> devices) {

        for (XsensDotDevice device : devices) {

            final int state = device.getConnectionState();
            if (state != CONN_STATE_CONNECTED) return false;
        }

        return true;
    }

    /**
     * Set one sensor for root of synchronization.
     *
     * @param devices A list contains all devices
     * @param isRoot  True - If set to root
     */
    private void setRootDevice(List<XsensDotDevice> devices, boolean isRoot) {

        if (devices.size() > 0) devices.get(0).setRootDevice(isRoot);
    }

    /**
     * Set the measurement mode to all sensors.
     *
     * @param mode The measurement mode
     */
    private void setMeasurementMode(int mode) {

        List<XsensDotDevice> devices = mSensorViewModel.getAllSensors();

        for (XsensDotDevice device : devices) {

            device.setMeasurementMode(mode);
        }
    }

    private void setMeasurement(boolean enabled) {

        List<XsensDotDevice> devices = mSensorViewModel.getAllSensors();

        for (XsensDotDevice device : devices) {

            if (enabled) device.startMeasuring();
            else device.stopMeasuring();
        }
    }

    @Override
    public void onSyncingProgress(final int progress, final int requestCode) {

        Log.i(TAG, "onSyncingProgress() - progress = " + progress + ", requestCode = " + requestCode);

        if (requestCode == SYNCING_REQUEST_CODE) {

            if (mSyncingDialog.isShowing()) {

                if (getActivity() != null) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Find the view of progress bar in dialog layout and update.
                            ProgressBar bar = mSyncingDialog.findViewById(R.id.syncing_progress);
                            bar.setProgress(progress);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onSyncingResult(String address, boolean isSuccess, int requestCode) {

        Log.i(TAG, "onSyncingResult() - address = " + address + ", isSuccess = " + isSuccess + ", requestCode = " + requestCode);
    }

    @Override
    public void onSyncingDone(final HashMap<String, Boolean> syncingResultMap, final boolean isSuccess, final int requestCode) {

        Log.i(TAG, "onSyncingDone() - isSuccess = " + isSuccess + ", requestCode = " + requestCode);

        if (requestCode == SYNCING_REQUEST_CODE) {

            if (getActivity() != null) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mSyncingDialog.isShowing()) mSyncingDialog.dismiss();

                        final ArrayList<XsensDotDevice> devices = mSensorViewModel.getAllSensors();
                        setRootDevice(devices, false);

                        if (isSuccess) {
                            // Syncing precess is success, choose one measurement mode to start measuring.
                            setMeasurementMode(PAYLOAD_TYPE_COMPLETE_EULER);
                            setMeasurement(true);
                            // Notify the current streaming status to MainActivity to refresh the menu.
                            mSensorViewModel.updateStreamingStatus(true);

                        } else {
                            // If the syncing result is fail, show a message to user
                            Toast.makeText(getContext(), getString(R.string.hint_syncing_failed), Toast.LENGTH_LONG).show();

                            for (Map.Entry<String, Boolean> result : syncingResultMap.entrySet()) {

                                if (!result.getValue()) {
                                    // Get the key of this failed device.
                                    String address = result.getKey();
                                    // It's preferred to stop measurement of all sensors.
                                    setMeasurement(false);
                                    // Notify the current streaming status to MainActivity to refresh the menu.
                                    mSensorViewModel.updateStreamingStatus(false);
                                }
                            }
                        }
                    }
                });
            }
        }
    }
}
