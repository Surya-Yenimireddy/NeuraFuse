package com.example.neurafuse.services;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public class MyVpnService extends VpnService {

    private static final String TAG = "NeuraFuseVPN";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Configure VPN
            Builder builder = new Builder();
            builder.setSession("NeuraFuse VPN")
                    .addAddress("10.0.0.2", 24)
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0);

            vpnInterface = builder.establish();
            Log.i(TAG, "✅ VPN started successfully");

            // Simple VPN thread that discards traffic
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    ByteBuffer packet = ByteBuffer.allocate(32767);
                    while (vpnInterface != null && vpnInterface.getFileDescriptor().valid()) {
                        packet.clear();
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "VPN thread stopped", e);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start VPN", e);
        }

        return START_STICKY;
    }

    /**
     * 🔒 Called by ForegroundMonitorService to block a specific app's network access.
     * Currently a placeholder for deeper packet filtering if extended later.
     */
    public static void blockApp(String packageName) {
        Log.w(TAG, "🚫 Blocking network for: " + packageName);
        // In a full implementation, use Android VpnService.Builder to exclude this app
        // or modify routing tables dynamically here.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing VPN", e);
        }
        Log.i(TAG, "🛑 VPN stopped");
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        onDestroy();
    }
}
