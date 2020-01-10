package com.rtk.partnerconfig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private TextView mLogView;
    private Handler mHandler = new Handler();
    private Button mBtn1;
    private Button mBtn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log("isDebug="+ Build.IS_DEBUGGABLE);
        final Context ctx = this;

        mLogView = findViewById(R.id.log_view);
        mBtn1 = findViewById(R.id.button);
        mBtn2 = findViewById(R.id.button2);

        Intent intent = getIntent();
        if(intent!=null){
//            testMicStatusNotify();
//            finish();
        }

        if(mBtn1!=null){
            mBtn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
        if(mBtn2!=null){
            mBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
    }

    private void testMicStatusNotify(){
        log("start notify mic status changed");
        Uri uri = Uri.parse("content://tvlauncher.mic/farfield_mic_status");
        getApplicationContext().getContentResolver().notifyChange(uri, null);
    }

    private void log(final String s){
        if(mLogView!=null){
            mLogView.post(new Runnable() {
                @Override
                public void run() {
                    mLogView.append(s);
                    mLogView.append("\n");
                }
            });
        }
        Log.d(TAG, s);
    }

    @Override
    protected void onDestroy() {
        log("onDestroy end");
        super.onDestroy();
    }
}
