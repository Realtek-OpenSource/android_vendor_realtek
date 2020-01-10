package com.rtk.media.ext;

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaPlayer.TrackInfo;
//import android.media.MediaTimestamp;
import android.os.Handler;
//import android.media.SRTRenderer;
import android.media.MediaPlayer;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by SkyJuan on 2018/1/24.
 */

class RtkReflector {
    private static ClassLoader loader = ClassLoader.getSystemClassLoader();

    static TrackInfo newMediaPlayerTrackInfo(int trackType, MediaFormat mediaFormat) {
        try {
            Constructor<TrackInfo> constructor =
                    TrackInfo.class.getDeclaredConstructor(int.class, MediaFormat.class);
            constructor.setAccessible(true);
            return constructor.newInstance(trackType, mediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static MediaPlayer.TrackInfo[] newMediaPlayerTrackInfoArray(RtkMediaPlayer.TrackInfo[] trackInfo) {
        MediaPlayer.TrackInfo[] outputTrackInfo = new MediaPlayer.TrackInfo[trackInfo.length];
        for (int i = 0; i < trackInfo.length; i++) {
            outputTrackInfo[i] = RtkReflector.newMediaPlayerTrackInfo(
                    trackInfo[i].getTrackType(), trackInfo[i].getFormat());
        }
        return outputTrackInfo;
    }

/*
    static MediaTimestamp newMediaTimestamp(long mediaUs, long systemNs, float rate) {
        try {
            Constructor<MediaTimestamp> constructor = MediaTimestamp.class.getDeclaredConstructor(
                    long.class, long.class, float.class);
            constructor.setAccessible(true);
            return constructor.newInstance(mediaUs, systemNs, rate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    static Context getAppContext() {
        try {
            Class cls = loader.loadClass("android.app.ActivityThread");
            Method m = cls.getDeclaredMethod("currentApplication");
            Object o = m.invoke(null);
            if (o instanceof Context) {
                return (Context) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void invokeSubtitleTrackOnData(Object oSubtitleTrack, Object oSubtitleData) {
        try {
            Method m = oSubtitleTrack.getClass().getDeclaredMethod("onData");
            m.setAccessible(true);
            m.invoke(oSubtitleTrack, oSubtitleData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static SRTRenderer newSRTRenderer(Context context, Handler handler) {
        try {
            Class cls = loader.loadClass("android.media.SRTRenderer");
            Constructor c = cls.getDeclaredConstructor(Context.class, Handler.class);
            c.setAccessible(true);
            return (SRTRenderer) c.newInstance(context, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
*/
}
