package com.realtek.hardware;

import java.util.Vector;

import android.os.SystemProperties;
import android.os.Handler;
import android.content.Context;
import android.util.Log;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.realtek.hardware.RtkTVSystem;
import com.realtek.hardware.ColorSet;
import com.realtek.hardware.OutputFormat;

public class RtkHDMIManager2 {

    public static final String PROPERTY_IGNORE_EDID = "persist.vendor.rtk.ignore.edid";

    public static final int COLOR_POLICY_AUTO       = (1);
    public static final int COLOR_POLICY_DEEP_AUTO  = (2);

    private static final boolean DEBUG = false;
    private static final boolean SYNC_SETTING = false;

    private static final String TAG = "RtkHDMIManager2";

    private static final String PROPERTY_DEEP_COLOR_FIRST = "persist.vendor.rtk.deep.first";

    public static final int HDCP_22                 = 22;
    public static final int HDCP_14                 = 14;
    public static final int HDCP_NONE               =  0;

    public static final int EVENT_PLUG_IN           =  1;
    public static final int EVENT_PLUG_OUT          =  0;

    private static final int DEPTH_NONE_MASK        = (0x0f); // all open
    private static final int DEPTH_8BIT_MASK        = (1<<1);
    private static final int DEPTH_10BIT_MASK       = (1<<2);
    private static final int DEPTH_12BIT_MASK       = (1<<3);

    private static final int SUPPORT_3D_MASK        = (1<<0);

    private static final int EXTRA_SAVE_TO_FACTORY          = (1<<0);   // save the result to factory
    private static final int EXTRA_FORCE_OUTPUT_FORMAT      = (1<<1);   // do not check the relationship of HDR mode , DV mode
    private static final int EXTRA_IGNORE_CURRENT           = (1<<2);   // set tv system and ignore if new output format is the same with current setting
    private static final int EXTRA_USE_NOW_COLOR_IF_SDR     = (1<<3);   // if is a SDR TV, then use current color mode
    private static final int EXTRA_RESET_COLOR_MODE         = (1<<4);   // reset color mode this time
    private static final int EXTRA_RESET_TV_SYSTEM          = (1<<5);   // reset TV system this time
    private static final int EXTRA_TRY_TO_USE_CURRENT_COLOR = (1<<6);   // RtkHDMIManager2 set TV system, try to use current color mode

    private static final int GET_MODE_CURRENT           = (1);
    private static final int GET_MODE_FACTORY           = (2);

    // sync with android/device/realtek/proprietary/libs/HDMILib/HdmiConstDef.h
    private static final int TV_SYS_COLOR_USE_DEFAULT   = (0xab);

    /**
     * struct hdmi_format_setting - HDMI output format setting
     * @mode: 0-OFF; 1-DVI; 2-HDMI
     * @vic: Video ID code
     * @freq_shift:
     * @color: 0-RGB444; 1-YUV422; 2-YUV444; 3-YUV420
     * @color_depth: 8-8bit, 10-10bit, 12-12bit
     * @_3d_format:
     * @hdr: 0-No hdr; 1-Dolby Vision; 2-HDR10
     */
    public static final int Depth8Bit       =    8;
    public static final int Depth10Bit      =   10;
    public static final int Depth12Bit      =   12;
    public static final int DepthNone       = 0xff;
    public static final int DepthAuto       = Depth8Bit;

    public static final int ColorRGB444     =    0;
    public static final int ColorYUV422     =    1;
    public static final int ColorYUV444     =    2;
    public static final int ColorYUV420     =    3;
    public static final int ColorNone       = 0xff;

    // 3D mode
    private static final int Mode_3D_Off    = 0;
    private static final int Mode_3D_FP     = 1;
    private static final int Mode_3D_SS     = 2;
    private static final int Mode_3D_TB     = 3;

    private static final Object sLock = new Object();
    private final Handler mHandler = new Handler();

    public interface EventListener {
        public abstract void onEvent(int event);
    }

    private EventListener mListener = null;

    public class HDMIDisplayFormat {
        public int mTVSystem;
        public int mColor;
        public int mColorDepth;
        public int m3DFormat;
        public int mHDR;
        public int mVIC;

        public int mEdidAutoMode;
        public int mColorModeEnum;

        private TVSystemInfo mInfo;
        private OutputFormat mFormat;

        HDMIDisplayFormat(OutputFormat fmt /*, OutputFormat factory*/ ) {

            mInfo = findTVSystemInfoViaOutputFormat(fmt);
            mFormat = fmt;

            mEdidAutoMode = native_readEdidAutoMode();

            mTVSystem = mInfo.mSettingValue;

            if(mEdidAutoMode == RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID) {
                mTVSystem = RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT;
            }

            mColor = fmt.mColor;
            mColorDepth = fmt.mColorDepth;
            m3DFormat = fmt.m3DFormat;
            mHDR = fmt.mHDR;

            if(native_readHDRAutoProperty() == 1) {
                Log.d(TAG, "HDMIDisplayFormat report HDR AUTO ENUM");
                mHDR = RtkTVSystem.VO_HDR_CTRL_AUTO;
            }

            mVIC = fmt.mVIC;
            mColorModeEnum = native_readColorModeEnum();

            /* seems useless, remove me and test
            if(mColorModeEnum == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT
            || mColorModeEnum == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {
                // convert nothing
            }else{
                mColorModeEnum = findColorModeEnum(mColor, mColorDepth);
            }
            */

            Log.d(TAG, "HDMI-HIDL HDMIDisplayFormat: "+this);

        }

        public String toString() {

            String colorString = getColorString(mColor);
            String hdrString = getHDRString(mHDR);
            String rst = ""+mTVSystem
                         +" "+mInfo.mWidth
                         +"x"+mInfo.mHeight
                         +" "+colorString
                         +","+mColorDepth
                         +" HDR:"+hdrString
                         +" ColorMode:"+mColorModeEnum
                         +" EDID Auto:"+mEdidAutoMode;

            return rst;
        }
    }

    /**
     * struct hdmi_format_support - Color format and 3D support for specific VIC
     * @vic: Video ID Code
     * @rgb444: b'0-Support RGB444, b'1-RGB444/8bit, b'2-RGB444/10bit, b'3-RGB444/12bit
     * @yuv422: b'0-Support YUV422, b'1-YUV422/8bit, b'2-YUV422/10bit, b'3-YUV422/12bit
     * @yuv444: b'0-Support YUV444, b'1-YUV444/8bit, b'2-YUV444/10bit, b'3-YUV444/12bit
     * @yuv420: b'0-Support YUV420, b'1-YUV420/8bit, b'2-YUV420/10bit, b'3-YUV420/12bit
     * @support_3d: b'0-Support 3D, b'1-FramePacking, b'2-TopAndBottom, b'3-SideBySideHalf
     * @support_fs: b'0-Support Frequency-Shift, b'1-Support FS 3D
     */
    static class HDMIFormatSupport {
        int mVIC;
        int mRgb444;
        int mYuv422;
        int mYuv444;
        int mYuv420;
        int mSupport3D;
        int msupportFS;
        int mReserved;

        TVSystemInfo mInfo;

        static Vector<ColorSet> sDeepColorList;

        static {
            sDeepColorList = new Vector<ColorSet>();

            sDeepColorList.add(new ColorSet(ColorYUV420,Depth12Bit));
            sDeepColorList.add(new ColorSet(ColorYUV420,Depth10Bit));

            sDeepColorList.add(new ColorSet(ColorYUV444,Depth12Bit));
            sDeepColorList.add(new ColorSet(ColorYUV444,Depth10Bit));

            sDeepColorList.add(new ColorSet(ColorYUV422,Depth12Bit));
            sDeepColorList.add(new ColorSet(ColorYUV422,Depth10Bit));

            sDeepColorList.add(new ColorSet(ColorRGB444,Depth12Bit));
            sDeepColorList.add(new ColorSet(ColorRGB444,Depth10Bit));
        }

        public HDMIFormatSupport (
            int vic,
            int rgb444,
            int yuv422,
            int yuv444,
            int yuv420,
            int support3D,
            int supportFS,
            int reserved) {

            mVIC = vic;
            mRgb444 = rgb444;
            mYuv422 = yuv422;
            mYuv444 = yuv444;
            mYuv420 = yuv420;
            mSupport3D = support3D;
            msupportFS = supportFS;
            mReserved = reserved;

            resolveTVSystemInfo(vic);
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

        private void dump() {
            Log.d(TAG, "VIC:"+mVIC+
                    "("+mInfo.mWidth+"x"+mInfo.mHeight+")"+
                    " FPS:"+mInfo.mFps+
                    " RGB:"+Integer.toBinaryString(mRgb444)+
                    " 422:"+Integer.toBinaryString(mYuv422)+
                    " 444:"+Integer.toBinaryString(mYuv444)+
                    " 420:"+Integer.toBinaryString(mYuv420)+
                    " 3D:" +Integer.toBinaryString(mSupport3D)+
                    " FS:" +Integer.toBinaryString(msupportFS));
        }

        private int getBitMask(int colorDepth) {
            switch(colorDepth) {
            case Depth8Bit: //case DepthAuto:
                return DEPTH_8BIT_MASK;
            case Depth10Bit:
                return DEPTH_10BIT_MASK;
            case Depth12Bit:
                return DEPTH_12BIT_MASK;
            case DepthNone:
            default:
                return DEPTH_NONE_MASK;
            }
        }

        public boolean isColorModeDepthSupported(int colorMode, int colorDepth) {

            int mask = getBitMask(colorDepth);
            boolean rst = false;

            switch(colorMode) {
            case ColorRGB444:
                rst = (mRgb444&mask)>0?true:false;
                break;
            case ColorYUV444:
                rst = (mYuv444&mask)>0?true:false;
                break;
            case ColorYUV422:
                rst = (mYuv422&mask)>0?true:false;
                break;
            case ColorYUV420:
                rst = (mYuv420&mask)>0?true:false;
                break;
            case ColorNone:
            default:
                rst = false;
                break;
            }

            if(DEBUG) {
                Log.d(TAG, "isColorModeDepthSupported colorMode:"
                        + colorMode
                        + " colorDepth:"
                        + colorDepth
                        + " rst:"
                        + rst);
            }

            return rst;
        }

        /* Replaced by native_getColorSet
        public ColorSet getAutoColorSetDeepColor() {
            for(int i=0;i<sDeepColorList.size();i++) {
                ColorSet val = sDeepColorList.get(i);
                if(isColorModeDepthSupported(val.mColorMode,val.mColorDepth)) {
                    return val;
                }
            }
            return null;
        }
        */

        /* Replaced by native_getColorSet
        public ColorSet getAutoColorSet() {

            // if user does not specify color depth, then 8bit is perfered.
            int colorDepth = Depth8Bit;
            int colorMode = ColorNone;

            if((mYuv444 & DEPTH_8BIT_MASK) > 0) {
                colorMode = ColorYUV444;
            }else if((mYuv422 & DEPTH_8BIT_MASK) > 0) {
                colorMode = ColorYUV422;
            }else if((mRgb444 & DEPTH_8BIT_MASK) > 0) {
                colorMode = ColorRGB444;
            }else if((mYuv420 & DEPTH_8BIT_MASK) > 0) {
                colorMode = ColorYUV420;
            }

            return new ColorSet(colorMode, colorDepth);
        }
        */

        public boolean isSupport3D() {
            if((mSupport3D & SUPPORT_3D_MASK) > 0){
                return true;
            }else{
                return false;
            }
        }
    }

    static class ColorEnumInfo {
        int mSettingValue;
        ColorSet mColorSet;

        ColorEnumInfo(int settingVal,
            ColorSet colorSet) {

            mSettingValue = settingVal;
            mColorSet = colorSet;
        }
    }

    /**
     * Simple class to record related info map to TVSystem in setting, including:
     * - vic
     * - freqShift
     * - color
     * - color depth
     * - 3d format
     * - hdr
     */
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
            mVideoStandard  = progress?RtkTVSystem.VIDEO_PROGRESSIVE:RtkTVSystem.VIDEO_INTERLACED;
            mWideScreen     = wideScreen;
        }

        public int ranking() {

            // not in support list
            if(getFormatSupport(mVIC) == null) {
                return -1;
            }

            // hide freqShift items
            if(mFreqShift > 0) {
                return -1;
            }

            // if auto mode is selected, max resolution is 3840x2160
            if(mWidth==4096 && mHeight==2160) {
                return -1;
            }

            int height = mProgress?mHeight:(mHeight/2);
            return mWidth*height*mFps;
        }
    }

    private static Vector<TVSystemInfo> sTVSystemInfos;
    private static Vector<ColorEnumInfo> sColorModes;

    static {
        System.loadLibrary("rtk-display_ctrl_jni");

        sTVSystemInfos = new Vector<TVSystemInfo>();
        sColorModes = new Vector<ColorEnumInfo>();

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

        // color mode map
        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_RGB444_8BIT,
            new ColorSet(ColorRGB444,
                Depth8Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_RGB444_10BIT,
            new ColorSet(ColorRGB444,
                Depth10Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_RGB444_12BIT,
            new ColorSet(ColorRGB444,
                Depth12Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr444_8BIT,
            new ColorSet(ColorYUV444,
                Depth8Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr444_10BIT,
            new ColorSet(ColorYUV444,
                Depth10Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr444_12BIT,
            new ColorSet(ColorYUV444,
                Depth12Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr422,
            new ColorSet(ColorYUV422,
                Depth8Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr420_8BIT,
            new ColorSet(ColorYUV420,
                Depth8Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr420_10BIT,
            new ColorSet(ColorYUV420,
                Depth10Bit)));

        sColorModes.add(new ColorEnumInfo(
            RtkTVSystem.TV_SYS_YCbCr420_12BIT,
            new ColorSet(ColorYUV420,
                Depth12Bit)));

    }

    private static Vector<HDMIFormatSupport> mEDIDSupportList =
        new Vector<HDMIFormatSupport>();

    private static RtkHDMIManager2 sInstance = null;

    private Context mContext;

    // for ignore EDID support.
    private ColorSet mAutoDeepColorSetIgnoreEDID = new ColorSet(ColorYUV420,Depth12Bit);
    private ColorSet mAutoColorSetIgnoreEDID = new ColorSet(ColorRGB444,Depth8Bit);

    /*
    private OutputFormat mOutputFormatFactory = new OutputFormat();
    private OutputFormat mOutputFormatCurrent = new OutputFormat();
    */

    public static RtkHDMIManager2 getRtkHDMIManager(Context c) {
        if(sInstance == null) {
            sInstance = new RtkHDMIManager2(c);
        }
        return sInstance;
    }

    RtkHDMIManager2(Context c) {
        Log.d(TAG, "RtkHDMIManager2()");
        mContext = c;
        if(DEBUG) {
            dumpTVSystemInfoMap();
        }
        initHDMIInfo();
    }

    public void release() {
        native_release();
        // then clear instance
        sInstance = null;
    }

    // public APIs
    public int getTVSystem() {

        OutputFormat rst = getCurrentOutputFormat(); //getFactoryOutputFormat();

        int videoSystem = -1;
        int videoStandard = -1;

        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);
            if( info.mVIC == rst.mVIC
                && info.mFreqShift == rst.mFreqShift)
            {
                videoSystem = info.mVideoSystem;
                videoStandard = info.mVideoStandard;
                break;
            }
        }

        Log.d(TAG,"getTVSystem videoSystem:"
                + videoSystem
                +" videoStandard:"
                + videoStandard);

        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);
            boolean progressive = (videoStandard == RtkTVSystem.VIDEO_PROGRESSIVE)?true:false;

            if(info.mVideoSystem == videoSystem
                && info.mProgress == progressive
                && info.mFreqShift == rst.mFreqShift) {

                Log.d(TAG, "getTVSystem return :"+info.mSettingValue);
                return info.mSettingValue;
            }
        }

        return -1;
    }

    public boolean checkHDRModeSupported(int mode) {
        int rst = native_checkHDRModeSupported(mode);
        return (rst>0)?true:false;
    }

    // usused.
    private boolean deepColorFirst() {
        int rst = SystemProperties.getInt(PROPERTY_DEEP_COLOR_FIRST,1);

        if(rst == 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isIgnoreEDID() {
        int rst = SystemProperties.getInt(PROPERTY_IGNORE_EDID,0);
        if(rst > 0) {
            return true;
        } else {
           return false;
        }
    }

    private String getHDRString(int hdr) {
        switch(hdr) {
        //case RtkTVSystem.VO_HDR_CTRL_NONE:
        case RtkTVSystem.VO_HDR_CTRL_AUTO:
            return "AUTO";
        case RtkTVSystem.VO_HDR_CTRL_DV_ON:
            return "DV";
        case RtkTVSystem.VO_HDR_CTRL_SDR:
            return "SDR";
        case RtkTVSystem.VO_HDR_CTRL_HDR_GAMMA:
            return "GAMMA";
        case RtkTVSystem.VO_HDR_CTRL_PQHDR:
            return "PQHDR";
        case RtkTVSystem.VO_HDR_CTRL_FUTURE:
            return "FUTURE";
        case RtkTVSystem.VO_HDR_CTRL_INPUT:
            return "INPUT";
        case RtkTVSystem.VO_HDR_CTRL_DV_LOW_LATENCY_12b_YUV422:
            return "VO_HDR_CTRL_DV_LOW_LATENCY_12b_YUV422";
        case RtkTVSystem.VO_HDR_CTRL_DV_LOW_LATENCY_10b_YUV444:
            return "VO_HDR_CTRL_DV_LOW_LATENCY_10b_YUV444";
        case RtkTVSystem.VO_HDR_CTRL_DV_LOW_LATENCY_10b_RGB444:
            return "VO_HDR_CTRL_DV_LOW_LATENCY_10b_RGB444";
        case RtkTVSystem.VO_HDR_CTRL_DV_LOW_LATENCY_12b_YUV444:
            return "VO_HDR_CTRL_DV_LOW_LATENCY_12b_YUV444";
        case RtkTVSystem.VO_HDR_CTRL_DV_LOW_LATENCY_12b_RGB444:
            return "VO_HDR_CTRL_DV_LOW_LATENCY_12b_RGB444";
        case RtkTVSystem.VO_HDR_CTRL_DV_ON_INPUT:
            return "VO_HDR_CTRL_DV_ON_INPUT";
        case RtkTVSystem.VO_HDR_CTRL_DV_ON_LOW_LATENCY_12b422_INPUT:
            return "VO_HDR_CTRL_DV_ON_LOW_LATENCY_12b422_INPUT";
        default:
            return "";
        }
    }

    private String getColorString(int color) {
        switch(color) {
        case ColorRGB444:
            return "RGB444";
        case ColorYUV444:
            return "YUV444";
        case ColorYUV422:
            return "YUV422";
        case ColorYUV420:
            return "YUV420";
        default:
            return "";
        }
    }

    private int findColorModeEnum(int color, int depth) {
        for(int i=0;i<sColorModes.size();i++) {
            ColorEnumInfo info = sColorModes.get(i);
            ColorSet set = info.mColorSet;
            if(set.mColorMode == color && set.mColorDepth == depth) {
                return info.mSettingValue;
            }
        }
        // let me crash, should not reach here
        return -1;
    }

    /*
    private int getCurrentColorModeEnum(HDMIDisplayFormat dFmt) {
        if(dFmt.mColorModeEnum == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT
            || dFmt.mColorModeEnum == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {
            return dFmt.mColorModeEnum;
        }else{
            int color       = dFmt.mColor;
            int colorDepth  = dFmt.mColorDepth;
            return findColorModeEnum(color, colorDepth);
        }
    }
    */

    private int asyncChangeTVSystemIgnoreEDID(int tvSystem){

        if(tvSystem == RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT) {
            Log.e(TAG, "asyncChangeTVSystemIgnoreEDID does not support auto mode");
            return 1;
        }

        TVSystemInfo info = findTVSystemInfo(tvSystem);
        if(info == null) {
            Log.e(TAG, "asyncChangeTVSystemIgnoreEDID no tv system info for : "+tvSystem);
            return 1;
        }

        int color_mode = native_readColorModeEnum();
        ColorSet color = null;

        if(color_mode == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT) {
            color = mAutoDeepColorSetIgnoreEDID;
        }else if(color_mode == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {
            color = mAutoColorSetIgnoreEDID;
        }else {
            color = getColorEnumInfo(color_mode).mColorSet;
        }

        if(color == null) {
            Log.e(TAG, "asyncChangeTVSystemIgnoreEDID color set is null");
            return 1;
        }

        int vic = info.mVIC;
        int freqShift = info.mFreqShift;
        int hdr = RtkTVSystem.VO_HDR_CTRL_SDR;
        // do not define color mode, let Service choice it
        int colorMode = color.mColorMode;
        int colorDepth = color.mColorDepth;
        int _3DFormat = 0;

        int flags = /*EXTRA_SAVE_TO_FACTORY*/
            EXTRA_FORCE_OUTPUT_FORMAT
            |EXTRA_IGNORE_CURRENT;

        Log.d(TAG, "asyncChangeTVSystemIgnoreEDID call native_setOutputFormat vic:"+vic);

        int rst = native_setOutputFormat(
            vic,
            freqShift,
            colorMode,
            colorDepth,
            _3DFormat,
            hdr,
            flags);

        return rst;

    }

    // Note: tvSystem is the value defined in Setting UI
    private int asyncChangeTVSystem(int tvSystem, boolean saveFactory) {
        synchronized(this) {

            if(saveFactory) {
            if( tvSystem != RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT) {
                native_saveEdidAutoMode(RtkTVSystem.TV_SYSTEM_AUTO_MODE_OFF);
            }else{
                native_saveEdidAutoMode(RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID);
                }
            }

            // Convert setting tv system enum to VIC and other info
            TVSystemInfo info = findTVSystemInfo(tvSystem);

            if(info == null) {
                // TODO: show a toast message
                Log.e(TAG, "Can not find TVSystemInfo of "+tvSystem);
                return -1;
            }

                HDMIFormatSupport formatSupport = getFormatSupport(info.mVIC);
                if(formatSupport == null) {
                    // TODO: show a toast message
                    Log.e(TAG, "Can not find HDMIFormatSupport of VIC:"+info.mVIC);
                    return -1;
                }

            int vic = info.mVIC;
            int freqShift = info.mFreqShift;
            int hdr = RtkTVSystem.VO_HDR_CTRL_CURRENT;
            // do not define color mode, let Service choice it
            int color = ColorNone;
            int colorDepth = DepthNone;
            int _3DFormat = 0;
            int flags = /* EXTRA_SAVE_TO_FACTORY */
                 EXTRA_IGNORE_CURRENT
                |EXTRA_TRY_TO_USE_CURRENT_COLOR;

            if(saveFactory) {
                flags = flags | EXTRA_SAVE_TO_FACTORY;
            }

            Log.d(TAG, "Call native_setOutputFormat vic:"+vic);

            int rst = native_setOutputFormat(
                vic,
                freqShift,
                color,
                colorDepth,
                _3DFormat,
                hdr,
                flags);

            return rst;
        }
    }

    public void setListener(EventListener listener) {
        mListener = listener;
    }

    public int runtimeSetTVSystem(int value) {
        Log.d(TAG, "runtimeSetTVSystem "+value);
        final int tvSystem = value;
        // try calling in the same thread
        //asyncChangeTVSystem(tvSystem);

        Thread thread = new Thread(){
            @Override
            public void run()  {
                asyncChangeTVSystem(tvSystem,false);
            }
        };
        thread.start();
        return 0;

    }

    public int setTVSystem(int value) {

        Log.d(TAG, "setTVSystem "+value);
        final int tvSystem = value;

        // try calling in the same thread
        //asyncChangeTVSystem(tvSystem);

        Thread thread = new Thread(){
            @Override
            public void run()  {
                if(isIgnoreEDID()) {
                    asyncChangeTVSystemIgnoreEDID(tvSystem);
                } else {
                    asyncChangeTVSystem(tvSystem,true);
                }
            }
        };
        thread.start();
        return 0;
    }

    public boolean checkIfHDMIPlugged() {
        boolean rst = native_checkIfHDMIPlugged();
        Log.d(TAG, "checkIfHDMIPlugged "+rst);
        return rst;
    }

    public boolean getHDMIEDIDReady() {
        boolean rst = native_getHDMIEDIDReady();
        Log.d(TAG, "getHDMIEDIDReady "+rst);
        return rst;
    }

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

            if(isUnderDoviMode()) {
                Log.d(TAG, "Under DOVI mode, hide 1080i, NTSC, PAL, 4096..");
                supportVideoFormat[RtkTVSystem.TV_SYS_1080I_50HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_1080I_60HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_1080I_59HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_NTSC] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_PAL] = 0;
                // 4k
                supportVideoFormat[RtkTVSystem.TV_SYS_4096_2160P_60HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_4096_2160P_50HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_4096_2160P_30HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_4096_2160P_25HZ] = 0;
                supportVideoFormat[RtkTVSystem.TV_SYS_4096_2160P_24HZ] = 0;
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

    public int isFormat3D() {
        OutputFormat currentFormat = getCurrentOutputFormat();
        if(currentFormat.m3DFormat != Mode_3D_Off) {
            return RtkTVSystem.MODE_3D_ON;
        } else {
            return RtkTVSystem.MODE_3D_OFF;
        }
    }

    public int checkTVSystem(int inputTvSystem, int colorspace_deepcolor) {

        Log.d(TAG, "checkTVSystem tvSystem:"
                + inputTvSystem
                + " color:"
                + colorspace_deepcolor);

        if(isIgnoreEDID()) {
            Log.d(TAG, "always report true if isIgnoreEDID");
            return 0;
        }


        if(colorspace_deepcolor == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT) {
            Log.d(TAG, "checkTVSystem TV_SYS_COLORSPACE_AUTO_DETECT always supports");
            return 0;
        }

        if(colorspace_deepcolor == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {
            Log.d(TAG, "checkTVSystem TV_SYS_DEEPCOLOR_AUTO_DETECT always supports");
            return 0;
        }

        // block DOVI mode
        if(isUnderDoviMode()) {
            Log.d(TAG, "checkTVSystem found current mode is DOVI, reject");
            return -1;
        }

        // block VO_HDR_CTRL_DV_ON_INPUT
        int hdr = getCurrentHDRMode();
        if(hdr == RtkTVSystem.VO_HDR_CTRL_DV_ON_INPUT) {
            Log.d(TAG, "checkTVSystem hdr : VO_HDR_CTRL_DV_ON_INPUT, reject");
            return -1;
        }

        ColorEnumInfo colorInfo = getColorEnumInfo(colorspace_deepcolor);

        if(colorInfo == null) {
            Log.e(TAG, "[checkTVSystem] can not get color mode enum");
            return -1;
        }

        int colorMode = colorInfo.mColorSet.mColorMode;
        int colorDepth = colorInfo.mColorSet.mColorDepth;

        TVSystemInfo info = findTVSystemInfo(inputTvSystem);
        if(info == null) {
            Log.e(TAG, "[checkTVSystem] Can not find TVSystemInfo of "+inputTvSystem);
            return -1;
        }

        HDMIFormatSupport formatSupport = getFormatSupport(info.mVIC);
        if(formatSupport == null) {
            Log.e(TAG, "[checkTVSystem] Can not find HDMIFormatSupport of VIC:"
                +info.mVIC);
            return -1;
        }

        boolean rst = formatSupport.isColorModeDepthSupported(colorMode, colorDepth);

        /*
        if(rst) {
            // dolby vision final confirm
            int currentHDRMode = getCurrentHDRMode();
            if(currentHDRMode == RtkTVSystem.VO_HDR_CTRL_DV_ON) {
                if(colorMode == ColorRGB444 && colorDepth == Depth8Bit) {
                    Log.d(TAG, "checkTVSystem pass DV HDR mode");
                }else{
                    Log.e(TAG, "checkTVSystem can not pass DV HDR mode "
                        + getColorString(colorMode)
                        +" Depth:"
                        + colorDepth);
                    rst = false;
                }
            }
        }
        */

        return rst?0:-1;
    }

    private void setOutputFormat(OutputFormat fmt, int flags) {
        native_setOutputFormat(
            fmt.mVIC,
            fmt.mFreqShift,
            fmt.mColor,
            fmt.mColorDepth,
            fmt.m3DFormat,
            fmt.mHDR,
            flags);
    }

    // for auto 24fps playback
    public HDMIDisplayFormat storeCurrentDisplayFormat() {
        return getCurrentDisplayFormat();
    }

    // for auto 24fps playback
    public void restoreDisplayFormat(HDMIDisplayFormat displayFormat) {
        OutputFormat fmt = displayFormat.mFormat;

        int flags = /*EXTRA_SAVE_TO_FACTORY*/
            EXTRA_FORCE_OUTPUT_FORMAT
            |EXTRA_IGNORE_CURRENT;

        setOutputFormat(fmt,flags);
    }

    public HDMIDisplayFormat getCurrentDisplayFormat() {
        OutputFormat currentFormat = getCurrentOutputFormat();
        //OutputFormat factoryFormat = getFactoryOutputFormat();
        return new HDMIDisplayFormat(currentFormat/*,factoryFormat*/);
    }

    private TVSystemInfo findTVSystemInfoViaOutputFormat(OutputFormat fmt) {
        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);

            if(info.mVIC == fmt.mVIC && info.mFreqShift == fmt.mFreqShift) {
                return info;
            }
        }
        // let me crash, should not reach here
        return null;
    }

    private int setHDMIVideoColorModeIgnoreEDID(int mode) {
        ColorSet color = null;

        if(mode == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT) {
            color = mAutoDeepColorSetIgnoreEDID;
        }else if(mode == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {
            color = mAutoColorSetIgnoreEDID;
        }else {
            color = getColorEnumInfo(mode).mColorSet;
        }

        Log.d(TAG, "setHDMIVideoColorModeIgnoreEDID mode:"+mode);
        Log.d(TAG, "setHDMIVideoColorModeIgnoreEDID "+color.mColorMode+","+color.mColorDepth);

        OutputFormat fmt = getCurrentOutputFormat(); //getFactoryOutputFormat();
        fmt.mColor = color.mColorMode;
        fmt.mColorDepth = color.mColorDepth;

        int flags = /*EXTRA_SAVE_TO_FACTORY*/
            EXTRA_FORCE_OUTPUT_FORMAT
            |EXTRA_IGNORE_CURRENT;

        setOutputFormat(fmt,flags);

        // then, save the setting
        native_saveColorModeEnum(mode);

        return 1;
    }

    public int setHDMIVideoColorMode(int mode) {

        if(isIgnoreEDID()) {
            return setHDMIVideoColorModeIgnoreEDID(mode);
        }

        // 1. get current tv system setting
        // 2. overwrite color mode (convert mode to ColorMode and ColorDepth)
        // 3. call native_setOutputFormat (no native function is needed)
        // 4. maybe one should save the setting value
        Log.d(TAG, "setHDMIVideoColorMode :"+mode);

        OutputFormat fmt = getCurrentOutputFormat(); //getFactoryOutputFormat();
        HDMIFormatSupport formatSupport = getFormatSupport(fmt.mVIC);

        if(formatSupport == null) {
            Log.e(TAG, "setHDMIVideoColorMode can not find HDMIFormatSupport of VIC:"+fmt.mVIC);
            return -1;
        }

        ColorSet colorSet = new ColorSet();

        if(mode == RtkTVSystem.TV_SYS_COLORSPACE_AUTO_DETECT) {

            native_getColorSet(
                fmt.mVIC,
                RtkTVSystem.VO_HDR_CTRL_CURRENT,
                COLOR_POLICY_AUTO,
                colorSet);

            //colorSet = formatSupport.getAutoColorSet();
        }else if(mode == RtkTVSystem.TV_SYS_DEEPCOLOR_AUTO_DETECT) {

            int ret = native_getColorSet(
                        fmt.mVIC,
                        RtkTVSystem.VO_HDR_CTRL_CURRENT,
                        COLOR_POLICY_DEEP_AUTO,
                        colorSet);

            if(ret == -1 || !colorSet.isValidate()) {
                native_getColorSet(
                    fmt.mVIC,
                    RtkTVSystem.VO_HDR_CTRL_CURRENT,
                    COLOR_POLICY_AUTO,
                    colorSet);
            }

            //colorSet = formatSupport.getAutoColorSetDeepColor();
        }else{
            colorSet = getColorEnumInfo(mode).mColorSet;
        }

        if(colorSet == null || !colorSet.isValidate()) {
            Log.e(TAG, "setHDMIVideoColorMode can not find colorSet of VIC:"+fmt.mVIC);
            return -1;
        }

        // then, save the setting
        native_saveColorModeEnum(mode);

        fmt.mColor = colorSet.mColorMode;
        fmt.mColorDepth = colorSet.mColorDepth;

        int flags = EXTRA_SAVE_TO_FACTORY
            |EXTRA_FORCE_OUTPUT_FORMAT
            |EXTRA_IGNORE_CURRENT;

        setOutputFormat(fmt,flags);

        // TODO backward compatiable, write to property

        // color mode: persist.rtk.hdmitx.colorspace
        // 0: AUTO
        // 1: RGB 444
        // 2: YCrCb 422
        // 3: YCrCb 444
        // 4: YCrCb 420

        // color depth: persist.rtk.deepcolor
        // auto
        // 10bit
        // 12bit
        // off

        return 1;
    }

    public byte[] getEDIDRawData() {
		byte[] arrResult = native_getEDIDRawData();
        /*
        Log.d(TAG, "getEDIDRawData buffer len:"+arrResult.length);
        for(int i=0;i<arrResult.length;i++) {
            Log.d(TAG, "EDID data "+Integer.toHexString(arrResult[i]));
        }
        */
        return arrResult;
    }

    public void getSinkCapability() {
        Log.d(TAG, "call getSinkCapability");
        native_getSinkCapability();
    }

    public boolean isTVSupport3D() {

        Log.d(TAG, "Call isTVSupport3D");

        for(int i=0;i<mEDIDSupportList.size();i++) {
            HDMIFormatSupport item = mEDIDSupportList.get(i);
            if(item.isSupport3D()) {
                Log.d(TAG, "isTVSupport3D success ("+i+")");
                return true;
            }
        }

        return false;
    }

    public int getTVSystemForRestored() {

        if(native_readEdidAutoMode() == RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID) {
            return RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT;
        }else{
            return getTVSystem();
        }
    }

    public int setHDMIFormat3D(int format3d, float fps) {
        return native_setHDMIFormat3D(format3d, fps);
    }

    public void setHDREotfMode(int hdr) {

        OutputFormat fmt = getCurrentOutputFormat();
        fmt.mHDR = hdr;
        fmt.mColor = ColorNone;
        fmt.mColorDepth = DepthNone;

        int flags = EXTRA_SAVE_TO_FACTORY
            |EXTRA_IGNORE_CURRENT
            |EXTRA_USE_NOW_COLOR_IF_SDR
            |EXTRA_RESET_COLOR_MODE
            |EXTRA_RESET_TV_SYSTEM;

        // when a HDR mode is set again, switch to color mode auto again
        native_saveColorModeEnum(TV_SYS_COLOR_USE_DEFAULT);

        // for a dolby vision tv, it might reset tv system, so when a HDR mode is choiced, reset TV system again
        native_saveEdidAutoMode(RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID);

        setOutputFormat(fmt,flags);
    }

    // TODO: handle settingVal is TV_SYS_NUM, it means current TV system
    private TVSystemInfo findTVSystemInfo(int settingVal) {

        if(settingVal == RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT) {
            return getAutoTVSystemInfo();
        }

        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);
            if(info.mSettingValue == settingVal) {
                return info;
            }
        }
        return null;
    }

    private TVSystemInfo getAutoTVSystemInfo() {

        TVSystemInfo info = sTVSystemInfos.get(0);
        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo tmp = sTVSystemInfos.get(i);
            if(info.ranking() < tmp.ranking()) {
                info = tmp;
            }
        }
        return info;
    }

    private static HDMIFormatSupport getFormatSupport(int vic) {
        synchronized(sLock) {
            for(int i=0;i<mEDIDSupportList.size();i++) {
                if(vic == mEDIDSupportList.get(i).mVIC) {
                    return mEDIDSupportList.get(i);
                }
            }
            return null;
        }
    }

    private int getEDIDSupportList() {
        synchronized(sLock) {
            if(mEDIDSupportList.size() > 0) {
                Log.w(TAG, "Calling getEDIDSupportList, clear existing list first");
                mEDIDSupportList.clear();
            }
            int rst = native_getEDIDSupportList();
            Log.d(TAG, "getEDIDSupportList size "+mEDIDSupportList.size());

            for(int i=0;i<mEDIDSupportList.size();i++) {
                mEDIDSupportList.get(i).dump();
            }
            return rst;
        }
    }

    private void clearEDIDSupportList() {
        synchronized(sLock) {
            mEDIDSupportList.clear();
        }
    }

    private ColorEnumInfo getColorEnumInfo(int colorspace_deepcolor) {

        for(int i=0;i<sColorModes.size();i++) {
            if(sColorModes.get(i).mSettingValue == colorspace_deepcolor) {
                return sColorModes.get(i);
            }
        }

        return null;
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


    // called from native
    private void from_native_addEDIDSupportListItem(
        int vic,
        int rgb444,
        int yuv422,
        int yuv444,
        int yuv420,
        int support_3d,
        int support_fs,
        int reserved) {

        //Log.d(TAG, "addEDIDSupportListItem vic:"+vic);

        HDMIFormatSupport item =
            new HDMIFormatSupport(
                vic,
                rgb444,
                yuv422,
                yuv444,
                yuv420,
                support_3d,
                support_fs,
                reserved);

        mEDIDSupportList.add(item);
    }

    private int getCurrentHDRMode() {
        return getCurrentOutputFormat().mHDR;
    }

    public OutputFormat getCurrentOutputFormat() {

        OutputFormat fmt = new OutputFormat();

        int rst = native_getOutputFormat(fmt,GET_MODE_CURRENT);

        return fmt;

        /*
        if(rst < 0) {
            Log.w(TAG,"Can not find current OutputFormat, return a default");

            mOutputFormatCurrent.mVIC          = RtkTVSystem.VIC_1920_1080P_60HZ;
            mOutputFormatCurrent.mFreqShift    = 0;
            mOutputFormatCurrent.mColor        = ColorYUV444;
            mOutputFormatCurrent.mColorDepth   = Depth8Bit;
            mOutputFormatCurrent.m3DFormat     = 0;
            mOutputFormatCurrent.mHDR          = 0;
        }
        return mOutputFormatCurrent;
        */
    }

    private OutputFormat getFactoryOutputFormat() {

        OutputFormat fmt = new OutputFormat();

        int rst = native_getOutputFormat(fmt,GET_MODE_FACTORY);

        return fmt;

        /*
        if(rst < 0) {
            Log.w(TAG,"Can not find OutputFormat in factory, return a default");

            mOutputFormatFactory.mVIC          = RtkTVSystem.VIC_1920_1080P_60HZ;
            mOutputFormatFactory.mFreqShift    = 0;
            mOutputFormatFactory.mColor        = ColorYUV444;
            mOutputFormatFactory.mColorDepth   = Depth8Bit;
            mOutputFormatFactory.m3DFormat     = 0;
            mOutputFormatFactory.mHDR          = 0;
        }
        return mOutputFormatFactory;
        */
    }

    private void dumpTVSystemInfoMap() {
        for(int i=0;i<sTVSystemInfos.size();i++) {
            TVSystemInfo info = sTVSystemInfos.get(i);
            Log.d(TAG, "SettingVal: "+info.mSettingValue+
                        " VIC:"+info.mVIC+
                        " freqShift:"+info.mFreqShift);
        }
    }

    private void initHDMIInfo() {
        Log.i(TAG, "Call initHDMIInfo");
        native_init();
        if(checkIfHDMIPlugged()) {
            getEDIDSupportList();
        }
    }

    // ask HIDL-server
    private boolean isUnderDoviMode() {

        int currentHDRMode = getCurrentHDRMode();
        int rst = native_isUnderDoviMode(currentHDRMode);

        return (rst>0)?true:false;
    }

    public void setHDMIEnable(boolean enable) {
        int intVal = (enable)?1:0;
        native_setHDMIEnable(intVal);
    }

    public void setHDCPVersion(int version) {
        native_setHDCPVersion(version);
    }

    // called from jni level, use handler to complete it
    private void onHandleHDMIEvent(int event) {
        Log.d(TAG, "HDMI-HIDL onHandleHDMIEvent event:"+event);
        switch(event) {
        case EVENT_PLUG_IN:
            mHandler.post(new Runnable() {
                public void run() {
                    getEDIDSupportList();

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

    /*
    private void setTVSystemAutoModeFromSettings(int autoMode) {
        setItemValueToSettings(Settings.System.REALTEK_SETUP_TV_SYSTEM_AUTO_MODE, autoMode);
    }

    private int getTVSystemAutoModeFromSettings() {
        return getItemValueFromSettings(Settings.System.REALTEK_SETUP_TV_SYSTEM_AUTO_MODE);
    }

    private void SetTVSystemAndStandardToSetup(int tvSystem, int tvStandard) {
        setItemValueToSettings(Settings.System.REALTEK_SETUP_TV_SYSTEM, tvSystem);
        setItemValueToSettings(Settings.System.REALTEK_SETUP_TV_STANDARD, tvStandard);
    }
    */

    private void setItemValueToSettings(String itemName, int itemValue) {
        Log.w(TAG, "setItemValue to Settings itemName: " + itemName + " itemValue: " + itemValue);
        Settings.System.putInt(mContext.getContentResolver(), itemName, itemValue);
    }

    private int getItemValueFromSettings(String itemName) {
        int itemValue = 0;

        try {
            itemValue = Settings.System.getInt(mContext.getContentResolver(), itemName);
        } catch (SettingNotFoundException snfe) {
            // error handling
            itemValue = -1;
        }
        Log.w(TAG, "getItemValueFromSettings itemName: " + itemName + " itemValue: " + itemValue);
        return itemValue;
    }

    private native boolean native_checkIfHDMIPlugged();
    private native boolean native_getHDMIEDIDReady();
    private native byte[] native_getEDIDRawData();
    //private native boolean native_isTVSupport3D();
    private native int native_setHDMIFormat3D(int format3d, float fps);
    private native int native_getEDIDSupportList();
    private native void native_release();
    private native void native_init();
    private native void native_setHDREotfMode(int mode);
    private native int native_getColorSet(
                            int vic,
                            int hdr,
                            int policy,
                            ColorSet rst);

    private native int native_getOutputFormat(OutputFormat fmt,int mode);

    private native int native_setOutputFormat(
                            int vic,
                            int freq_shift,
                            int color,
                            int color_depth,
                            int _3d_format,
                            int hdr,
                            int extra_flags);

    // save / read APIs
    private native void native_saveOutputFormat(
                            int vic,
                            int freq_shift,
                            int color,
                            int color_depth,
                            int _3d_format,
                            int hdr);


    private native void native_saveEdidAutoMode(int value);
    private native int native_readEdidAutoMode();

    private native void native_saveColorModeEnum(int value);
    private native int native_readColorModeEnum();

    private native int native_readHDRAutoProperty();

    private native int native_isUnderDoviMode(int mode);

    private native int native_checkHDRModeSupported(int mode);

    private native void native_setHDMIEnable(int enable);

    private native void native_setHDCPVersion(int version);

    private native void native_getSinkCapability();

    // un-need
    // private native int native_setHDMIVideoColorMode(int mode);
    // private native int native_getTVSystemForRestored();
    // private native boolean native_isTVSupport3D();
    // private native int native_readOutputFormat(OutputFormat fmt);


    /*
    private void from_native_setCurrentOutputFormat(
        int vic,
        int freqShift,
        int color,
        int colorDepth,
        int _3DFormat,
        int hdr) {

        Log.d(TAG, "from_native_setCurrentOutputFormat vic:"+vic);

        mOutputFormatCurrent.mVIC          = vic;
        mOutputFormatCurrent.mFreqShift    = freqShift;
        mOutputFormatCurrent.mColor        = color;
        mOutputFormatCurrent.mColorDepth   = colorDepth;
        mOutputFormatCurrent.m3DFormat     = _3DFormat;
        mOutputFormatCurrent.mHDR          = hdr;
    }

    private void from_native_setFactoryOutputFormat(
        int vic,
        int freqShift,
        int color,
        int colorDepth,
        int _3DFormat,
        int hdr) {

        Log.d(TAG, "from_native_setFactoryOutputFormat vic:"+vic);

        mOutputFormatFactory.mVIC          = vic;
        mOutputFormatFactory.mFreqShift    = freqShift;
        mOutputFormatFactory.mColor        = color;
        mOutputFormatFactory.mColorDepth   = colorDepth;
        mOutputFormatFactory.m3DFormat     = _3DFormat;
        mOutputFormatFactory.mHDR          = hdr;
    }
    */
}
