package com.google.android.exoplayer2.lifemedia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by cclab on 2017-04-21.
 */

// InetInfoCollect Connection의 Type is well returned, but RSSI is not
    // The problem is authority problem about Context
    // If context.getSystemService( ... ) -> No Access
    // But getApplicationContext.getSystemService( ... ) or getSystemService( ... ) -> Well Access

public class InetInfoCollect extends BroadcastReceiver {

    ContextInformation contextInfo;

    public InetInfoCollect (ContextInformation contextInfo){
        this.contextInfo = contextInfo;
    }

    public String collectConnectionType(Context context){

        try{
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork.isConnectedOrConnecting();

            if(activeNetwork != null && isConnected){

                if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE){
                    return "Mobile";
                }

                else if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI){
                    return "WiFi";
                }
            }

            else{
                return null;
            }
        }

        catch(Exception e){
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = false;
            if(activeNetwork != null)
                isConnected = activeNetwork.isConnectedOrConnecting();

            if(activeNetwork != null && isConnected){
                return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ? "WiFi" : null;
            }

            else{
                return null;
            }
        }

        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String connectionType = collectConnectionType(context);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = null;
        int RSSI = 0;

        //WiFi --> Mobile
        if(wifiManager != null){
            wifiInfo = wifiManager.getConnectionInfo();
            //if(wifiInfo != null)
                //RSSI = wifiInfo.getRssi();
        }

        if(connectionType != null) {
            if (connectionType.equals("Mobile")) {
                ContextInformation.connectionType = "Mobile";
                //ContextInformation.RSSI = 0;
            } else if (connectionType.equals("WiFi") && wifiInfo != null && RSSI != 0) {
                ContextInformation.connectionType = "WiFi";
                //ContextInformation.RSSI = RSSI;
            }
        }

    }

}
