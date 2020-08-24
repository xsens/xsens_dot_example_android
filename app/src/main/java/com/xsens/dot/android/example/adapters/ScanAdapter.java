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

package com.xsens.dot.android.example.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.example.interfaces.SensorClickInterface;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_CONNECTED;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_CONNECTING;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_DISCONNECTED;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.CONN_STATE_RECONNECTING;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanViewHolder> {

    private static final String TAG = ScanAdapter.class.getSimpleName();

    public static final String KEY_DEVICE = "device";
    public static final String KEY_STATE = "state";

    private Context mContext;

    private SensorClickInterface mListener;
    private ArrayList<HashMap<String, Object>> mSensorList;

    public ScanAdapter(Context context, ArrayList<HashMap<String, Object>> scannedSensorList) {

        mContext = context;
        mSensorList = scannedSensorList;
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor, parent, false);
        return new ScanViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, final int position) {

        BluetoothDevice device = (BluetoothDevice) mSensorList.get(position).get(KEY_DEVICE);
        int state = (int) mSensorList.get(position).get(KEY_STATE);

        holder.sensorName.setText(device.getName());
        holder.sensorMacAddress.setText(device.getAddress());

        switch (state) {

            case CONN_STATE_DISCONNECTED:

                holder.sensorState.setVisibility(View.GONE);
                holder.sensorState.setText(mContext.getResources().getString(R.string.disconnected));
                break;

            case CONN_STATE_CONNECTING:

                holder.sensorState.setVisibility(View.VISIBLE);
                holder.sensorState.setText(mContext.getResources().getString(R.string.connecting));
                break;

            case CONN_STATE_CONNECTED:

                holder.sensorState.setVisibility(View.VISIBLE);
                holder.sensorState.setText(mContext.getResources().getString(R.string.connected));
                break;

            case CONN_STATE_RECONNECTING:

                holder.sensorState.setVisibility(View.VISIBLE);
                holder.sensorState.setText(mContext.getResources().getString(R.string.reconnecting));
                break;
        }

        holder.rootView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mListener != null) mListener.onSensorClick(v, position);
            }
        });
    }

    @Override
    public int getItemCount() {

        return mSensorList == null ? 0 : mSensorList.size();
    }

    public BluetoothDevice getDevice(int position) {

        if (mSensorList != null) {

            return (BluetoothDevice) mSensorList.get(position).get(KEY_DEVICE);
        }

        return null;
    }

    public void setSensorClickListener(SensorClickInterface listener) {

        mListener = listener;
    }

    static class ScanViewHolder extends RecyclerView.ViewHolder {

        View rootView;
        TextView sensorName;
        TextView sensorMacAddress;
        TextView sensorState;

        ScanViewHolder(View v) {

            super(v);

            rootView = v;
            sensorName = v.findViewById(R.id.sensor_name);
            sensorMacAddress = v.findViewById(R.id.sensor_mac_address);
            sensorState = v.findViewById(R.id.sensor_state);
        }
    }
}
