package com.rtk.media.ext;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.DeniedByServerException;
import android.media.MediaDataSource;
import android.media.MediaDrm;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.Metadata;
import android.media.SubtitleController;
import android.media.MediaTimeProvider;
import android.media.PlaybackParams;
import android.media.ResourceBusyException;
import android.media.SyncParams;
import android.media.UnsupportedSchemeException;
import android.media.VolumeShaper;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.rtk.media.ext.RtkMediaPlayer.NaviInfo;
import com.rtk.media.ext.RtkMediaPlayer.BDInfo;
import com.rtk.media.ext.RtkMediaPlayer.NaviType;
import com.rtk.media.ext.RtkMediaPlayer.OnSeekResultListener;
import com.rtk.media.ext.RtkMediaPlayer.OnSpeedChangedListener;

public class RtkMediaPlayerWrap extends MediaPlayer {

    private final static String TAG = "RtkMediaPlayerWrap";

    private MediaPlayer mMediaPlayer;
    private int mUseRTMediaPlayer =  RtkMediaPlayer.FORCE_NONE;

    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;

    private OnSpeedChangedListener mOnSpeedChangedListener;
    private OnSeekResultListener mOnSeekResultListener;

    public RtkMediaPlayerWrap() {
    }

    public RtkMediaPlayerWrap(int forcePlayerType) {
        mUseRTMediaPlayer = forcePlayerType;
        mMediaPlayer = RtkMediaPlayerFactory.create(forcePlayerType);
    }

    @Override
    public Parcel newRequest() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).newRequest();
        }
        return super.newRequest();
    }

    @Override
    public void invoke(Parcel request, Parcel reply) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).invoke(request, reply);
            return;
        }
        super.invoke(request, reply);
    }

    @Override
    public Metadata getMetadata(final boolean update_only, final boolean apply_filter) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getMetadata(update_only, apply_filter);
        }
        return super.getMetadata(update_only, apply_filter);
    }

    @Override
    public void setDataSource(String path, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(path, headers);
            return;
        }
        super.setDataSource(path, headers);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {

        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setVolume(leftVolume, rightVolume);
            return;
        }
        super.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setVolume(float volume) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setVolume(volume);
            return;
        }
        super.setVolume(volume);
    }

    @Override
    public void setSubtitleAnchor(
            SubtitleController controller,
            SubtitleController.Anchor anchor) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setSubtitleAnchor(controller, anchor);
            return;
        }
        super.setSubtitleAnchor(controller, anchor);
    }

    @Override
    public void addSubtitleSource(InputStream is, MediaFormat format)
            throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).addSubtitleSource(is, format);
            return;
        }
        super.addSubtitleSource(is, format);
    }

    @Override
    public MediaTimeProvider getMediaTimeProvider() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getMediaTimeProvider();
        }
        return super.getMediaTimeProvider();
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDisplay(sh);
            return;
        }
        super.setDisplay(sh);
    }

    @Override
    public void setSurface(Surface surface) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setSurface(surface);
            return;
        }
        super.setSurface(surface);
    }

    @Override
    public void setVideoScalingMode(int mode) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setVideoScalingMode(mode);
            return;
        }
        super.setVideoScalingMode(mode);
    }

    @Override
    public void setDataSource(Context context, Uri uri)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(context, uri);
            return;
        }
        super.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri,
            Map<String, String> headers, List<HttpCookie> cookies)
            throws IOException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(context, uri, headers, cookies);
            return;
        }
        super.setDataSource(context, uri, headers, cookies);
    }

    @Override
    public void setDataSource(Context context, Uri uri,
            Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(context, uri, headers);
            return;
        }
        super.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

        if(mUseRTMediaPlayer==RtkMediaPlayer.FORCE_BY_SCORE) {
            if(keepOriginalFlow(path)==true && analysisHeader(path)==true) {
                if(mMediaPlayer instanceof RtkMediaPlayer) {
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }
            else {
                if(mMediaPlayer == null) {
                    mMediaPlayer = RtkMediaPlayerFactory.create(RtkMediaPlayer.FORCE_BY_SCORE);
                }
            }
            setListener();
        }

        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(path);
            return;
        }
        super.setDataSource(path);
    }

    @Override
    public void setDataSource(AssetFileDescriptor afd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(afd);
            return;
        }
        super.setDataSource(afd);
    }

    @Override
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(fd);
            return;
        }
        super.setDataSource(fd);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(fd, offset, length);
            return;
        }
        super.setDataSource(fd, offset, length);
    }

    @Override
    public void setDataSource(MediaDataSource dataSource)
            throws IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDataSource(dataSource);
            return;
        }
        super.setDataSource(dataSource);
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).prepare();
            return;
        }
        super.prepare();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).prepareAsync();
            return;
        }
        super.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).start();
            return;
        }
        super.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).stop();
            return;
        }
        super.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).pause();
            return;
        }
        super.pause();
    }

    @Override
    public VolumeShaper createVolumeShaper(VolumeShaper.Configuration configuration) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).createVolumeShaper(configuration);
        }
        return super.createVolumeShaper(configuration);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setWakeMode(context, mode);
            return;
        }
        super.setWakeMode(context, mode);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setScreenOnWhilePlaying(screenOn);
            return;
        }
        super.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public int getVideoWidth() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getVideoWidth();
        }
        return super.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getVideoHeight();
        }
        return super.getVideoHeight();
    }

    @Override
    public PersistableBundle getMetrics() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getMetrics();
        }
        return super.getMetrics();
    }

    @Override
    public boolean isPlaying() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).isPlaying();
        }
        return super.isPlaying();
    }

    @Override
    public void setPlaybackParams(PlaybackParams params) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setPlaybackParams(params);
            return;
        }
        super.setPlaybackParams(params);
    }

    @Override
    public PlaybackParams getPlaybackParams() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getPlaybackParams();
        }
        return super.getPlaybackParams();
    }

    @Override
    public void setSyncParams(SyncParams params) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setSyncParams(params);
            return;
        }
        super.setSyncParams(params);
    }

    @Override
    public SyncParams getSyncParams() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getSyncParams();
        }
        return super.getSyncParams();
    }

    @Override
    public void seekTo(long msec, int mode) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).seekTo(msec, mode);
            return;
        }
        super.seekTo(msec, mode);
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).seekTo(msec);
            return;
        }
        super.seekTo(msec);
    }

    @Override
    public MediaTimestamp getTimestamp() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getTimestamp();
        }
        return super.getTimestamp();
    }

    @Override
    public int getCurrentPosition() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getCurrentPosition();
        }
        return super.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getDuration();
        }
        return super.getDuration();
    }

    @Override
    public void setNextMediaPlayer(MediaPlayer next) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setNextMediaPlayer(next);
            return;
        }
        super.setNextMediaPlayer(next);
    }

    @Override
    public void release() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).release();
            mMediaPlayer = null;
            return;
        }
        super.release();
    }

    @Override
    public void reset() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).reset();
            return;
        }
        super.reset();
    }

    @Override
    public void setAudioStreamType(int streamType) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAudioStreamType(streamType);
            return;
        }
        super.setAudioStreamType(streamType);
    }

    @Override
    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAudioAttributes(attributes);
            return;
        }
        super.setAudioAttributes(attributes);
    }

    @Override
    public void setLooping(boolean looping) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setLooping(looping);
            return;
        }
        super.setLooping(looping);
    }

    @Override
    public boolean isLooping() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).isLooping();
        }
        return super.isLooping();
    }

    @Override
    public void setAudioSessionId(int sessionId)
            throws IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAudioSessionId(sessionId);
            return;
        }
        super.setAudioSessionId(sessionId);
    }

    @Override
    public int getAudioSessionId() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getAudioSessionId();
        }
        return super.getAudioSessionId();
    }

    @Override
    public void attachAuxEffect(int effectId) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).attachAuxEffect(effectId);
            return;
        }
        super.attachAuxEffect(effectId);
    }

    @Override
    public void setAuxEffectSendLevel(float level) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAuxEffectSendLevel(level);
            return;
        }
        super.setAuxEffectSendLevel(level);
    }

    public RtkMediaPlayer.TrackInfo[] getRtkTrackInfo() throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getRtkTrackInfo();
        }
        RtkMediaPlayer.TrackInfo trackInfo[] = getInbandTrackInfo();
        // add out-of-band tracks
        return trackInfo;
    }

    private RtkMediaPlayer.TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken("android.media.IMediaPlayer");
            request.writeInt(1); //INVOKE_ID_GET_TRACK_INFO
            invoke(request, reply);
            RtkMediaPlayer.TrackInfo trackInfo[] = reply.createTypedArray(RtkMediaPlayer.TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    @Override
    public void addTimedTextSource(String path, String mimeType)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).addTimedTextSource(path, mimeType);
            return;
        }
        super.addTimedTextSource(path, mimeType);
    }

    @Override
    public void addTimedTextSource(Context context, Uri uri, String mimeType)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).addTimedTextSource(context, uri, mimeType);
            return;
        }
        super.addTimedTextSource(context, uri, mimeType);
    }

    @Override
    public void addTimedTextSource(FileDescriptor fd, String mimeType)
            throws IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).addTimedTextSource(fd, mimeType);
            return;
        }
        super.addTimedTextSource(fd, mimeType);
    }

    @Override
    public void addTimedTextSource(FileDescriptor fd, long offset, long length, String mime)
            throws IllegalArgumentException, IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).addTimedTextSource(fd, offset, length, mime);
            return;
        }
        super.addTimedTextSource(fd, offset, length, mime);
    }

    @Override
    public int getSelectedTrack(int trackType) throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getSelectedTrack(trackType);
        }
        return super.getSelectedTrack(trackType);
    }

    @Override
    public void selectTrack(int index) throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).selectTrack(index);
            return;
        }
        super.selectTrack(index);
    }

    @Override
    public void deselectTrack(int index) throws IllegalStateException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).deselectTrack(index);
            return;
        }
        super.deselectTrack(index);
    }

    @Override
    protected void finalize() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).finalize();
            return;
        }
        super.finalize();
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        mOnPreparedListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnPreparedListener(listener);
            return;
        }
        super.setOnPreparedListener(listener);
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        mOnCompletionListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnCompletionListener(listener);
            return;
        }
        super.setOnCompletionListener(listener);
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnBufferingUpdateListener(listener);
            return;
        }
        super.setOnBufferingUpdateListener(listener);
    }

    @Override
    public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnSeekCompleteListener(listener);
            return;
        }
        super.setOnSeekCompleteListener(listener);
    }

    @Override
    public void setOnVideoSizeChangedListener(final OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnVideoSizeChangedListener(listener);
            return;
        }
        super.setOnVideoSizeChangedListener(listener);
    }

    @Override
    public void setOnTimedTextListener(final OnTimedTextListener listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnTimedTextListener(listener);
            return;
        }
        super.setOnTimedTextListener(listener);
    }

    @Override
    public void setOnTimedMetaDataAvailableListener(final OnTimedMetaDataAvailableListener listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnTimedMetaDataAvailableListener(listener);
            return;
        }
        super.setOnTimedMetaDataAvailableListener(listener);
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        mOnErrorListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnErrorListener(listener);
            return;
        }
        super.setOnErrorListener(listener);
    }

    @Override
    public void setOnInfoListener(final OnInfoListener listener) {
        mOnInfoListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnInfoListener(listener);
            return;
        }
        super.setOnInfoListener(listener);
    }

    @Override
    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnDrmConfigHelper(listener);
            return;
        }
        super.setOnDrmConfigHelper(listener);
    }

    @Override
    public void setOnDrmInfoListener(OnDrmInfoListener listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnDrmInfoListener(listener);
            return;
        }
        super.setOnDrmInfoListener(listener);
    }

    @Override
    public void setOnDrmInfoListener(OnDrmInfoListener listener, Handler handler) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnDrmInfoListener(listener, handler);
            return;
        }
        super.setOnDrmInfoListener(listener, handler);
    }

    @Override
    public void setOnDrmPreparedListener(OnDrmPreparedListener listener) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnDrmPreparedListener(listener);
            return;
        }
        super.setOnDrmPreparedListener(listener);
    }

    @Override
    public void setOnDrmPreparedListener(OnDrmPreparedListener listener, Handler handler) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnDrmPreparedListener(listener, handler);
            return;
        }
        super.setOnDrmPreparedListener(listener, handler);
    }

    @Override
    public DrmInfo getDrmInfo() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getDrmInfo();
        }
        return super.getDrmInfo();
    }

    @Override
    public void prepareDrm(UUID uuid)
            throws UnsupportedSchemeException, ResourceBusyException,
            ProvisioningNetworkErrorException, ProvisioningServerErrorException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).prepareDrm(uuid);
            return;
        }
        super.prepareDrm(uuid);
    }

    @Override
    public void releaseDrm() throws NoDrmSchemeException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).releaseDrm();
            return;
        }
        super.releaseDrm();
    }

    @Override
    public MediaDrm.KeyRequest getKeyRequest(byte[] keySetId, byte[] initData,
                                             String mimeType, int keyType, Map<String, String> optionalParameters)
            throws NoDrmSchemeException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        }
        return super.getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
    }

    @Override
    public byte[] provideKeyResponse(byte[] keySetId,  byte[] response)
            throws NoDrmSchemeException, DeniedByServerException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).provideKeyResponse(keySetId, response);
        }
        return super.provideKeyResponse(keySetId, response);
    }

    @Override
    public void restoreKeys(byte[] keySetId) throws NoDrmSchemeException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).restoreKeys(keySetId);
            return;
        }
        super.restoreKeys(keySetId);
    }

    @Override
    public String getDrmPropertyString(String propertyName) throws NoDrmSchemeException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getDrmPropertyString(propertyName);
        }
        return super.getDrmPropertyString(propertyName);
    }

    @Override
    public void setDrmPropertyString(String propertyName, String value)
            throws NoDrmSchemeException {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setDrmPropertyString(propertyName, value);
            return;
        }
        super.setDrmPropertyString(propertyName, value);
    }

///////////////////////////////////////////////////////////////////////////////////////////
//// Add by Rtk start ////////////////////////////////////////////////////////////////////////

    public void setOnSeekResultListener(OnSeekResultListener listener)
    {
        mOnSeekResultListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnSeekResultListener(listener);
            return;
        }
    }

    public void setOnSpeedChangedListener(OnSpeedChangedListener listener)
    {
        mOnSpeedChangedListener = listener;
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setOnSpeedChangedListener(listener);
            return;
        }
    }

    public void useRTMediaPlayer(int forceType) {
        Log.d(TAG, "useRTMediaPlayer: "+ forceType);
        mUseRTMediaPlayer = forceType;
        switch (forceType) {
            case RtkMediaPlayer.FORCE_NONE:
            case RtkMediaPlayer.FORCE_ANDROID_MEDIAPLAYER:
                if(mMediaPlayer instanceof RtkMediaPlayer) {
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
                break;
            case RtkMediaPlayer.FORCE_RT_MEDIAPLAYER:
                if(mMediaPlayer == null) {
                    mMediaPlayer = RtkMediaPlayerFactory.create(forceType);
                }
                break;
            case RtkMediaPlayer.FORCE_BY_SCORE:
                break;
            default:
                break;
        }
        setListener();
    }

    public boolean fastforward(int speed) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).fastforward(speed);
        }
        return false;
    }

    public boolean fastrewind(int speed) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).fastrewind(speed);
        }
        return false;
    }

    public TrackInfo[] getSubtitleTrackInfo(boolean bForceUpdate) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getSubtitleTrackInfo(bForceUpdate);
        }
        return new TrackInfo[0];
    }

    public int getSubtitleCurrentIndex() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getSubtitleCurrentIndex();
        }
        return 0;
    }

    public void setVideoScaledSize(int width, int height) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setVideoScaledSize(width, height);
            return;
        }
    }    

    public void setSubtitleInfo(int streamNum,int enable,int textEncoding,int testColor,int fontSize,int syncTime,int offset) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setSubtitleInfo(streamNum, enable, textEncoding, testColor, fontSize,syncTime, offset);
            return;
        }
    }

    public void setAutoScanExtSubtitle(int autoScan) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAutoScanExtSubtitle(autoScan);
            return;
        }
    }

    public void setExtSubtitlePath(String path) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setExtSubtitlePath(path);
            return;
        }
    }

    public TrackInfo[] getAudioTrackInfo(boolean bForceUpdate) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getAudioTrackInfo(bForceUpdate);
        }
        return new TrackInfo[0];
    }

    public void setAudioTrackInfo(int streamNum)  {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setAudioTrackInfo(streamNum);
            return;
        }
    }

    public int getAudioCurrentIndex() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getAudioCurrentIndex();
        }
        return 0;
    }

    public  byte[] execSetGetNavProperty(int propertyID,byte[] inputArray) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).execSetGetNavProperty(propertyID, inputArray);
        }
        return new byte[0];
    }

    public boolean deliverNaviControlCmd(int action, int target) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).deliverNaviControlCmd(action, target);
        }
        return false;
    }

    public NaviInfo getNavigationInfo() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getNavigationInfo();
        }
        return null;
    }

    public BDInfo getBDInfo() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getBDInfo();
        }
        return null;
    }

    public BDInfo getBDChapterInfo() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getBDChapterInfo();
        }
        return null;
    }

    public void setMVC(boolean bEnabled) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setMVC(bEnabled);
        }
    }

    public NaviType getNavigationType() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).getNavigationType();
        }
        return null;
    }

    public void setStreamingBufferThreshold(int highThreshold, int lowThreshold) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            ((RtkMediaPlayer)mMediaPlayer).setStreamingBufferThreshold(highThreshold, lowThreshold);
        }
    }

    public boolean setBookmark(Uri Uri) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).setBookmark(Uri);
        }
        return false;
    }

    public boolean applyBookmark(Uri Uri) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).applyBookmark(Uri);
        }
        return false;
    }

    public boolean removeBookmark(Uri Uri) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).removeBookmark(Uri);
        }
        return false;
    }

    public boolean checkBookmark(Uri Uri) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).checkBookmark(Uri);
        }
        return false;
    }

    public boolean setParameter(int key, Parcel value) {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            return ((RtkMediaPlayer)mMediaPlayer).setParameter(key, value);
        }
        return false;
    }

    private void setListener() {
        if(mMediaPlayer instanceof RtkMediaPlayer) {
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            ((RtkMediaPlayer)mMediaPlayer).setOnSpeedChangedListener(mOnSpeedChangedListener);
            ((RtkMediaPlayer)mMediaPlayer).setOnSeekResultListener(mOnSeekResultListener);
            super.setOnPreparedListener(null);
            super.setOnCompletionListener(null);
            super.setOnSeekCompleteListener(null);
            super.setOnVideoSizeChangedListener(null);
            super.setOnErrorListener(null);
            super.setOnInfoListener(null);
        }
        else {
            super.setOnPreparedListener(mOnPreparedListener);
            super.setOnCompletionListener(mOnCompletionListener);
            super.setOnSeekCompleteListener(mOnSeekCompleteListener);
            super.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            super.setOnErrorListener(mOnErrorListener);
            super.setOnInfoListener(mOnInfoListener);
        }
    }


    /**
     * Keep going official Android media flow.
     */
    boolean keepOriginalFlow(String url) {
        String FILE_EXTS[] = {
            //".3gp", 3gp should be handled by RTMediaPlayer
            ".m4a",
            ".aac",
            ".flac",
            ".mp3",
            ".mid",
            ".midi",
            ".smf",
            ".xmf",
            ".mxmf",
            ".rtttl",
            ".rtx",
            ".ota",
            ".imy",
            ".ogg",
            ".dsf",
            ".wav",
            ".wma",
            ".amr",
            ".ape"
        };

        for (int i = 0; i < FILE_EXTS.length; ++i) {
            if(url.endsWith(FILE_EXTS[i])) {
                Log.d(TAG, "keepOriginalFlow(true): "+ url);
                return true;
            }
        }

        // redirct to stagefrightPlayer in following cases
        // 1. system sound effect
        // 2. apk sound effect
        if (url.startsWith("/system/")
            || url.startsWith("/data/app/")) {
            return true;
        } else if (url.startsWith("/data/data/")) {
            String VIDEO_FILE_EXTS[] = {
                ".ts",
                ".mp4",
                ".avi",
                ".wmv",
                ".3gp",
                ".mkv",
                ".vob",
                ".mpg",
                ".mpeg"
            };

            for (int i = 0; i < VIDEO_FILE_EXTS.length; ++i) {
                if(url.endsWith(VIDEO_FILE_EXTS[i])) {
                    Log.d(TAG, "keepOriginalFlow(false): "+ url);
                    return false;
                }
            }
            Log.d(TAG, "keepOriginalFlow(true): "+ url);
            return true;
        }
        Log.d(TAG, "keepOriginalFlow(false): "+ url);
        return false;
    }

    boolean analysisHeader(String url) {

        byte[] getBytes = new byte[512];
        try {
            java.io.File file = new java.io.File(url);
            java.io.InputStream is = new java.io.FileInputStream(file);
            is.read(getBytes, 0, 512);
            is.close();
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String header = new String(getBytes);

        //if audio with following case , switch to RTMediaPlayer
        //sample rate >= 96khz(.ogg, .wav, .flac)
        //dts audio
        //aac adif
        //alac
        //adpcm
        //ac3
        if(header.substring(0, 4).equals("OggS")){
            if(header.substring(29, 35).equals("vorbis")) {
                return true;    //NuPlayer
            }
        }
        if(header.substring(0, 3).equals("DSD")){
            if(header.substring(28, 31).equals("fmt")) {
                return true;    //NuPlayer
            }
        }

        if(header.substring(0, 4).equals("RIFF") && header.substring(8, 12).equals("WAVE")){
            int compress = getBytes[20] << 8 | getBytes[21];
            int ch = getBytes[22] << 8 | getBytes[23];
            Log.d(TAG, "wav audio compress "+compress+" ("+ch+"ch)");
            //android only support WAV FORMAT PCM, ALAW, MULAW, MSGSM, EXTENSIBLE
            if(compress != 0X0001 && compress != 0x0006 && compress != 0x0007 &&
                compress != 0x0031 && compress != 0xFFFE) {
                return false;    //RTMediaPlayer
            }
            //DHCHERC-1541
            //Android only support 2 channle ALAW
            if ((compress == 0x0006 || compress == 0x0007 || compress == 0xFFFE) && ch > 2) {
                Log.d(TAG, "A-law/u-law with channel "+ch+". Switch to DvdPlayer to decode");
                return false;    //RTMediaPlayer
            }
        }
        else if(header.substring(0, 4).equals("ADIF")){
            return false;    //RTMediaPlayer
        }
        else if(header.indexOf("#EXTM3U") >= 0){
            return false;    //RTMediaPlayer
        }
        else if(url.endsWith(".rmvb") || url.endsWith(".rv")) {
            return false;    //RTMediaPlayer
        }
        else if(url.endsWith(".flac")) {
            if((header.indexOf("matroska") >= 0)) {
                return false;    //RTMediaPlayer
            }
        }
        else if(header.indexOf("alac") >= 0){
            return false;    //RTMediaPlayer
        }
        return true;    //NuPlayer
    }

///////////////////////////////////////////////////////////////////////////////////////////
//// Add by Rtk end ////////////////////////////////////////////////////////////////////////

}
