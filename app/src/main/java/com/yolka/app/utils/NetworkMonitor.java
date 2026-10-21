package com.yolka.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Monitor reactivo de conectividad para el modo Parcialmente Offline.
 * Cuando se restablece la conexión a internet, dispara SyncQueueManager.flushPendingQueue().
 */
public class NetworkMonitor {

    private static volatile NetworkMonitor instance;
    private final MutableLiveData<Boolean> onlineLiveData = new MutableLiveData<>(true);
    private boolean isOnline = true;

    private NetworkMonitor(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        isOnline = checkCurrentConnectivity(cm);
        onlineLiveData.postValue(isOnline);

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                boolean wasOffline = !isOnline;
                isOnline = true;
                onlineLiveData.postValue(true);
                if (wasOffline) {
                    SyncQueueManager.getInstance().flushPendingQueue(null);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                isOnline = checkCurrentConnectivity(cm);
                onlineLiveData.postValue(isOnline);
            }
        });
    }

    private boolean checkCurrentConnectivity(ConnectivityManager cm) {
        try {
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) {
                    instance = new NetworkMonitor(context);
                }
            }
        }
    }

    public static NetworkMonitor getInstance() {
        return instance;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public LiveData<Boolean> getOnlineState() {
        return onlineLiveData;
    }
}
