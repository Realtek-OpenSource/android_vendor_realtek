package com.realtek.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.provider.Settings;
import com.rtk.mediabrowser.R;

/**
 * Created by archerho on 2016/2/18.
 */
public class UseRtMediaPlayer {
    private static final String PREF_NAME = "UseRtMediaPlayer";
    private static final String FORCE_RT_MEDIA_PLAYER = "FORCE_RT_MEDIA_PLAYER";
    private boolean mUseRtPlayer;
    private SharedPreferences mPref;
    private static UseRtMediaPlayer mInstance;
    private Context mCtx;

    public static UseRtMediaPlayer getInstance(Context ctx){
        if(mInstance==null)
            mInstance = new UseRtMediaPlayer(ctx);
        return mInstance;
    }

    private UseRtMediaPlayer(Context ctx){
        if(mPref==null)
            mPref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mCtx = ctx;
    }

    public boolean setUseRtPlayer(boolean b){
        if(mPref==null) return false;
        SharedPreferences.Editor edit = mPref.edit();
        edit.putBoolean(FORCE_RT_MEDIA_PLAYER, b);
        edit.apply();
        mUseRtPlayer = b;
        if(b) {
            SystemProperties.set("ctl.start", "rvsd-1-0");
            SystemProperties.set("ctl.start", "DvdPlayer");
        } else {
            SystemProperties.set("ctl.stop", "DvdPlayer");
            SystemProperties.set("ctl.stop", "rvsd-1-0");
        }
        return b;
    }

    public boolean switchUseRtPlayer(){
        return setUseRtPlayer(!mUseRtPlayer);
    }

    public boolean getUseRtPlayer(){
        if(mPref==null) return false;
        mUseRtPlayer = mPref.getBoolean(FORCE_RT_MEDIA_PLAYER, true);
        return mUseRtPlayer;
    }
}
