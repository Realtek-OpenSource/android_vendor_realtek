package com.example.hdmirxdemo;

import java.io.IOException;

//import android.content.Context;
import android.content.Intent;

import android.os.ParcelFileDescriptor;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.graphics.SurfaceTexture;
import android.util.Log;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.hardware.RtkHDMIRxManager.HDMIRxListener;
import com.realtek.hardware.RtkHDMIRxManager.Size;
import com.realtek.hardware.HDMIRxStatus;
import com.realtek.hardware.HDMIRxParameters;

public class HDMIRxManagerController implements HDMIRxListener {

    private static final String TAG = "HDMIRxManagerController";
    private static final String TAG_VERBOSE = TAG+"-VERBOSE";

    private static final boolean DEBUG = Keys.HDMIRX_MANAGER_DEBUG_VERBOSE;

    private static HDMIRxManagerController sInstance = null;
    private static Object sLock = new Object();

    public static HDMIRxManagerController getInstance(){
        if(sInstance == null) {
            sInstance = new HDMIRxManagerController();
        }
        return sInstance;
    }

    private RtkHDMIRxManager mHDMIRX;
    private boolean mOpened = false;

    HDMIRxManagerController() {
        Log.d(TAG, "HDMIRxManagerController()");
        mHDMIRX = new RtkHDMIRxManager();
        mHDMIRX.setListener(this);
        mOpened = false;
    }

    // work-around hotplug
    public void onEvent(int eventId, int value) {
        if(eventId == HDMIRxListener.EVENT_HDMIRX_VIDEO_HOTPLUG) {
            Log.d(TAG, "[THOR] onEvent: "+eventId+" value:"+value);
            Intent i = null;
            if(value > 0) {
                i = new Intent(MyReceiver.RX_STATUS_STRING).putExtra(
                        HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE,
                        true);
            }else{
                i = new Intent(MyReceiver.RX_STATUS_STRING).putExtra(
                        HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE,
                        false);
            }
            // call static api
            MyReceiver.processHotPlugEvent(i);
        }
    }

    public void registerRxListener() {
        if(mHDMIRX != null) {
            mHDMIRX.setListener(this);
        }
    }

    public static boolean isOpened(){
        synchronized(sLock){
            return getInstance().mOpened;
        }
    }

    public static int open(String pkgName){
        synchronized(sLock){

            if(DEBUG){
                Log.v(TAG_VERBOSE, "call open");
            }

            if(getInstance().mOpened){
                Log.w(TAG, "Call mHDMIRX open, but it's already beend open successed");
                return 0;
            }

            int rst = getInstance().mHDMIRX.open(pkgName);
            if(rst == 0){
                getInstance().mOpened = true;
            }else{
                Log.e(TAG, "[THOR] mHDMIRX open failed - call release");
                getInstance().mHDMIRX.release();
                getInstance().mHDMIRX.prepare();
                getInstance().registerRxListener();
                getInstance().mOpened = false;
            }
            return rst;
        }
    }

    public static void stop() {
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call stop");
            }
            if(getInstance().mOpened){
                getInstance().mHDMIRX.stop();
            }else{
                Log.w(TAG, "calling stop under none open state.");
            }
        }
    }

    public static void prepare() {
        synchronized(sLock) {
            getInstance().mHDMIRX.prepare();
            getInstance().registerRxListener();
        }
    }

    public static void release(){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call release");
            }
            Log.d(TAG,"[THOR] call release()");
            getInstance().mHDMIRX.release();
            Log.d(TAG,"[THOR] call prepare()");
            getInstance().mHDMIRX.prepare();
            getInstance().registerRxListener();
            getInstance().mOpened = false;
        }
    }

    public static void setListener(RtkHDMIRxManager.HDMIRxListener l){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setListener");
            }
            getInstance().mHDMIRX.setListener(l);
        }
    }

    public static HDMIRxStatus getHDMIRxStatus(){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call getHDMIRxStatus");
            }
            return getInstance().mHDMIRX.getHDMIRxStatus();
        }
    }

    public static HDMIRxParameters getParameters(){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call getParameters");
            }
            return getInstance().mHDMIRX.getParameters();
        }
    }

    public static void setPreviewDisplay (SurfaceHolder holder) throws IOException {
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setPreviewDisplay");
            }
            getInstance().mHDMIRX.setPreviewDisplay(holder);
        }
    }

    public static void setPreviewDisplay3 (SurfaceTexture surfaceTexture) throws IOException {
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setPreviewDisplay3");
            }
            getInstance().mHDMIRX.setPreviewDisplay3(surfaceTexture);
        }
    }

    public static void setParameters(HDMIRxParameters param){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setParameters");
            }
            getInstance().mHDMIRX.setParameters(param);
        }
    }

    public static void play(){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call play");
            }
            getInstance().mHDMIRX.play();
        }
    }

    public static int getAudioMode(){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call getAudioMode");
            }
            return getInstance().mHDMIRX.getAudioMode();
        }
    }

    public static void configureTargetFormat(RtkHDMIRxManager.VideoConfig v, RtkHDMIRxManager.AudioConfig a){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call configureTargetFormat");
            }
            getInstance().mHDMIRX.configureTargetFormat(v,a);
        }
    }

    public static void setTargetFd(ParcelFileDescriptor pfd, int format){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setTargetFd");
            }
            getInstance().mHDMIRX.setTargetFd(pfd,format);
        }
    }

    public static void setTranscode(boolean transcode) {
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setTranscode");
            }
            getInstance().mHDMIRX.setTranscode(transcode);
        }
    }

    public static boolean isHDMIRxPlugged() {
        synchronized(sLock){
            return getInstance().mHDMIRX.isHDMIRxPlugged();
        }
    }

    public static void setPlayback(boolean v, boolean a){
        synchronized(sLock){
            if(DEBUG){
                Log.v(TAG_VERBOSE, "call setPlayback");
            }
            getInstance().mHDMIRX.setPlayback(v,a);
        }
    }
}
