package com.realtek.hardware;

import java.util.StringTokenizer;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

//import android.app.ActivityThread;
//import android.media.AudioSystem;
import android.os.RemoteException;
import android.os.Handler;
//import android.os.ServiceManager;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;

import com.realtek.hardware.HDMIRxStatus;
import com.realtek.hardware.HDMIRxParameters;

/*
 * The implementation of the RtkHDMIRxManager.
 */
public class RtkHDMIRxManager {
    static {
        System.loadLibrary("rtk-display_ctrl_jni");
    }

    private static final String TAG = "RtkHDMIRxManager";

    public static final int HDMIRX_FORMAT_NV12 = 0;
    public static final int HDMIRX_FORMAT_ARGB = 1;
    public static final int HDMIRX_FORMAT_JPEG = 2;

    public static final int HDMIRX_FILE_FORMAT_TS  = 0;
    public static final int HDMIRX_FILE_FORMAT_MP4 = 1;

    public static final int HDMIRX_AUDIO_RAW    = 0;
    public static final int HDMIRX_AUDIO_PCM    = 1;

    private final String HdmiRxVideoSwitch = "/sys/devices/virtual/switch/rx_video/state";
    private final String HdmiRxAudioSwitch = "/sys/devices/virtual/switch/rx_audio/state";
    private final String HdmiRxVideoInfo = "/sys/class/video4linux/video250/hdmirx_video_info";
    private final String HdmiRxAudioInfo = "/sys/class/video4linux/video250/hdmirx_audio_info";

    private HDMIRxStatus mHDMIRxStatus = new HDMIRxStatus();

    private final Handler mHandler = new Handler();

    /**
     * A simple event listener to listen Rx event
     */
    public interface HDMIRxListener {
        /**
        * Event: Camera error, could be connected by another client
        */
        public static final int EVENT_CAMERA_ERROR = 1;
        public static final int EVENT_HDMIRX_VIDEO_HOTPLUG = 2;

        public abstract void onEvent(int eventId, int value);
    }

    public HDMIRxListener mListener = null;

    public RtkHDMIRxManager() {
    }

    public void setListener(HDMIRxListener listener){
        mListener = listener;
    }

    private void handleEventFromNative(int event, int value){
        Log.d(TAG, "native event "+event);

        final int fEvent = event;
        final int fValue = value;

        mHandler.post(new Runnable() {
            public void run() {
                if(mListener != null) {
                    mListener.onEvent(fEvent,fValue);
                }
            }
        });
    }

    public boolean isHDMIRxPlugged() {
        return getSwitchState(HdmiRxVideoSwitch);
    }

    public HDMIRxStatus getHDMIRxStatus() {
        parseRxInfo();
        return mHDMIRxStatus;
    }

	public boolean setHDMIRxAudio() {
	    boolean result = false;
	    if (getSwitchState(HdmiRxAudioSwitch)) {
            //AudioSystem.setHDMIRXstate(true);
            result = true;
        }
	    return result;
	}

    public boolean muteHDMIRxAudio() {
	    boolean result = false;
	    if (getSwitchState(HdmiRxAudioSwitch)) {
            //AudioSystem.setHDMIRXstate(false);
            result = true;
        }
	    return result;
    }

    public void prepare() {
        String packageName = "RtkHDMIRxManager";//ActivityThread.currentPackageName();
        Log.d(TAG, "hdmi rx prepare(), package name:" + packageName);
        native_prepare();
    }

    public int open(String packageName) {
        //String packageName = "RtkHDMIRxManager";//ActivityThread.currentPackageName();
        Log.d(TAG, "hdmi rx open(), package name:" + packageName);
        int retryLoop = 10;
        int hdmirxId = resolveHDMIRxCamId();
        while(hdmirxId < 0) {

            Log.e(TAG, "no HDMIRx found - retry");
            hdmirxId = resolveHDMIRxCamId();
            android.os.SystemClock.sleep(100);
            retryLoop--;
            if(retryLoop <= 0) {
                Log.e(TAG, "no HDMIRx found. break");
                break;
            }
        }

        if(hdmirxId < 0) {
            return -1;
        }

        return native_open(packageName,hdmirxId);
    }

    public void release() {
        Log.d(TAG, "hdmi rx release()");
        native_release();
    }

    public final void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        Log.d(TAG, "hdmi rx setPreviewDisplay(), holder:" + holder);
        Surface surface = (holder == null)? null : holder.getSurface();
        native_setPreviewDisplay(surface);
    }

    // added for TIF
    public final void setPreviewDisplay2(Surface surface) throws IOException {
        Log.d(TAG, "hdmi rx setPreviewDisplay2(), surface:" + surface);
        native_setPreviewDisplay(surface);
    }

    public final void setPreviewDisplay3(SurfaceTexture surfaceTexture) throws IOException {
        Surface surface = new Surface(surfaceTexture);
        native_setPreviewDisplay(surface);
    }

    public HDMIRxParameters getParameters() {
        HDMIRxParameters p = new HDMIRxParameters();
        String s = new String();

        try {
            s = native_getParameters();
        }
        catch(RuntimeException e) {
            Log.e(TAG, "getParameters Error: "+e);
        }

        p.unflatten(s);
        return p;
    }

    public void setParameters(HDMIRxParameters param){
        Log.d(TAG, "hdmi rx setParameters()");
        int width = 720;
        int height = 480;
        int fps = 30;

        if (param != null) {
            Size size = param.getPreviewSize();
            width = size.width;
            height = size.height;
            fps = param.getPreviewFrameRate();
        }

        Log.d(TAG, "hdmi rx preview size:(" + width + "," + height + "), fps:" + fps);
        native_setParameters(width, height, fps);
    }

    public int play() {
        Log.d(TAG, "hdmi rx play()");
        int result = native_startPreview();
        setHDMIRxAudio();// info AudioPilicy to set volume
        return result;
    }

    public int stop() {
        Log.d(TAG, "hdmi rx stop()");
        muteHDMIRxAudio();// to mute AO, avoid "pop noise"
        int result = native_stopPreview();
        return result;
    }

    public byte[] getSnapshot(int format, int x, int y, int cropWidth, int cropHeight, int outWidth, int outHeight) {
        //Log.d(TAG, "hdmi rx getSnapshot fmt:" + format + "(" + x + "," + y + "), " + cropWidth + "x" + cropHeight + " => " + outWidth + "x" + outHeight);
        return native_getSnapshot(format, x, y, cropWidth, cropHeight, outWidth, outHeight);
    }

    public void setTranscode(boolean on) {
        Log.d(TAG, "setTranscode on:"+on);

        if(on && getAudioMode() == HDMIRX_AUDIO_RAW){
            Log.e(TAG, "enable record during RAW mode");
        }

        native_setTranscode(on);
    }

    public void configureTargetFormat(RtkHDMIRxManager.VideoConfig vConfig, RtkHDMIRxManager.AudioConfig aConfig) {
        Log.d(TAG, "configureTargetFormat..");

        native_configureTargetFormat(vConfig.width, vConfig.height, 60,
                                     vConfig.bitrate, 1, 0, 1, 2048, 8,
                                     aConfig.channelCount, 0,
                                     aConfig.sampleRate, aConfig.bitrate);
    }

    public void setTargetFd(ParcelFileDescriptor fd, int fileFormat){
        Log.d(TAG, "setTargetFd fd:"+fd);
        native_setTargetFd(fd.getFd(), fileFormat);
    }

    public void setPlayback(boolean videoEn, boolean audioEn){
        native_setPlayback(videoEn,audioEn);
    }

    public int getAudioMode(){
        int rawOn = SystemProperties.getInt("persist.rtk.hdmirx.raw", 0);
        if(rawOn == 1)
            return HDMIRX_AUDIO_RAW;
        else
            return HDMIRX_AUDIO_PCM;
    }

    private void parseRxInfo() {
        Log.d(TAG, "*** In parseRxInfo ****");
        try {
            BufferedReader in;
            String line;
            int pos = 0;

            // parse HDMI Rx Video Info
            in = new BufferedReader(new FileReader(HdmiRxVideoInfo));

            // Type:HDMIRx
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(line.compareToIgnoreCase("HDMIRx") == 0) {
                    mHDMIRxStatus.type = HDMIRxStatus.TYPE_HDMI_RX;
                } else {
                    mHDMIRxStatus.type = HDMIRxStatus.TYPE_MIPI;
                }
                Log.d(TAG, "*** Video Type: " + line);
            }

            // Status:NotReady
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(line.compareToIgnoreCase("Ready") == 0) {
                    mHDMIRxStatus.status= HDMIRxStatus.STATUS_READY;
                } else {
                    mHDMIRxStatus.status = HDMIRxStatus.STATUS_NOT_READY;
                }
                Log.d(TAG, "*** Video Status: " + line);
            }

            // Width:0
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                mHDMIRxStatus.width = Integer.parseInt(line);;
                Log.d(TAG, "*** Video Width: " + mHDMIRxStatus.width);
            }

            // Height:0
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                mHDMIRxStatus.height = Integer.parseInt(line);
                Log.d(TAG, "*** Video Height: " + mHDMIRxStatus.height);
            }

            // ScanMode:Progressive
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(line.compareToIgnoreCase("Progressive") == 0) {
                    mHDMIRxStatus.scanMode = HDMIRxStatus.SCANMODE_PROGRESSIVE;
                } else {
                    mHDMIRxStatus.scanMode = HDMIRxStatus.SCANMODE_INTERLACED;
                }
                Log.d(TAG, "*** Video ScanMode: " + line);
            }

            // Color:RGB
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(line.compareToIgnoreCase("RGB") == 0) {
                    mHDMIRxStatus.color= HDMIRxStatus.COLOR_RGB;
                } else if (line.compareToIgnoreCase("YUV422") == 0) {
                    mHDMIRxStatus.color = HDMIRxStatus.COLOR_YUV422;
                } else if (line.compareToIgnoreCase("YUV444") == 0) {
                    mHDMIRxStatus.color = HDMIRxStatus.COLOR_YUV444;
                } else {
                    mHDMIRxStatus.color = HDMIRxStatus.COLOR_YUV411;
                }
                Log.d(TAG, "*** Video Color: " + line);
            }

            // Fps:0
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                mHDMIRxStatus.freq = Integer.parseInt(line);
                Log.d(TAG, "*** Video Framerate: " + mHDMIRxStatus.freq);
            }
            in.close(); // Always close a stream when you are done with it.

            // parse HDMI Rx Audio Info
            in = new BufferedReader(new FileReader(HdmiRxAudioInfo));

            // Ready:0
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(Integer.parseInt(line) == 1)
                    mHDMIRxStatus.audioStatus = HDMIRxStatus.STATUS_READY;
                else
                    mHDMIRxStatus.audioStatus = HDMIRxStatus.STATUS_NOT_READY;
                Log.d(TAG, "*** Audio Status: " + line);
            }

            // Freq:0
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                mHDMIRxStatus.freq = Integer.parseInt(line);
                Log.d(TAG, "*** Audio Samplerate: " + mHDMIRxStatus.freq);
            }

            // Status:NotReady
            if((line = in.readLine()) != null) {
                pos = line.indexOf(':');
                line = line.substring(pos+1);
                if(line.compareToIgnoreCase("Non-LPCM") == 0) {
                    mHDMIRxStatus.spdif = HDMIRxStatus.SPDIF_RAW;
                } else {
                    mHDMIRxStatus.spdif = HDMIRxStatus.SPDIF_LPCM;
                }
                Log.d(TAG, "*** SPDIF Type: " + line);
            }
            in.close();
        } catch (IOException e) {
            // Handle FileNotFoundException, etc. here
            Log.e(TAG, "*** IOException in parseRxInfo ***");
        }
    }

    private boolean getSwitchState(String path) {
        boolean state = false;
        try {
            String line;
            BufferedReader in = new BufferedReader(new FileReader(path));
            if((line = in.readLine()) != null) {
                if(Integer.parseInt(line) == 1)
                    state = true;
                else
                    state = false;
            }
        } catch (IOException e) {
            // Handle FileNotFoundException, etc. here
            Log.e(TAG, "*** IOException in getSwitchState ***");
        }

        Log.d(TAG, "getSwitchState state:" + state + " path:" + path);
        return state;
    }

    private int resolveHDMIRxCamId() {
        int n = android.hardware.Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int i = 0; i < n; i++) {
            android.hardware.Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                Log.i(TAG, "back camera found: " + i);
                return i;
            }
        }
        Log.i(TAG, "no back camera");
        return -1;
    }
    //-------------------------------------------------------------------

    private native int native_open(String packageName,int id);
    private native void native_prepare();
    private native void native_release();
    private native void native_setPreviewDisplay(Surface surface);
    private native final String native_getParameters();
    private native void native_setParameters(int width, int height, int fps);
    private native int native_startPreview();
    private native int native_stopPreview();
    private native byte[] native_getSnapshot(int format, int x, int y, int cropWidth, int cropHeight, int outWidth, int outHeight);

    // rx record function
    private native void native_setTranscode(boolean on);
    private native void native_setTargetFd(int fd, int format);
    private native void native_setPlayback(boolean videoEn, boolean audioEn);
    private native void native_configureTargetFormat(
            // video setting
            int w, int h, int fps,
            int vbitrate, int iframeInterval,
            int deinterlace, int bitrateMode,
            int level, int profile,
            // audio setting
            int channelCount, int channelMode, int sampleRate, int abitrate);

    /**
     * Image size (width and height dimensions).
     */
    static public class Size {
        /**
        * Sets the dimensions for pictures.
        *
        * @param w the photo width (pixels)
        * @param h the photo height (pixels)
        */
        public Size(int w, int h) {
            width = w;
            height = h;
        }
        /**
        * Compares {@code obj} to this size.
        *
        * @param obj the object to compare this size with.
        * @return {@code true} if the width and height of {@code obj} is the
        *		   same as those of this size. {@code false} otherwise.
        */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return width == s.width && height == s.height;
        }
        @Override
        public int hashCode() {
            return width * 32713 + height;
        }
        /** width of the picture */
        public int width;
        /** height of the picture */
        public int height;
    }

    /**
     * A simple class to put all video config parameters
     */
    public static class VideoConfig{
        public int width;
        public int height;
        public int bitrate;

        public VideoConfig(
                int w,
                int h,
                int bitrate){
            this.width = w;
            this.height = h;
            this.bitrate = bitrate;
        }
    }

    /**
     * A simple class to put all audio config parameters
     */
    public static class AudioConfig{
        public int channelCount;
        public int sampleRate;
        public int bitrate;

        public AudioConfig(
                int channelCount,
                int sampleRate,
                int bitrate){
            this.channelCount = channelCount;
            this.sampleRate = sampleRate;
            this.bitrate = bitrate;
        }
    }
}
