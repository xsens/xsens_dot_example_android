package com.xsens.dot.android.example.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xsens.dot.android.example.R;

import java.util.ArrayList;

public class ScannerAdapter extends RecyclerView.Adapter<ScannerAdapter.ScannerViewHolder> {

    private ArrayList<BluetoothDevice> mScannedSensorList;

    public ScannerAdapter(ArrayList<BluetoothDevice> scannedSensorList) {
        mScannedSensorList = scannedSensorList;
    }

    @NonNull
    @Override
    public ScannerAdapter.ScannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_scanner, parent, false);

        return new ScannerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScannerViewHolder holder, int position) {
        holder.sensorName.setText(mScannedSensorList.get(position).getName());
        holder.sensorMacAddress.setText(mScannedSensorList.get(position).getAddress());

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        });
    }

    @Override
    public int getItemCount() {
        return mScannedSensorList == null ? 0 : mScannedSensorList.size();
    }

    static class ScannerViewHolder extends RecyclerView.ViewHolder {
        View rootView;
        TextView sensorName;
        TextView sensorMacAddress;

        ScannerViewHolder(View v) {
            super(v);

            rootView = v;
            sensorName = v.findViewById(R.id.sensor_name);
            sensorMacAddress = v.findViewById(R.id.sensor_mac_address);
        }
    }

}
