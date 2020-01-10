/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "AudioUtilImpl"

#include <log/log.h>
#include <cutils/properties.h>
#include <AudioUtil.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>
#include "AudioUtilImpl.h"

using ::android::hidl::memory::V1_0::IMemory;

namespace vendor {
namespace realtek {
namespace audioutil {
namespace V1_0 {
namespace implementation {

AudioUtilImpl::DeathRecipient::DeathRecipient(AudioUtilImpl *util)
        : pUtil(util) {}

void AudioUtilImpl::DeathRecipient::serviceDied(
        uint64_t cookie,
        const wp<::android::hidl::base::V1_0::IBase>& /*who*/) {
    ALOGD("[%s] cookie %lld",__FUNCTION__,cookie);
    int32_t key = (int32_t) cookie;
    pUtil->removeCallback(key);
}


void
AudioUtilImpl::sHandleEvent(
    const struct hdmirx_module_callback* callback,
    int32_t event)
{

    ALOGD("[%s][HDMIRx] event:%d [%s]",
            __FUNCTION__,
            event,
            event2Str(event));

    AudioUtilImpl* instance = const_cast<AudioUtilImpl*>(
        static_cast<const AudioUtilImpl*>(callback));

    // handle internally
    if(instance != NULL) {
        // handle internally first
        if( event == EVENT_RX_AUDIO )
        {
            if(instance->mAudioSource != NULL) {
                int32_t state = getHDMIRxAudioState();
                instance->mAudioSource->handleRxAudioChanged(state);
            }else{
                ALOGD("No flow detected, do nothing");
            }
        }
    }

    if(instance != NULL) {
        Mutex::Autolock l(instance->mCbLock);
        size_t size = instance->mHidlCallbacks.size();
        for(size_t i=0;i<size;i++) {
            sp<IAudioUtilCallback> cb = instance->mHidlCallbacks.valueAt(i);
            int32_t key = instance->mHidlCallbacks.keyAt(i);
            ALOGD("[%s][HDMIRx] deliver event [%s] to callback %d cb %p",
                __FUNCTION__,
                event2Str(event),
                key,
                cb.get());
            if(cb.get() != NULL) {
                cb->handleEvent(event);
            }
        }
    }
}

AudioUtilImpl::AudioUtilImpl()
    : mAudioSource(NULL),
      hdmirx_module_callback_t({sHandleEvent})
{
    ALOGV("%s", __FUNCTION__);

    AudioUtil_Init();

    mDeathRecipient = new DeathRecipient(this);
    // uevent monitor
    mMonitor = Monitor::Instance();
    mMonitor->setCallback(this);
    mMonitor->onInitialize();
}

AudioUtilImpl::~AudioUtilImpl()
{
    ALOGV("%s", __FUNCTION__);
    AudioUtil_Deinit();
}

Return<int32_t> AudioUtilImpl::setAudioSpdifOutputMode(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioSpdifOutputMode(mode);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioSpdifOutputOffMode()
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioSpdifOutputOffMode();
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioHdmiOutputMode(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioHdmiOutputMode(mode);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioAGC(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioAGCMode(mode);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioForceChannelCtrl(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioForceChannelCtrl(mode);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioSurroundSoundMode(int32_t mode)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioSurroundSoundMode(mode);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioHdmiFreqMode()
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioHdmiFreqMode();
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioDecVolume(int32_t volume)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioDecVolume(volume);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioMute(bool mute)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioMute(mute);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioHdmiMute(bool mute)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioHdmiMute(mute);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioHdmiRxVolume(int32_t volume, int32_t aoId, int32_t aiId, int32_t pinId)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetAudioHdmiRxVolume(volume, aoId, aiId, pinId);
    return ret;
}

Return<int32_t> AudioUtilImpl::setAudioHdmiRxVolume2(int32_t volume)
{
    ALOGV("%s", __FUNCTION__);
    if(mAudioSource != NULL) {
        ALOGD("[%s] volume: %d",__FUNCTION__,volume);
        mAudioSource->setAOVolume(volume);
    }else{
        ALOGV("[%s] no HDMIRxAudio instance exists",__FUNCTION__);
    }
    return 0;
}

Return<int32_t> AudioUtilImpl::setHDMIRXtoBTUsb(int32_t flag, int32_t aiId)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)SetHDMIRXtoBTUsb(flag, aiId);
    return ret;
}

Return<int32_t> AudioUtilImpl::setHDMIRxAudioInstance(bool flag, int32_t sampleRate, int32_t channelCount)
{
    if(flag) {
        if(mAudioSource != NULL) {
            ALOGW("AudioSource is being used by other process!!!");
            return -1;
        }

        char value[100];
        property_get("persist.vendor.rtk.hdmirx.raw", value, "0");
        if(strcmp(value, "0") == 0) {
            mAudioSource = new AudioRxPCMSource;
            ALOGD("%s:%d HDMI Rx PCM input mode", __FUNCTION__, __LINE__);
        } else {
            mAudioSource = new AudioRxRawSource;
            ALOGD("%s:%d HDMI Rx RAW input mode", __FUNCTION__, __LINE__);
        }

        mAudioSource->setAudioParams(sampleRate, channelCount);
        mAudioSource->prepare();
        mAudioSource->setRenderAudio(true);
        mAudioSource->start();
    } else {
        if(mAudioSource != NULL) {
            mAudioSource->stop();
            delete mAudioSource;
            mAudioSource = NULL;
        }
    }
    return 0;
}

Return<int32_t> AudioUtilImpl::setHDMIRxAudioRender(bool flag)
{
    int32_t ret = -1;
    if(mAudioSource != NULL)
        ret = mAudioSource->setRenderAudio(flag);
    return ret;
}

Return<int32_t> AudioUtilImpl::setHDMIRxAudioRecord(bool flag)
{
    int32_t ret = -1;
    if(mAudioSource != NULL)
        ret = mAudioSource->setRecordAudio(flag);
    return ret;
}

Return<void> AudioUtilImpl::captureHDMIRxAudioData(const hidl_memory& mem, captureHDMIRxAudioData_cb _hidl_cb)
{
    int32_t status = -1;
    RxAudioFrame audioFrame = {NULL, 0, 0, 0};

    sp<IMemory> memory = mapMemory(mem);

    if (memory == nullptr) {
        ALOGE("Could not map hidl_memory");
        _hidl_cb(mem, -1, -1, -1, -1, -1);
        return Void();
    }

    audioFrame.bufferAddr = static_cast<uint8_t*>(static_cast<void*>(memory->getPointer()));

    memory->update();
    if(mAudioSource != NULL) {
        status = mAudioSource->captureAudio(audioFrame);
    }
    memory->commit();

    _hidl_cb(mem, audioFrame.timeStamp, audioFrame.frameSize, audioFrame.channelCount, audioFrame.sampleRate, status);
    return Void();
}

Return<int32_t> AudioUtilImpl::setCallback(
    int32_t key,
    const sp<IAudioUtilCallback>& callback)
{
    Mutex::Autolock l(mCbLock);
    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
    if(keyIdx < 0) {
        mHidlCallbacks.add(key,callback.get());
        Return<bool> linkResult = callback->linkToDeath(mDeathRecipient,key);

        bool linkSuccess = linkResult.isOk() ?
            static_cast<bool>(linkResult) : false;
        if (!linkSuccess) {
            ALOGW("[%s] Couldn't link death recipient for KeyId %d",
                    __FUNCTION__,
                    key);
        }
    }
    return 1;
}

Return<int32_t> AudioUtilImpl::removeCallback(
    int32_t key)
{
    Mutex::Autolock l(mCbLock);

    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
    if(keyIdx >= 0) {
        sp<IAudioUtilCallback> cb = mHidlCallbacks.valueAt(keyIdx);
        if(cb != NULL) {
            cb->unlinkToDeath(mDeathRecipient);
        }
    }

    size_t rst = mHidlCallbacks.removeItem(key);
    return 1;
}

IAudioUtil* HIDL_FETCH_IAudioUtil(const char* /*name*/) {
    AudioUtilImpl* audioutil = new AudioUtilImpl();
    if(audioutil == nullptr) {
        ALOGE("%s: cannot allocate audioutil!", __FUNCTION__);
        return nullptr;
    }
    return audioutil;
}

} // namespace implementation
} // namespace V1_0
} // namespace audioutil
} // namespace realtek
} // namespace vendor
