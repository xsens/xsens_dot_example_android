package com.xsens.dot.android.example.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;


public class BluetoothViewModel extends ViewModel {

    public static BluetoothViewModel getInstance(@NonNull ViewModelStoreOwner owner) {
        return new ViewModelProvider(owner, new ViewModelProvider.NewInstanceFactory())
                .get(BluetoothViewModel.class);
    }

    private MutableLiveData<Boolean> isBluetoothAdapterEnabled = new MutableLiveData<>();

    public MutableLiveData<Boolean> isBluetoothAdapterEnabled() {
        return isBluetoothAdapterEnabled;
    }

    public void updateBluetoothEnableState(boolean isEnabled) {
        isBluetoothAdapterEnabled.postValue(isEnabled);
    }

}