package com.xsens.dot.android.example.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.utils.Utils;
import com.xsens.dot.android.example.databinding.ActivityMainBinding;
import com.xsens.dot.android.example.viewmodels.BluetoothViewModel;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BLUETOOTH = 1024;
    private static final int REQUEST_PERMISSION_LOCATION = 2048;

    private ActivityMainBinding mBinding;
    private BluetoothViewModel mBluetoothViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());

        setupFragmentContainer();

        mBluetoothViewModel = BluetoothViewModel.getInstance(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        checkBluetoothPermissions();
    }

    private void checkBluetoothPermissions() {
        boolean isBluetoothAdapterEnabled = Utils.isBluetoothAdapterEnabled(this);
        boolean hasLocationPermission = hasLocationPermission();

        if (isBluetoothAdapterEnabled) {
            if (!hasLocationPermission) {
                requestLocationPermission();
            }
        } else {
            requestEnableBluetooth();
        }

        Log.d(TAG, "isBluetoothAdapterEnabled " + isBluetoothAdapterEnabled + ", hasLocationPermission " + hasLocationPermission);

        mBluetoothViewModel.updateBluetoothEnableState(isBluetoothAdapterEnabled && hasLocationPermission);
    }

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, MainActivity.class);
    }

    private void setupFragmentContainer() {
        if (null != getIntent()) {
            ScannerFragment fragment = ScannerFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.scanner_fragment_container, fragment).commit();
        }
    }

    private void requestEnableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() - requestCode = " + requestCode + ", resultCode = " + resultCode);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (resultCode == RESULT_OK) {
                checkBluetoothPermissions();
            } else {
                Toast.makeText(this, "Please turn on bluetooth.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult() - requestCode = " + requestCode);

        if (requestCode == REQUEST_PERMISSION_LOCATION) {

            for (int i = 0; i < grantResults.length; i++) {

                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        checkBluetoothPermissions();
                    } else {
                        Toast.makeText(this, "Please allow location permission to use your trackers.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }
}
