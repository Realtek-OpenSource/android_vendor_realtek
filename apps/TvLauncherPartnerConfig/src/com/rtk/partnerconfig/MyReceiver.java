package com.rtk.partnerconfig;

import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import java.lang.*;

public class MyReceiver extends BroadcastReceiver {
	private static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = intent.getAction();
        Log.i(TAG, "receive intent:" + action);
    }
}