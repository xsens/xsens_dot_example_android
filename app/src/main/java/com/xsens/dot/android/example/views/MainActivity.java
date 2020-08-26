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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.databinding.ActivityMainBinding;
import com.xsens.dot.android.example.interfaces.ScanClickInterface;
import com.xsens.dot.android.example.interfaces.StreamingClickInterface;
import com.xsens.dot.android.example.utils.Utils;
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel;
import com.xsens.dot.android.example.viewmodels.SensorViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

/**
 * The main activity.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // The code of request
    private static final int REQUEST_ENABLE_BLUETOOTH = 1001, REQUEST_PERMISSION_LOCATION = 1002;

    // The tag of fragments
    public static final String FRAGMENT_TAG_SCAN = "scan", FRAGMENT_TAG_DATA = "data";

    // The view binder of MainActivity
    private ActivityMainBinding mBinding;

    // The Bluetooth view model instance
    private BluetoothViewModel mBluetoothViewModel;

    // The sensor view model instance
    private SensorViewModel mSensorViewModel;

    // A variable for scanning flag
    private boolean mIsScanning = false;

    // Send the start/stop scan click event to fragment
    private ScanClickInterface mScanListener;

    // Send the start/stop streaming click event to fragment
    private StreamingClickInterface mStreamingListener;

    // A variable to keep the current fragment id
    public static String sCurrentFragment = FRAGMENT_TAG_SCAN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());

        setupFragmentContainer();
        bindViewModel();
        checkBluetoothAndPermission();

        // Register this action to monitor Bluetooth status.
        registerReceiver(mBluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();

        bindViewModel();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        unregisterReceiver(mBluetoothStateReceiver);
    }

    @Override
    public void onBackPressed() {

        FragmentManager manager = getSupportFragmentManager();

        // If the fragment count > 0 in the stack, try to resume the previous page.
        if (manager.getBackStackEntryCount() > 0) manager.popBackStack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() - requestCode = " + requestCode + ", resultCode = " + resultCode);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (resultCode == RESULT_OK) checkBluetoothAndPermission();
            else Toast.makeText(this, getString(R.string.hint_turn_on_bluetooth), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult() - requestCode = " + requestCode);

        if (requestCode == REQUEST_PERMISSION_LOCATION) {

            for (int i = 0; i < grantResults.length; i++) {

                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) checkBluetoothAndPermission();
                    else Toast.makeText(this, getString(R.string.hint_allow_location), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem scanItem = menu.findItem(R.id.action_scan);
        MenuItem streamingItem = menu.findItem(R.id.action_streaming);
        MenuItem measureItem = menu.findItem(R.id.action_measure);

        if (mIsScanning) scanItem.setTitle(getString(R.string.menu_stop_scan));
        else scanItem.setTitle(getString(R.string.menu_start_scan));

        final boolean isStreaming = mSensorViewModel.isStreaming().getValue();
        if (isStreaming) streamingItem.setTitle(getString(R.string.menu_stop_streaming));
        else streamingItem.setTitle(getString(R.string.menu_start_streaming));

        if (sCurrentFragment.equals(FRAGMENT_TAG_SCAN)) {

            scanItem.setVisible(true);
            streamingItem.setVisible(false);
            measureItem.setVisible(true);

        } else if (sCurrentFragment.equals(FRAGMENT_TAG_DATA)) {

            scanItem.setVisible(false);
            streamingItem.setVisible(true);
            measureItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case R.id.action_scan:

                if (mScanListener != null && checkBluetoothAndPermission()) {
                    // Make sure th location permission is granted then start/stop scanning.
                    if (mIsScanning) mScanListener.onScanTriggered(false);
                    else mScanListener.onScanTriggered(true);
                }
                break;

            case R.id.action_streaming:
                // When the streaming button is clicked, notify to DataFragment and wait for the syncing result.
                mStreamingListener.onStreamingTriggered();
                break;

            case R.id.action_measure:
                // Change to DataFragment and put ScanFragment to the back stack.
                Fragment dataFragment = DataFragment.newInstance();
                addFragment(dataFragment, FRAGMENT_TAG_DATA);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Use ScanFragment as default page.
     */
    private void setupFragmentContainer() {

        if (null != getIntent()) {

            ScanFragment fragment = ScanFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, FRAGMENT_TAG_SCAN).commit();
        }
    }

    /**
     * Add a fragment to full the screen.
     *
     * @param fragment The instance of fragment
     * @param tag      The tag of fragment
     */
    private void addFragment(Fragment fragment, String tag) {

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, tag).addToBackStack(null).commit();
    }

    /**
     * Check the state of Bluetooth adapter and location permission.
     */
    private boolean checkBluetoothAndPermission() {

        boolean isBluetoothEnabled = Utils.isBluetoothAdapterEnabled(this);
        boolean isPermissionGranted = Utils.isLocationPermissionGranted(this);

        if (isBluetoothEnabled) {
            if (!isPermissionGranted) Utils.requestLocationPermission(this, REQUEST_PERMISSION_LOCATION);
        } else {
            Utils.requestEnableBluetooth(this, REQUEST_ENABLE_BLUETOOTH);
        }

        boolean status = isBluetoothEnabled && isPermissionGranted;
        Log.i(TAG, "checkBluetoothAndPermission() - " + status);

        mBluetoothViewModel.updateBluetoothEnableState(status);
        return status;
    }

    /**
     * Initialize and observe view models.
     */
    private void bindViewModel() {

        mBluetoothViewModel = BluetoothViewModel.getInstance(this);

        mBluetoothViewModel.isScanning().observe(this, new Observer<Boolean>() {

            @Override
            public void onChanged(Boolean scanning) {
                // If the status of scanning is changed, try to refresh the menu.
                mIsScanning = scanning;
                invalidateOptionsMenu();
            }
        });

        mSensorViewModel = SensorViewModel.getInstance(this);

        mSensorViewModel.isStreaming().observe(this, new Observer<Boolean>() {

            @Override
            public void onChanged(Boolean status) {
                // If the status of streaming is changed, try to refresh the menu.
                invalidateOptionsMenu();
            }
        });
    }

    /**
     * Set the trigger of scan button.
     *
     * @param listener The class which implemented ScanClickInterface
     */
    public void setScanTriggerListener(ScanClickInterface listener) {

        mScanListener = listener;
    }

    /**
     * Set the trigger of streaming button.
     *
     * @param listener The class which implemented StreamingClickInterface
     */
    public void setStreamingTriggerListener(StreamingClickInterface listener) {

        mStreamingListener = listener;
    }

    /**
     * A receiver for Bluetooth status.
     */
    private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (action != null) {

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    // Notify the Bluetooth status to ScanFragment.
                    switch (state) {

                        case BluetoothAdapter.STATE_OFF:

                            mBluetoothViewModel.updateBluetoothEnableState(false);
                            break;

                        case BluetoothAdapter.STATE_ON:

                            mBluetoothViewModel.updateBluetoothEnableState(true);
                            break;
                    }
                }
            }
        }
    };
}
