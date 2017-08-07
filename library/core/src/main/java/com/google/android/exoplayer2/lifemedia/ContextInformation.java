package com.google.android.exoplayer2.lifemedia;

/**
 * Created by cclab on 2017-04-21.
 */

public class ContextInformation {

    //NetworkAttr
    public static String connectionType = "";
    public static long bandwidth = 0;
    public static long bitrateJitter = 0;
    public static int RSSI = 0;
    public static boolean onServerError = false;
    public static boolean contextEnable = false;
    public static long delay = 0;

    //DeviceAttr
    public static String deviceModel = "";
    public static String deviceType = "";
    public static int width = 0;
    public static int height = 0;
    public static double bufferMs  = 0;
    public static int batteryPercent = 0;
    public static String androidID = "";

    //Request Range
    public static String requestRange = "";

    //Get Response Header
    public static int responseQuality = 0;

    public ContextInformation(){}

    @SuppressWarnings("static-access")
    public ContextInformation(String connectionType, long bandwidth, int delayMs, long bitrateJitter, int RSSI, String deviceModel, String deviceType,
                              int width, int height, int bufferMs, int batteryPercent, String androidID){
        this.connectionType = connectionType;
        this.bandwidth = bandwidth;
        this.bitrateJitter = bitrateJitter;
        this.RSSI = RSSI;

        this.deviceModel = deviceModel;
        this.width = width;
        this.height = height;
        this.bufferMs = bufferMs;
        this.batteryPercent = batteryPercent;
        this.androidID = androidID;
    }

}
