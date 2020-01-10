/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "AudioRedirectUtilImpl"

#include <log/log.h>

#include "AudioRedirectUtilImpl.h"

namespace vendor {
namespace realtek {
namespace audioredirectutil {
namespace V1_0 {
namespace implementation {

AudioRedirectUtilImpl::AudioRedirectUtilImpl()
{
    ALOGV("AudioRedirectUtilImpl()");
    mAudioCapturer = new android::AudioCapturer;
}

AudioRedirectUtilImpl::~AudioRedirectUtilImpl()
{
    ALOGV("~AudioRedirectUtilImpl()");
    delete mAudioCapturer;
}

Return<int32_t> AudioRedirectUtilImpl::prepare()
{
    ALOGV("%s : %d",__FUNCTION__,__LINE__);
    int32_t err = mAudioCapturer->prepare();
    return err;
}

Return<int32_t> AudioRedirectUtilImpl::flush()
{
    ALOGV("%s : %d",__FUNCTION__,__LINE__);
    int32_t err = mAudioCapturer->flush();
    return err;
}

Return<void> AudioRedirectUtilImpl::startCap(const android::hardware::hidl_memory& mem, startCap_cb _hidl_cb)
{
    ALOGV("%s : %d",__FUNCTION__,__LINE__);

    int32_t state = -1;
    android::FRAME audioFrame = {NULL, 0, 0, 0};

    android::sp<IMemory> memory = mapMemory(mem);

    if (memory == nullptr) {
        ALOGE("Could not map hidl_memory");
        _hidl_cb(mem, -1, -1);
        return Void();
    }

    audioFrame.bufferAddr = static_cast<uint8_t*>(static_cast<void*>(memory->getPointer()));

    memory->update();
    if(mAudioCapturer != NULL) {
        state = mAudioCapturer->startCap(&audioFrame);
    }
    memory->commit();
    ALOGV("state = %d, bufferSize = %d", state, audioFrame.frameSize);
    _hidl_cb(mem, state, audioFrame.frameSize);
    return Void();
}

Return<int32_t> AudioRedirectUtilImpl::stopCap()
{
    ALOGV("%s : %d",__FUNCTION__,__LINE__);
    int32_t err = mAudioCapturer->stopCap();
    return err;
}

Return<int32_t> AudioRedirectUtilImpl::setDelay(int32_t delay)
{
    ALOGV("%s : %d",__FUNCTION__,__LINE__);
    int32_t err = mAudioCapturer->setDelay(delay);
    return err;
}

IAudioRedirectUtil* HIDL_FETCH_IAudioRedirectUtil(const char* /*name*/) {
    AudioRedirectUtilImpl* audioredirectutil = new AudioRedirectUtilImpl();
    if(audioredirectutil == nullptr) {
        ALOGE("%s: cannot allocate audioredirectutil!", __FUNCTION__);
        return nullptr;
    }
    return audioredirectutil;
}

} // namespace implementation
} // namespace V1_0
} // namespace audioredirectutil
} // namespace realtek
} // namespace vendor
