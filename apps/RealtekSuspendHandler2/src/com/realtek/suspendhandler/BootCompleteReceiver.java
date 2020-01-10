package com.realtek.suspendhandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.media.AudioManager;
import android.os.SystemProperties;

/**
 * Realtek system suspend/resume handler
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    static final String TAG= "RealtekSuspend" ;
    //private AudioManager mSound = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "intent : "+intent.getAction());

        // start service...
        Intent serviceIntent = new Intent(context, com.realtek.suspendhandler.SuspendService.class);
        context.startService(serviceIntent);

        /*
        boolean bCTSMode = true;
        mSound = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mSound != null && bCTSMode == true) {
            try {
                    mSound.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.01f);
                } catch (Exception ex) {
                    Log.e(TAG, "Bootcomplete to play EffectSound fail" + ex.toString());
                }
        }
        */
    }
}

