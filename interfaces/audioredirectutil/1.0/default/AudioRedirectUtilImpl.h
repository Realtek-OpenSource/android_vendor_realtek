/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_AUDIOREDIRECTUTIL_V1_0_H
#define VENDOR_REALTEK_AUDIOREDIRECTUTIL_V1_0_H

#include <hidlmemory/mapping.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <android/hidl/allocator/1.0/IAllocator.h>
#include <AudioCapturer.h>
#include <vendor/realtek/audioredirectutil/1.0/IAudioRedirectUtil.h>
#include <utils/RefBase.h>

namespace vendor {
namespace realtek {
namespace audioredirectutil {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::audioredirectutil::V1_0::IAudioRedirectUtil;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hidl::memory::V1_0::IMemory;

struct AudioRedirectUtilImpl : public IAudioRedirectUtil {
    AudioRedirectUtilImpl();
    ~AudioRedirectUtilImpl();

    Return<int32_t> prepare() override;
    Return<int32_t> flush() override;
    Return<void> startCap(const android::hardware::hidl_memory& mem, startCap_cb _hidl_cb) override;
    Return<int32_t> stopCap() override;
    Return<int32_t> setDelay(int32_t delay) override;

    private:
    android::AudioCapturer *mAudioCapturer;
};

extern "C" IAudioRedirectUtil* HIDL_FETCH_IAudioRedirectUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace audioredirectutil
} // namespace realtek
} // namespace vendor

#endif //VENDOR_AUDIOREDIRECTUTIL_V1_0_H
