package com.rtk.media.ext;

import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

public final class RtkMediaPlayerFactory {

    private final static String TAG = "RtkMediaPlayerFactory";

    private RtkMediaPlayerFactory() {
    }

    public static MediaPlayer create() {
        return create(RtkMediaPlayer.FORCE_RT_MEDIAPLAYER);
    }

    public static MediaPlayer create(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switch (type) {
                case RtkMediaPlayer.FORCE_RT_MEDIAPLAYER:
                    Log.d(TAG, "\033[1;35mcreate: new RtkMediaPlayer() \033[m\n");
                    return new RtkMediaPlayer();
                case RtkMediaPlayer.FORCE_BY_SCORE:
                    Log.d(TAG, "\033[1;35mcreate: new RtkMediaPlayer() FORCE_BY_SCORE \033[m\n");
                    return new RtkMediaPlayer(RtkMediaPlayer.FORCE_BY_SCORE);
                default:
                    Log.d(TAG, "\033[1;35mcreate: new MediaPlayer() \033[m\n");
                    return null;
            }
        } else {
            Log.d(TAG, "\033[1;35mcreate: new MediaPlayer() \033[m\n");
            return null;
        }
    }

}
