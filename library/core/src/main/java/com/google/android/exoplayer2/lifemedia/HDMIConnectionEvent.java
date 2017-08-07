package com.google.android.exoplayer2.lifemedia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by cclab on 2017-04-21.
 */

public class HDMIConnectionEvent extends BroadcastReceiver {

    ContextInformation contextInfo;

    public HDMIConnectionEvent (ContextInformation contextInfo){
        this.contextInfo = contextInfo;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String checkHDMI = intent.getAction();
        if(checkHDMI.equals("android.intent.action.HDMI_PLUGGED")){
            if(ContextInformation.deviceType != null){
                ContextInformation.deviceType = ContextInformation.deviceType.equals("Tab") ? "TV" : "Phone";
            }
        }
    }

}
