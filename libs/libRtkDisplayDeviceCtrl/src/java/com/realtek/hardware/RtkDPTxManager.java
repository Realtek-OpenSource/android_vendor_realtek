package com.realtek.hardware;

import java.util.Vector;

import android.util.Log;
import android.content.Context;
import android.os.Handler;

import com.realtek.hardware.RtkTVSystem;
import com.realtek.hardware.DPOutputFormat;

public class RtkDPTxManager {

    static class TVSystemInfo {

        int mSettingValue;
        int mVIC;
        int mFreqShift;

        int mWidth;
        int mHeight;
        boolean mProgress;
        int mFps;

        // backward compatiable.
        int mVideoSystem;
        int mVideoStandard;
        int mWideScreen;

        TVSystemInfo(
            int settingVal,
            int vic,
            int freqShift,
            int width,
            int height,
            boolean progress,
            int fps,
            int videoSystem,
            int wideScreen) {

            mSettingValue   = settingVal;
            mVIC            = vic;
            mFreqShift      = freqShift;
            mWidth          = width;
            mHeight         = height;
            mProgress       = progress;
            mFps            = fps;
            mVideoSystem    = videoSystem;

            if(progress) {
                mVideoStandard = RtkTVSystem.VIDEO_PROGRESSIVE;
            } else {
                mVideoStandard = RtkTVSystem.VIDEO_INTERLACED;
            }

            mWideScreen     = wideScreen;
        }

    }

    static class DPTxFormatSupport {
        int mVIC;
        TVSystemInfo mInfo = null;

        DPTxFormatSupport(int vic) {
            mVIC = vic;
            resolveTVSystemInfo(vic);
        }

        void dump() {
            Log.d(TAG, "VIC:"+mVIC+" ("
                +mInfo.mWidth+"x"
                +mInfo.mHeight+") "
                +mInfo.mFps);
        }

        private void resolveTVSystemInfo(int vic) {
            for(int i=0;i<sTVSystemInfos.size();i++) {
                TVSystemInfo info = sTVSystemInfos.get(i);
                if(info.mVIC == vic) {
                    mInfo = info;
                    return;
                }
            }
        }
        public int source() {
            if(mInfo == null) {
                Log.e(TAG, "DPTxFormatSupport source null error VIC:"+mVIC);
                return -1;
            }

            if(mInfo.mFreqShift > 0) {
                return -1;
            }

            int rank = mInfo.mHeight * mInfo.mHeight * mInfo.mFps;

            if(!mInfo.mProgress) {
                rank = rank/2;
            }
            return rank;
        }
    }

    public interface EventListener {
        public abstract void onEvent(int event);
    }

    private static final String TAG                 = "RtkDPTXManager";

    private static final boolean DEBUG              = true;

    public static final int EVENT_PLUG_IN           =  1;
    public static final int EVENT_PLUG_OUT          =  0;

    private static final int GET_MODE_CURRENT       = (1);
    private static final int GET_MODE_FACTORY       = (2);

    private static Vector<TVSystemInfo> sTVSystemInfos;

    static {
        System.loadLibrary("rtk-display_ctrl_jni");

        sTVSystemInfos = new Vector<TVSystemInfo>();

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_NTSC,
            RtkTVSystem.VIC_720_480I_60HZ,
            0,720,480,false,60,
            RtkTVSystem.VIDEO_NTSC,0));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_PAL,
            RtkTVSystem.VIC_720_576I_50HZ,
            0,720,576,false,50,
            RtkTVSystem.VIDEO_PAL,0));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_480P,
            RtkTVSystem.VIC_720_480P_60HZ,
            0,720,480,true,60,
            RtkTVSystem.VIDEO_NTSC,0));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_576P,
            RtkTVSystem.VIC_720_576P_50HZ,
            0,720,576,true,50,
            RtkTVSystem.VIDEO_PAL,0));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_720P_50HZ,
            RtkTVSystem.VIC_1280_720P_50HZ,
            0,1280,720,true,50,
            RtkTVSystem.VIDEO_HD720_50HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_720P_60HZ,
            RtkTVSystem.VIC_1280_720P_60HZ,
            0,1280,720,true,60,
            RtkTVSystem.VIDEO_HD720_60HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080I_50HZ,
            RtkTVSystem.VIC_1920_1080I_50HZ,
            0,1920,1080,false,50,
            RtkTVSystem.VIDEO_HD1080_50HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080I_60HZ,
            RtkTVSystem.VIC_1920_1080I_60HZ,
            0,1920,1080,false,60,
            RtkTVSystem.VIDEO_HD1080_60HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_50HZ,
            RtkTVSystem.VIC_1920_1080P_50HZ,
            0,1920,1080,true,50,
            RtkTVSystem.VIDEO_HD1080_50HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_60HZ,
            RtkTVSystem.VIC_1920_1080P_60HZ,
            0,1920,1080,true,60,
            RtkTVSystem.VIDEO_HD1080_60HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_24HZ,
            RtkTVSystem.VIC_3840_2160P_24HZ,
            0,3840,2160,true,24,
            RtkTVSystem.VIDEO_HD2160_24HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_25HZ,
            RtkTVSystem.VIC_3840_2160P_25HZ,
            0,3840,2160,true,25,
            RtkTVSystem.VIDEO_HD2160_25HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_30HZ,
            RtkTVSystem.VIC_3840_2160P_30HZ,
            0,3840,2160,true,30,
            RtkTVSystem.VIDEO_HD2160_30HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_4096_2160P_24HZ,
            RtkTVSystem.VIC_4096_2160P_24HZ,
            0,4096,2160,true,24,
            RtkTVSystem.VIDEO_HD4096_2160_24HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_24HZ,
            RtkTVSystem.VIC_1920_1080P_24HZ,
            0,1920,1080,true,24,
            RtkTVSystem.VIDEO_HD1080_24HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_720P_59HZ,
            RtkTVSystem.VIC_1280_720P_60HZ,
            1,1280,720,true,60,
            RtkTVSystem.VIDEO_HD720_59HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080I_59HZ,
            RtkTVSystem.VIC_1920_1080I_60HZ,
            1,1920,1080,false,60,
            RtkTVSystem.VIDEO_HD1080_59HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_23HZ,
            RtkTVSystem.VIC_1920_1080P_24HZ,
            1,1920,1080,true,24,
            RtkTVSystem.VIDEO_HD1080_23HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_59HZ,
            RtkTVSystem.VIC_1920_1080P_60HZ,
            1,1920,1080,true,60,
            RtkTVSystem.VIDEO_HD1080_59HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_23HZ,
            RtkTVSystem.VIC_3840_2160P_24HZ,
            1,3840,2160,true,24,
            RtkTVSystem.VIDEO_HD2160_23HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_29HZ,
            RtkTVSystem.VIC_3840_2160P_30HZ,
            1,3840,2160,true,30,
            RtkTVSystem.VIDEO_HD2160_29HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_60HZ,
            RtkTVSystem.VIC_3840_2160P_60HZ,
            0,3840,2160,true,60,
            RtkTVSystem.VIDEO_HD2160_60HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_50HZ,
            RtkTVSystem.VIC_3840_2160P_50HZ,
            0,3840,2160,true,50,
            RtkTVSystem.VIDEO_HD2160_50HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_4096_2160P_50HZ,
            RtkTVSystem.VIC_4096_2160P_50HZ,
            0,4096,2160,true,50,
            RtkTVSystem.VIDEO_HD4096_2160_50HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_4096_2160P_60HZ,
            RtkTVSystem.VIC_4096_2160P_60HZ,
            0,4096,2160,true,60,
            RtkTVSystem.VIDEO_HD4096_2160_60HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_4096_2160P_25HZ,
            RtkTVSystem.VIC_4096_2160P_25HZ,
            0,4096,2160,true,25,
            RtkTVSystem.VIDEO_HD4096_2160_25HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_4096_2160P_30HZ,
            RtkTVSystem.VIC_4096_2160P_30HZ,
            0,4096,2160,true,30,
            RtkTVSystem.VIDEO_HD4096_2160_30HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_2160P_59HZ,
            RtkTVSystem.VIC_3840_2160P_60HZ,
            1,3840,2160,true,60,
            RtkTVSystem.VIDEO_HD2160_59HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_25HZ,
            RtkTVSystem.VIC_1920_1080P_25HZ,
            0,1920,1080,true,25,
            RtkTVSystem.VIDEO_HD1080_25HZ,1));

        sTVSystemInfos.add(new TVSystemInfo(
            RtkTVSystem.TV_SYS_1080P_30HZ,
            RtkTVSystem.VIC_1920_1080P_30HZ,
            0,1920,1080,true,30,
            RtkTVSystem.VIDEO_HD1080_30HZ,1));
    }

    private static final Object sLock = new Object();
    private final Handler mHandler = new Handler();
    private Vector<DPTxFormatSupport> mEDIDSupportList =
        new Vector<DPTxFormatSupport>();

    private Context mContext = null;
    private static RtkDPTxManager sInstance = null;
    private EventListener mListener = null;


    public static RtkDPTxManager getRtkDPTxManager(Context c) {
        if(sInstance == null) {
            sInstance = new RtkDPTxManager(c);
        }
        return sInstance;
    }

    RtkDPTxManager(Context c) {
        Log.d(TAG, "RtkDPTxManager()");
        mContext = c;
        native_init();
        if(checkIfDPTxPlugged()) {
            getEDIDSupportList();
        }
    }

    private void getEDIDSupportList() {
        synchronized(sLock) {
            if(mEDIDSupportList.size() > 0) {
                Log.w(TAG, "Calling getEDIDSupportList, clear existing list first");
                mEDIDSupportList.clear();
            }
            native_getEDIDSupportList();
            Log.d(TAG, "getEDIDSupportList size "+mEDIDSupportList.size());
            for(int i=0;i<mEDIDSupportList.size();i++) {
                mEDIDSupportList.get(i).dump();
            }
        }
    }

    private void clearEDIDSupportList() {
        synchronized(sLock) {
            mEDIDSupportList.clear();
        }
    }

    private void onHandleDPTxEvent(int event) {
        Log.d(TAG, "DPTX-HIDL onHandleDPTxEvent event:"+event);
        switch(event) {
        case EVENT_PLUG_IN:
            mHandler.post(new Runnable() {
                public void run() {
                    getEDIDSupportList();

                    // for debug
                    getVideoFormat();

                    if(mListener != null) {
                        mListener.onEvent(event);
                    }
                }});
            break;
        case EVENT_PLUG_OUT:
            mHandler.post(new Runnable() {
                public void run(){
                    clearEDIDSupportList();

                    if(mListener != null) {
                        mListener.onEvent(event);
                    }

                }});
            break;
        default:
            break;
        }
    }

    public void release() {
        native_release();
        sInstance = null;
    }

    private int getSetupIDViaVICNoFreqShift(int vic) {
        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);
            if(info.mFreqShift == 0 && info.mVIC == vic) {
                return info.mSettingValue;
            }
        }
        return -1;
    }

    // public APIs
    public int[] getVideoFormat() {
        synchronized(sLock) {
            int[] supportVideoFormat = new int[RtkTVSystem.TV_SYS_NUM];

            for(int i=0;i<supportVideoFormat.length;i++) {
                supportVideoFormat[i] = 0;
            }

            int setupID = 0;
            for(int i=0;i<mEDIDSupportList.size();i++) {
                int vic = mEDIDSupportList.get(i).mVIC;
                setupID = getSetupIDViaVICNoFreqShift(vic);
                if(setupID > 0) {
                    supportVideoFormat[setupID] = 1;
                }
            }

            if(DEBUG) {
                Log.d(TAG, "getVideoFormat supportVideoFormat length:"+supportVideoFormat.length);
                for(int i=0;i<supportVideoFormat.length;i++) {
                    Log.d(TAG, "getVideoFormat:["+i+"]"
                        + supportVideoFormat[i]);
                }
            }

            return supportVideoFormat;
        }
    }

    public boolean checkIfDPTxPlugged() {
        if(native_hasDPTxBackend() < 0) {
            return false;
        }
        return native_checkIfDPTxPlugged();
    }

    public boolean getDPTxEDIDReady() {
        if(native_hasDPTxBackend() < 0) {
            return false;
        }
        return native_getDPTxEDIDReady();
    }

    private int resolveAutoVic() {

        if(!checkIfDPTxPlugged()){
            Log.e(TAG, "resolveAutoVic no DP is plugged");
            return -1;
        }

        if(mEDIDSupportList.size() <= 0) {
            Log.e(TAG, "resolveAutoVic no EDID support list");
            return -1;
        }

        DPTxFormatSupport fmt = mEDIDSupportList.get(0);
        for(DPTxFormatSupport item:mEDIDSupportList) {
            if(fmt.source() < item.source()) {
                fmt = item;
            }
        }
        Log.d(TAG, "resolveAutoVic: "+fmt.mVIC);
        return fmt.mVIC;
    }

    public int setTVSystem(int tvSystem) {

        if(native_hasDPTxBackend() < 0) {
            Log.d(TAG, "setTVSystem: do nothing - no DPTx");
            return 0;
        }

        Log.d(TAG,"setTVSystem:" + tvSystem);
        int vic = -1;

        if(tvSystem == RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT) {
            native_saveEdidAutoMode(RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID);
            vic = resolveAutoVic();
        }else{

            native_saveEdidAutoMode(RtkTVSystem.TV_SYSTEM_AUTO_MODE_OFF);

            for(int i=0;i<sTVSystemInfos.size();i++) {
                TVSystemInfo info = sTVSystemInfos.get(i);
                if(info.mSettingValue == tvSystem) {
                    vic = info.mVIC;
                    break;
                }
            }
        }

        Log.d(TAG, "found VIC: "+vic);
        if(vic != -1) {
            native_setOutputFormat(vic); // no flags
        }

        return 0;
    }

    public int getTVSystem() {

        if(native_hasDPTxBackend() < 0) {
            Log.d(TAG, "getTVSystem: has no DPTx do nothing");
            return RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT;
        }

        // check if auto prop is set
        if(native_readEdidAutoMode() == RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID) {
            Log.d(TAG, "Get TV System : Auto mode");
            return RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT;
        }

        DPOutputFormat fmt = new DPOutputFormat();
        native_getOutputFormat(fmt, GET_MODE_CURRENT);
        int vic = fmt.mVIC;
        int setupID = getSetupIDViaVICNoFreqShift(vic);
        Log.d(TAG, "[getTVSystem] VIC:"+ vic+" setupID:"+setupID);
        return setupID;
    }

    public int hasDPTxBackend() {
        return native_hasDPTxBackend();
    }

    public void setListener(EventListener listener) {
        mListener = listener;
    }

    // native functions.
    private native void native_release();
    private native void native_init();
    private native boolean native_checkIfDPTxPlugged();
    private native boolean native_getDPTxEDIDReady();
    private native void native_getEDIDSupportList();
    private native void native_setOutputFormat(int vic);
    private native int native_getOutputFormat(DPOutputFormat fmt, int mode);
    private native int native_hasDPTxBackend();

    private native void native_saveEdidAutoMode(int value);
    private native int native_readEdidAutoMode();

    private void from_native_addEDIDSupportListItem(int vic) {
        Log.d(TAG, "from_native_addEDIDSupportListItem : "+vic);
        DPTxFormatSupport data = new DPTxFormatSupport(vic);
        mEDIDSupportList.add(data);
    }

}
