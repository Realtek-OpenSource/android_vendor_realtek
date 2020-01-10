package com.rtk.SourceIn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.UEventObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.rtk.SourceIn.HDMIRxPlayer;

import java.io.File;
import java.io.FileNotFoundException;

public class RTKSourceInActivity extends Activity 
{
    private static final int PERMISSIONS_REQUEST_CAMERA = 1;

    private String TAG="HDMIRxActivity";
    private ViewGroup m_Root;

    private HDMIRxPlayer m_HDMIRxPlayer = null;
    private final Handler mHandler = new Handler();
    private byte[] mCapture;
    private static final long SCREENSHOT_SLOT = 200;
    private boolean mIsFullScreen = true;

    private File mRecordFile;
    private boolean mRecordOn = false;
    private boolean mHDMIPlugged = false;

    private final String HdmiRxSwitch      = "/sys/devices/virtual/switch/rx_video/state";
    private final String HdmiRxUeventKeyword      = "DEVPATH=/devices/virtual/switch/rx_video";

    private final Runnable mHotplugRunnable = new Runnable() {
        public void run() {
            if(m_HDMIRxPlayer != null) {
                if(mHDMIPlugged) {
                    m_HDMIRxPlayer.play();
                } else {
                    m_HDMIRxPlayer.stop();
                }
            }
        }
    };

	private UEventObserver mHDMIRxObserver = new UEventObserver() {
		@Override
		public void onUEvent(UEventObserver.UEvent event) {
            //Log.w(TAG, "mHDMIRxObserver:onUEvent");
            //mHDMIPlugged = "1".equals(event.get("SWITCH_STATE"));
            //mHandler.post(mHotplugRunnable);
		}
	};

    private final Runnable mScreenShot = new Runnable() {
        @Override
        public void run() {
            if(m_HDMIRxPlayer==null)
                return;
            if(m_HDMIRxPlayer.isPlaying()== false) {
                mHandler.postDelayed(this, SCREENSHOT_SLOT);
                return;
            }
            CaptureTask capTask = new CaptureTask(1, 0, 0, 1280, 720, 320, 180);
            capTask.execute();
            mHandler.postDelayed(this, SCREENSHOT_SLOT);
        }
    };

    private void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        }else{
            Log.d(TAG, "getPermission OK");
        }
    }

    public void onRequestPermissionsResult (int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "get camera permission success");
                //m_HDMIRxPlayer = new HDMIRxPlayer(this, m_Root, 1920, 1080);
            } else {
                Log.e(TAG,"Camera permission denied, can't do anything");
            }
            finish();
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        getPermission();
        mRecordOn = false;
        //Make app to full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_hdmirx);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override 
    public void onStop()
    {
        Log.d(TAG,"onStop");
        super.onStop();
        mRecordOn = false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        boolean retval = false;
        if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
            Log.d(TAG,"Key code = "+event.getKeyCode());
            if(m_HDMIRxPlayer!=null)
                Log.d(TAG,"isPlaying() = "+m_HDMIRxPlayer.isPlaying());
        }

        if( event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer!=null && m_HDMIRxPlayer.isPlaying() == false)
            {
                m_HDMIRxPlayer.play();
            }
            retval = true;
        }
        else if( event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_STOP)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer!=null && m_HDMIRxPlayer.isPlaying() == true)
            {
                m_HDMIRxPlayer.stop();
            }
            retval = true;
        }
        else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer!=null && m_HDMIRxPlayer.isPlaying() == true)
            {
                if(mIsFullScreen==true) {
                    m_HDMIRxPlayer.setSurfaceSize(720, 480);
                    mIsFullScreen=false;
                }
                else {
                    m_HDMIRxPlayer.setSurfaceSize(1920, 1080);
                    mIsFullScreen=true;
                }
            }
            retval = true;
        }
        else if( event.getKeyCode() == KeyEvent.KEYCODE_INFO)
        {
            m_HDMIRxPlayer.showStatusToast(false);
            retval = true;
        }else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT){
            if(event.getAction() == KeyEvent.ACTION_DOWN){

                if(mRecordOn){
                    Toast.makeText(this, "record instance exists",Toast.LENGTH_SHORT).show();
                }else{
                    // transcode on
                    Log.d(TAG+"-Record","transcode on");
                    mRecordFile = getRecordFile();
                    try{
                        Log.d(TAG, "mRecordFile:"+mRecordFile);
                        int mode = ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_READ_WRITE;
                        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(mRecordFile,mode);
                        m_HDMIRxPlayer.startRecord(pfd);
                        Toast.makeText(this, "Start HDMI record to "+mRecordFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
                        mRecordOn = true;
                    } catch(FileNotFoundException e){
                        //e.printStackTrace();
                        finish();
                        overridePendingTransition(0,0);
                    }
                }
            }
            retval = true;
        }else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT){
            if(event.getAction() == KeyEvent.ACTION_DOWN){

                if(mRecordOn){
                    // transcode off
                    Log.d(TAG+"-Record", "transcode off");
                    m_HDMIRxPlayer.stopRecord();
                    mRecordFile = null;
                    Toast.makeText(this, "Stop HDMI record",Toast.LENGTH_SHORT).show();
                    mRecordOn = false;
                }else{
                    Toast.makeText(this, "no record instance exists",Toast.LENGTH_SHORT).show();
                }
            }
            retval = true;
        }else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP){
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                m_HDMIRxPlayer.togglePreview(true,true);
            }
            retval = true;
        }else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN){
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                m_HDMIRxPlayer.togglePreview(true,false);
            }
            retval = true;
        }
        if (retval ==false)
        {
            retval = super.dispatchKeyEvent(event);
        } 
        return retval;
    }
	
    private File getRecordFile(){
        return CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO, true, this);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG,"onResume");
        super.onResume();
        if(hasRtkMusicPlaybackService()) {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "stop");
            sendBroadcast(i);
        }
        m_Root = (ViewGroup) findViewById(R.id.root);
        m_HDMIRxPlayer = new HDMIRxPlayer(this, m_Root, 1920, 1080);
//        mHandler.postDelayed(mScreenShot, SCREENSHOT_SLOT);
        //mHDMIRxObserver.startObserving(HdmiRxUeventKeyword);
    }
	  
    @Override 
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG,"Configuration Change :"+newConfig);
        Log.d(TAG,"Configuration Change keyboard: "+newConfig.keyboard);
        Log.d(TAG,"Configuration Change keyboardHidden: "+newConfig.keyboardHidden);
    }

    @Override
    public void onPause()
    {
        Log.d(TAG,"onPause");
        super.onPause();
        //mHDMIRxObserver.stopObserving();

        mHandler.removeCallbacks(mScreenShot);
        if(m_HDMIRxPlayer!=null) {
            if(m_HDMIRxPlayer.isPlaying() == true)
            {
                m_HDMIRxPlayer.stop();
            }
            m_HDMIRxPlayer.release();
            m_HDMIRxPlayer = null;
        }
        finish();
        overridePendingTransition(0,0);
    } 
	  
    @Override
    public void onDestroy()
    {
        Log.d(TAG,"onDestroy");
        super.onDestroy();	
    }

    public boolean hasRtkMusicPlaybackService(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.android.music.MediaPlaybackService".equals(service.service.getClassName())&&service.foreground){
                Log.i("BGMusic","\033[0;31;31m MusicPlayback is running \033[m");
                return true;
            }
        }
        Log.i("BGMusic","\033[0;31;31m No MusicPlayback is running \033[m");
        return false;
    }

    public class CaptureTask extends AsyncTask<Void, Void, byte[]> {
        int x=0;
        int y=0;
        int width=0;
        int height=0;
        int outWidth=0;
        int outHeight=0;
        int type=0;
        public CaptureTask(int type, int x, int y, int width, int height, int outWidth, int outHeight)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.outWidth = outWidth;
            this.outHeight = outHeight;
            this.type = type;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected byte[] doInBackground(Void... para)
        {
            if(m_HDMIRxPlayer==null || m_HDMIRxPlayer.isPlaying()== false)
                return null;

            byte[] data = m_HDMIRxPlayer.capture(type, x, y, width, height, outWidth, outHeight);
            return data;
        }

        @Override
        protected void onPostExecute (byte[] data)
        {
        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();
            Log.d(TAG,"captureTask onCancelled ");
        }
        @Override
        protected void finalize ()throws Throwable
        {
            super.finalize();
        }
    }
}
