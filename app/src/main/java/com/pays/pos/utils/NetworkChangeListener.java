package com.pays.pos.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;

public class NetworkChangeListener {
    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;

    public interface NetworkListener {
        void onNetworkLost();
        void onNetworkAvailable();
    }

    public NetworkChangeListener(Context context, NetworkListener listener) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d("NetworkListener", "Network connected");
                listener.onNetworkAvailable();
            }

            @Override
            public void onLost(Network network) {
                Log.d("NetworkListener", "Network disconnected");
                listener.onNetworkLost();
            }
        };
    }

    public void register() {
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    public void unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
