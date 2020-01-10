#ifndef ANDROID_RTK_MEDIAPLAYER_H
#define ANDROID_RTK_MEDIAPLAYER_H

#include <utils/KeyedVector.h>
#include <utils/Mutex.h>
#include "IMediaPlayer.h"
#include <media/mediaplayer.h>

namespace android
{


class RtkMediaPlayer
{
public:
    RtkMediaPlayer();
    ~RtkMediaPlayer();
    status_t        setListener(const sp<MediaPlayerListener>& listener);
    status_t        setDataSource(
            const char *url,
            const KeyedVector<String8, String8> *headers);
    
    status_t        setDataSource(int fd, int64_t offset, int64_t length);
    status_t        getMetadata(bool update_only, bool apply_filter, Parcel *metadata);
    status_t        setVideoSurfaceTexture(
                            const sp<IGraphicBufferProducer>& bufferProducer);
    status_t        prepare();
    status_t        prepareAsync();
    status_t        start();
    status_t        stop();
    status_t        pause();
    status_t        reset();
    bool            isPlaying();
    status_t        seekTo(
            int msec,
            MediaPlayerSeekMode mode = MediaPlayerSeekMode::SEEK_PREVIOUS_SYNC);
    status_t        getVideoWidth(int *w);
    status_t        getVideoHeight(int *h);
    status_t        getCurrentPosition(int *msec);
    status_t        getDuration(int *msec);
    status_t        setAudioStreamType(audio_stream_type_t type);
    status_t        getAudioStreamType(audio_stream_type_t *type);
    status_t        setLooping(int loop);
    bool            isLooping();
    status_t        setVolume(float leftVolume, float rightVolume);
    audio_session_t   getAudioSessionId();
    status_t        fastforward(int speed);
    status_t        fastrewind(int speed);
    status_t        setSubtitleInfo(int streamNum,int enable,int textEncoding,int textColor,int fontSize, int syncTime, int offset);
    status_t        setAutoScanExtSubtitle(int autoScan);
    status_t        setExtSubtitlePath(const char *path);
    status_t        setAudioTrackInfo(int streamNum);
    status_t        deliverNaviControlCmd(int action, int status);
    status_t        setParameter(int key, const Parcel& request);
    status_t        getParameter(int key, Parcel* reply);
    status_t        notify(int msg, int ext1, int ext2, const Parcel *obj = NULL);
    status_t        invoke(const Parcel& request, Parcel *reply);

    static void notifyListener(int msg, int ext1, int ext2, const Parcel *obj);

private:
    Mutex   mLock;
    sp<MediaPlayerListener>     mListener;
    audio_stream_type_t         mStreamType;
    bool                        mLoop;


};

}; // namespace android

#endif // ANDROID_RT_MEDIAPLAYER_H
