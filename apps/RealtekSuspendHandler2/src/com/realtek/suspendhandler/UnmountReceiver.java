package com.realtek.suspendhandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

/**
 * Realtek system suspend/resume handler
 * maybe useless in the future
 */
public class UnmountReceiver extends BroadcastReceiver {

    static final String TAG= "RealtekSuspendUnmount" ;

    // use service to manage wakeLock
    private SuspendService mService;
    public UnmountReceiver(SuspendService service){
        mService = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // check if is during suspend mode
        if(mService.mIsSuspendMode){
            String upath = intent.getDataString();
            Log.d(TAG, "onReceive :"+intent.getAction()+" "+intent.getDataString()+" "+upath);
        }
    }
}
