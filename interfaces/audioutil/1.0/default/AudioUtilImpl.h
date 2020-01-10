/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_AUDIOUTIL_V1_0_AUDIOUTIL_H
#define VENDOR_REALTEK_AUDIOUTIL_V1_0_AUDIOUTIL_H

#include <utils/Mutex.h>
#include <utils/KeyedVector.h>
#include <vendor/realtek/audioutil/1.0/IAudioUtil.h>
#include <vendor/realtek/audioutil/1.0/IAudioUtilCallback.h>
#include <AudioRxPCMSource.h>
#include <AudioRxRawSource.h>
// hdmirx audio uevent monitor
#include <Monitor.h>

using namespace android_hdmirx_audio;

namespace vendor {
namespace realtek {
namespace audioutil {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::audioutil::V1_0::IAudioUtil;
using ::vendor::realtek::audioutil::V1_0::IAudioUtilCallback;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_death_recipient;

struct AudioUtilImpl : public IAudioUtil ,
                       public hdmirx_module_callback_t {
    AudioUtilImpl();
    ~AudioUtilImpl();

    // Methods from ::vendor::realtek::audioutil::V1_0::IAudioUtil follow.
    Return<int32_t> setAudioSpdifOutputMode(int32_t mode) override;
    Return<int32_t> setAudioSpdifOutputOffMode() override;
    Return<int32_t> setAudioHdmiOutputMode(int32_t mode) override;
    Return<int32_t> setAudioAGC(int32_t mode) override;
    Return<int32_t> setAudioForceChannelCtrl(int32_t mode) override;
    Return<int32_t> setAudioSurroundSoundMode(int32_t mode) override;
    Return<int32_t> setAudioHdmiFreqMode() override;
    Return<int32_t> setAudioDecVolume(int32_t volume) override;
    Return<int32_t> setAudioMute(bool mute) override;
    Return<int32_t> setAudioHdmiMute(bool mute) override;
    Return<int32_t> setAudioHdmiRxVolume(int32_t volume, int32_t aoId, int32_t aiId, int32_t pinId) override;
    Return<int32_t> setHDMIRXtoBTUsb(int32_t flag, int32_t aiId) override;

    Return<int32_t> setHDMIRxAudioInstance(bool flag, int32_t sampleRate, int32_t channelCount) override;
    Return<int32_t> setHDMIRxAudioRender(bool flag) override;
    Return<int32_t> setHDMIRxAudioRecord(bool flag) override;
    Return<void> captureHDMIRxAudioData(const hidl_memory& mem, captureHDMIRxAudioData_cb _hidl_cb) override;

    Return<int32_t> setCallback(int32_t key, const sp<IAudioUtilCallback>& callback) override;
    Return<int32_t> removeCallback(int32_t key) override;
    Return<int32_t> setAudioHdmiRxVolume2(int32_t volume) override;

    static void sHandleEvent(
        const struct hdmirx_module_callback* callback,
        int32_t event);

private:
    AudioRxSource *                                 mAudioSource;
    Mutex                                           mCbLock;
    KeyedVector<int32_t, sp<IAudioUtilCallback>>    mHidlCallbacks;

    // uevent monitor
    Monitor *                                       mMonitor;

    class DeathRecipient : public hidl_death_recipient {
    public:
        DeathRecipient(AudioUtilImpl *util);
        //~DeathRecipient(){}

        void serviceDied(
                uint64_t cookie,
                const wp<::android::hidl::base::V1_0::IBase>& who) override;

    private:
        AudioUtilImpl *pUtil;
    };

    sp<DeathRecipient> mDeathRecipient;

};

extern "C" IAudioUtil* HIDL_FETCH_IAudioUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace audiotuil
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_AUDIOUTIL_V1_0_AUDIOUTIL_H
