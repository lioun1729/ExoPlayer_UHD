package com.google.android.exoplayer2.lifemedia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * Created by cclab on 2017-04-21.
 */

public class BatteryCollect extends BroadcastReceiver {

    ContextInformation contextInfo;

    public BatteryCollect(ContextInformation contextInfo){
        this.contextInfo = contextInfo;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        int batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryRatio = (level * 100) / scale;

        if(isCharging && batteryRatio != 0){
            ContextInformation.batteryPercent = batteryRatio;
        }

        else if(batteryRatio < 100 && batteryRatio != 0){
            ContextInformation.batteryPercent = batteryRatio;
        }

    }
}
