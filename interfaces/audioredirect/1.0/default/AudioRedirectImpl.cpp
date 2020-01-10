/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "AudioRedirectImpl" 

#include <log/log.h>

#include "AudioRedirectImpl.h"

namespace vendor {
namespace realtek {
namespace audioredirect {
namespace V1_0 {
namespace implementation {

AudioRedirectImpl::AudioRedirectImpl()
{
    ALOGV("%s", __FUNCTION__);
}

AudioRedirectImpl::~AudioRedirectImpl()
{
    ALOGV("%s", __FUNCTION__);
}

Return<int32_t> AudioRedirectImpl::startAudioCap()
{
    ALOGV("%s", __FUNCTION__);
#if 1
    if (mAudioRedirect == NULL) {
        mAudioRedirect = new RtkAudioRedirect;
        mAudioRedirectLooper = new ALooper;
        mAudioRedirectLooper->setName("mAudioRedirect");
        mAudioRedirectLooper->registerHandler(mAudioRedirect);
        mAudioRedirectLooper->start();
    }
    mAudioRedirect->startAudioCap(static_cast<AHandler *>(mAudioRedirect.get()));
    mState = mState | 0x00000001;
#endif
    return 0;
}

Return<int32_t> AudioRedirectImpl::stopAudioCap()
{
    ALOGV("%s", __FUNCTION__);
#if 1
    mAudioRedirect->stopAudioCap(mAudioRedirect);
    mState = mState & 0x11111110;
    if (!mState) {
        mAudioRedirectLooper->unregisterHandler(mAudioRedirect->id());
        mAudioRedirectLooper->stop();
        mAudioRedirect.clear();
        mAudioRedirect = NULL;
        mAudioRedirectLooper.clear();
        mAudioRedirectLooper = NULL;
    }
#endif
    return 0;
}

Return<int32_t> AudioRedirectImpl::startAudioRender()
{
    ALOGV("%s", __FUNCTION__);
#if 1
    if (mAudioRedirect == NULL) {
        mAudioRedirect = new RtkAudioRedirect;
        mAudioRedirectLooper = new ALooper;
        mAudioRedirectLooper->setName("mAudioRedirect");
        mAudioRedirectLooper->registerHandler(mAudioRedirect);
        mAudioRedirectLooper->start();
    }

    if (mRenderClient == 0) {
        mAudioRedirect->startAudioRender(static_cast<AHandler *>(mAudioRedirect.get()));
    }

    mRenderClient++;
    ALOGV("num of mRenderClient = %d\n", mRenderClient);
    mState = mState | 0x00000010;
#endif
    return 0;
}

Return<int32_t> AudioRedirectImpl::flushAudioRender()
{
    ALOGV("%s", __FUNCTION__);
#if 1
    if(mAudioRedirect != NULL)
        mAudioRedirect->flushAudioRender(static_cast<AHandler *>(mAudioRedirect.get()));
#endif
    return 0;
}

Return<int32_t> AudioRedirectImpl::stopAudioRender()
{
    ALOGV("%s", __FUNCTION__);
#if 1
    if (mRenderClient == 0) {
        ALOGV("should not be there");
        return OK;
    }

    mRenderClient--;
    if (mRenderClient == 0) {
        mAudioRedirect->stopAudioRender(static_cast<AHandler *>(mAudioRedirect.get()));
        mState = mState & 0x11111101;

        if (!mState) {
            mAudioRedirectLooper->unregisterHandler(mAudioRedirect->id());
            mAudioRedirectLooper->stop();
            mAudioRedirect.clear();
            mAudioRedirect = NULL;
            mAudioRedirectLooper.clear();
            mAudioRedirectLooper = NULL;
        }
    }
#endif
    return 0;
}

Return<int32_t> AudioRedirectImpl::setAudioMode(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
#if 1
    if(mAudioRedirect != NULL)
        mAudioRedirect->setAudioMode(static_cast<AHandler *>(mAudioRedirect.get()), mode);
#endif
    return 0;
}

IAudioRedirect* HIDL_FETCH_IAudioRedirect(const char* /*name*/) {
    AudioRedirectImpl* audioredirect = new AudioRedirectImpl();
    if(audioredirect == nullptr) {
        ALOGE("%s: cannot allocate audioredirect!", __FUNCTION__);
        return nullptr;
    }
    return audioredirect;
}

} // namespace implementation
} // namespace V1_0
} // namespace audioredirect
} // namespace realtek
} // namespace vendor
