/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_AUDIOREDIRECT_V1_0_AUDIOREDIRECT_H
#define VENDOR_REALTEK_AUDIOREDIRECT_V1_0_AUDIOREDIRECT_H

#include <vendor/realtek/audioredirect/1.0/IAudioRedirect.h>

#if 1
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AHandler.h>
#include <RtkAudioRedirect.h>
#endif

using namespace android;

namespace vendor {
namespace realtek {
namespace audioredirect {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::audioredirect::V1_0::IAudioRedirect;
using ::android::hardware::Return;
using ::android::hardware::Void;

struct AudioRedirectImpl : public IAudioRedirect {
    AudioRedirectImpl();
    ~AudioRedirectImpl();

    // Methods from ::vendor::realtek::audioredirect::V1_0::IAudioRedirect follow.
    Return<int32_t> startAudioCap() override;
    Return<int32_t> stopAudioCap() override;
    Return<int32_t> startAudioRender() override;
    Return<int32_t> flushAudioRender() override;
    Return<int32_t> stopAudioRender() override;
    Return<int32_t> setAudioMode(int32_t mode) override;

#if 1
private:
    sp<RtkAudioRedirect> mAudioRedirect;
    sp<ALooper> mAudioRedirectLooper;
    uint32_t mRenderClient;
    uint32_t mState;
    mutable Mutex mLock;
#endif
};

extern "C" IAudioRedirect* HIDL_FETCH_IAudioRedirect(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace audiotuil
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_AUDIOREDIRECT_V1_0_AUDIOREDIRECT_H
