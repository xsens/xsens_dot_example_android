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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xsens.dot.android.example.R;
import com.xsens.dot.android.sdk.events.XsensDotData;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A view adapter for item view to present data.
 */
public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {

    private static final String TAG = DataAdapter.class.getSimpleName();

    // The keys of HashMap
    public static final String KEY_ADDRESS = "address", KEY_TAG = "tag", KEY_DATA = "data";

    // The application context
    private Context mContext;

    // Put all data from sensors into one list
    private ArrayList<HashMap<String, Object>> mDataList;

    /**
     * Default constructor.
     *
     * @param context  The application context
     * @param dataList A list contains tag and data
     */
    public DataAdapter(Context context, ArrayList<HashMap<String, Object>> dataList) {

        mContext = context;
        mDataList = dataList;
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data, parent, false);
        return new DataViewHolder(itemView);
    }

    @Override
    @SuppressLint("DefaultLocale")
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {

        String tag = (String) mDataList.get(position).get(KEY_TAG);
        XsensDotData xsData = (XsensDotData) mDataList.get(position).get(KEY_DATA);

        holder.sensorName.setText(tag);

        double[] eulerAngles = xsData.getEuler();
        String eulerAnglesStr =
                String.format("%.6f", eulerAngles[0]) + ", " +
                String.format("%.6f", eulerAngles[1]) + ", " +
                String.format("%.6f", eulerAngles[2]);
        holder.orientationData.setText(eulerAnglesStr);

        float[] freeAcc = xsData.getFreeAcc();
        String freeAccStr =
                String.format("%.6f", freeAcc[0]) + ", " +
                String.format("%.6f", freeAcc[1]) + ", " +
                String.format("%.6f", freeAcc[2]);
        holder.freeAccData.setText(freeAccStr);
    }

    @Override
    public int getItemCount() {

        return mDataList == null ? 0 : mDataList.size();
    }

    /**
     * A Customized class for ViewHolder of RecyclerView.
     */
    static class DataViewHolder extends RecyclerView.ViewHolder {

        View rootView;
        TextView sensorName;
        TextView orientationData;
        TextView freeAccData;

        DataViewHolder(View v) {

            super(v);

            rootView = v;
            sensorName = v.findViewById(R.id.sensor_name);
            orientationData = v.findViewById(R.id.orientation_data);
            freeAccData = v.findViewById(R.id.free_acc_data);
        }
    }
}
