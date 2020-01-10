package com.example.hdmirxdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.util.Log;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.content.pm.PackageManager;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.hardware.RtkHDMIRxManager.Size;
import com.realtek.hardware.HDMIRxStatus;
import com.realtek.hardware.HDMIRxParameters;

public class MyReceiver extends BroadcastReceiver {

    public static final String RX_STATUS_STRING = "android.intent.action.HDMIRX_PLUGGED";
    public static final String RX_HDCP_STRING = "android.intent.action.HDMIRX_HDCP_STATUS";

    public static final String VIDEO_STATUS_BC = "android.intent.action.VIDEO_STATUS";

    private static final String sApkLaunchActivityName = "com.example.hdmirxdemo.HDMIRxLauncher";

    static final String TAG= "HDMIRxDemoReceiver";
    public static BroadcastListener sListener = null;
    private Context mContext;
    private static Context sAppContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        sAppContext = context.getApplicationContext();

        String action = intent.getAction();
        Log.d(TAG, "intent: "+action);

        if(action.equals("android.intent.action.BOOT_COMPLETED")){
            HDMIDbHelper dbHelper = HDMIDbHelper.getInstance(context);
            GeneralService2.getInstance(context);
            startWatchdogService(context);

            switchMode(context);
        }else if(action.equals(RX_STATUS_STRING)){
            processHotPlugEvent(intent);
        }else if(action.equals(RX_HDCP_STRING)){
            processHDCPEvent(intent);
        }else if(action.equals(VIDEO_STATUS_BC)){
            processVideoStatus(intent, context);
        }
    }

    public static boolean isServiceMode() {
        String mode = SystemProperties.get(
                Keys.PROP_APK_MODE,
                Keys.ACTIVITY_MODE);

        if(mode.equals(Keys.SERVICE_MODE)) {
            return true;
        } else {
            return false;
        }
    }

    private void switchMode(Context c) {


        if(isServiceMode()) {
            /* Disable apk entry */
            disableComponent(c,sApkLaunchActivityName);
            /* Instance HDMIRxManager in background */
            HDMIRxManagerController.getInstance().prepare();
            /* Init first plugged event */
            if(HDMIRxManagerController.getInstance().isHDMIRxPlugged()) {
                startFloatingWindowServiceMode();
            }
        } else {
            enableComponent(c,sApkLaunchActivityName);
        }
    }

    private void enableComponent(Context context, String klass) {
        ComponentName name = new ComponentName(context, klass);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(name,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);
    }

    private void disableComponent(Context context, String klass) {
        ComponentName name = new ComponentName(context, klass);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(name,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }

    private void startWatchdogService(Context c){
        Intent serviceIntent = new Intent(
                c, com.example.hdmirxdemo.WatchdogService.class);
        c.startService(serviceIntent);
    }

    // cause VE1 shares encode and decode H264 tasks, so add this mechanism, don't care in 8.1
    private void processVideoStatus(Intent i, Context c){
        Log.d(TAG, "processVideoStatus");
        Bundle b = i.getExtras();
        boolean status = b.getBoolean("status",false);
        String mime = b.getString("mime");
        int width = b.getInt("width",-1);
        int height = b.getInt("height",-1);
        int fps = (int) (b.getFloat("frame-rate",-1f));

        if(width==-1 || height==-1 || fps == -1){
            Log.e(TAG,"invalid VIDEO_STATUS_BC, return");
            return;
        }

        if(status){
            GeneralService2.getInstance(c).insertVideoStatus(mime, width, height, fps);
        }else{
            GeneralService2.getInstance(c).deleteVideoStatus(mime, width, height, fps);
        }
    }

    private void showToast(String message) {
        Toast.makeText(mContext,message,Toast.LENGTH_SHORT).show();
    }

    public static void processHotPlugEvent(Intent intent){
        Log.d(TAG, "[THOR] processHotPlugEvent");

        if (!(intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false))){
            Log.d(TAG, "hdmi plug out");
            FloatingWindowService2.stopServiceItSelf();
            FloatingWindowService2.sHDMIRxStatus = null;
            FloatingWindowService2.sHDMIRxParameters = null;
        }else{
            Log.d(TAG, "hdmi plugged - service state:\""+
                    FloatingWindowService2.bServiceIsAlive+
                    "\"");

            //prepareHDMIInfoParameters();
            FloatingWindowViewGroup3.removePendingRxWarningDialog();

            /* Service mode action */
            if(isServiceMode()) {
                startFloatingWindowServiceMode();
            }
        }

        // sListener is MainActivity, it does following
        // 1. plug in  -> lunch FloatingWindowService
        // 2. plug out -> stop FloatingWindowService
        if(sListener != null){
            sListener.handleBroadcast(intent);
        }

    }

    private static void startFloatingWindowServiceMode() {
        if(FloatingWindowService2.bServiceIsAlive) {
            Log.d(TAG,"Service already exists");
        }else{
            Bundle b = new Bundle();
            b.putBoolean(Keys.KEY_TOUCHABLE,true);
            int[] pos = Keys.sServiceModeLocation;
            /* keep tracking of last window */
            pos[0] = Keys.serviceModeX;
            pos[1] = Keys.serviceModeY;

            b.putIntArray(Keys.KEY_DIMENSION,pos);
            boolean audioEnable = true;
            b.putBoolean(Keys.KEY_AUDIO_ON,audioEnable);
            Intent serviceIntent = new Intent(
                    sAppContext,
                    com.example.hdmirxdemo.FloatingWindowService2.class);
            serviceIntent.putExtra("rx",b);
            sAppContext.startService(serviceIntent);
        }
    }


    private void processHDCPEvent(Intent intent){
        Log.d(TAG,"processHDCPEvent");
        if(FloatingWindowService2.bServiceIsAlive){
            FloatingWindowService2.handleHDCPBroadcast(intent);
        }
    }
}

