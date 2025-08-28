package com.example.NeuraFuse.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyVpnService extends VpnService {

    private static final String TAG = "NeuraFuseVPN";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VPN Service started");
        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        if (vpnInterface != null) {
            Log.d(TAG, "VPN already running");
            return;
        }

        Builder builder = new Builder();
        builder.setSession("NeuraFuseVPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0); // Block all traffic unless allowed

        try {
            Set<String> allowedApps = getPrimaryApps();

            Log.d(TAG, "Allowed apps: " + allowedApps);

            for (String packageName : allowedApps) {
                builder.addAllowedApplication(packageName);
            }

            vpnInterface = builder.establish();

            if (vpnInterface != null) {
                Log.d(TAG, "VPN established successfully ✅");
            } else {
                Log.e(TAG, "❌ VPN Interface is null");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start VPN", e);
        }
    }

    private Set<String> getPrimaryApps() {
        SharedPreferences prefs = getSharedPreferences("PrimaryApps", MODE_PRIVATE);
        return prefs.getStringSet("primaryApps", new HashSet<>());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
                Log.d(TAG, "VPN stopped");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error stopping VPN", e);
        }
    }
}
