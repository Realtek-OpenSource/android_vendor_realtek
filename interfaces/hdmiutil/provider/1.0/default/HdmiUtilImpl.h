/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_HDMIUTIL_V1_0_HDMIUTIL_H
#define VENDOR_REALTEK_HDMIUTIL_V1_0_HDMIUTIL_H

#include <utils/Mutex.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>

#include <vendor/realtek/hdmiutil/common/1.0/types.h>
#include <vendor/realtek/hdmiutil/provider/1.0/IHdmiUtil.h>
#include <vendor/realtek/hdmiutil/provider/1.0/IHdmiUtilCallback.h>

// private implementation
#include "HdmiCommon.h"
#include "HDMIMonitor.h"
#include "HDMICallback.h"
#include "HDMIServiceCore.h"

using namespace android_hdmi_hidl;

namespace vendor {
namespace realtek {
namespace hdmiutil {
namespace provider {
namespace V1_0 {
namespace implementation {

using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::hidl_death_recipient;
using ::android::sp;
using ::android::Mutex;

//using ::vendor::realtek::hdmiutil::common::V1_0::VideoFormat;
using ::vendor::realtek::hdmiutil::common::V1_0::EDIDData;
using ::vendor::realtek::hdmiutil::common::V1_0::HDMIFormatSupport;
using ::vendor::realtek::hdmiutil::common::V1_0::EDIDSupportList;
using ::vendor::realtek::hdmiutil::common::V1_0::OutputFormat;
using ::vendor::realtek::hdmiutil::common::V1_0::ColorSet;
using ::vendor::realtek::hdmiutil::common::V1_0::TVCapInfo;

using ::vendor::realtek::hdmiutil::provider::V1_0::IHdmiUtilCallback;
using ::vendor::realtek::hdmiutil::provider::V1_0::IHdmiUtil;

struct HdmiUtilImpl : public IHdmiUtil,
                      public hdmi_module_callback_t,
                      public hdmi_core_module_callback_t /*, public RefBase */ {

    HdmiUtilImpl();
    ~HdmiUtilImpl();

    // HDMI HIDL API
    Return<int32_t> setCallback(int32_t key, const sp<IHdmiUtilCallback>& callback) override;
    Return<int32_t> removeCallback(int32_t key) override;
    Return<void> readOutputFormat(readOutputFormat_cb _hidl_cb) override;
    Return<int32_t> saveOutputFormat(const OutputFormat& format) override;
    Return<int32_t> readEdidAutoMode() override;
    Return<int32_t> saveEdidAutoMode(int32_t value) override;
    Return<bool> checkIfHDMIPlugged() override;
    Return<bool> getHDMIEDIDReady() override;
    Return<void> getEDIDRawData(getEDIDRawData_cb _hidl_cb) override;
    Return<bool> isTVSupport3D() override;
    Return<int32_t> setHDMIFormat3D(int32_t format3d, float fps) override;
    Return<void> getEDIDSupportList(getEDIDSupportList_cb _hidl_cb) override;
    Return<int32_t> setOutputFormat(const OutputFormat& format, int32_t flags) override;
    Return<void> getCurrentOutputFormat(getCurrentOutputFormat_cb _hidl_cb) override;
    Return<int32_t> setHDREotfMode(int32_t mode) override;
    Return<void> getColorSet(
        int32_t vic,
        int32_t hdr,
        int32_t policy,
        getColorSet_cb _hidl_cb) override;

    Return<int32_t> getColorSet2(int32_t vic, int32_t hdr, int32_t policy) override;

    Return<int32_t> saveColorModeEnum(int32_t val) override;
    Return<int32_t> readColorModeEnum() override;

    Return<int32_t> readHDRAutoProperty() override;

    Return<int32_t> checkHDRModeSupported(int32_t mode) override;
    Return<int32_t> isUnderDoviMode(int32_t mode) override;

    Return<int32_t> setHDMIEnable(int32_t enable) override;
    Return<void> getTVCapInfo(getTVCapInfo_cb _hidl_cb) override;

    Return<int32_t> getSinkCapability() override;

    Return<int32_t> acquireLock(int32_t type) override;
    Return<int32_t> releaseLock(int32_t type) override;

    Return<int32_t> setHDCPVersion(int32_t version) override;
    Return<int32_t> getHDCPVersion() override;
    Return<int32_t> getConfiguredHDCPVersion() override;

#if 0
    Return<void> getVideoFormat(getVideoFormat_cb _hidl_cb) override;
    Return<int32_t> checkTVSystem(int32_t inputTvSystem, int32_t colorspace_deepcolor) override;
    Return<int32_t> getTVSystem() override;
    Return<int32_t> getTVSystemForRestored() override;
    Return<int32_t> setHDMIVideoColorMode(int32_t mode) override;
#endif

    Mutex                                       mCbLock;
    //Mutex                                     mEventTypeLock;
    KeyedVector<int32_t, sp<IHdmiUtilCallback>> mHidlCallbacks;
    // HDMIServiceCore provides APIs to access HDMI HW
    HDMIServiceCore *                           mCore;
    HDMIMonitor *                               mMonitor;

    bool                                        mHDMIPlugged;

#if 0
    VideoFormat                                 mFormats;
    EDIDData                                    mEDIDData;
    EDIDSupportList                             mSupportList;
#endif

    std::vector<HDMIFormatSupport>              mEDIDConvertVector;

    static void sHandleEvent(
        const struct hdmi_module_callback* callback,
        int32_t event);

    static void sHandleCoreEvent(
        const struct hdmi_core_module_callback* callback,
        int32_t event,
        int32_t vic,
        int32_t mode,
        int32_t interfaceType);

    void constructEDIDSupportList();

    class DeathRecipient : public hidl_death_recipient {
    public:
        DeathRecipient(HdmiUtilImpl *util);
        //~DeathRecipient(){}

        void serviceDied(
                uint64_t cookie,
                const wp<::android::hidl::base::V1_0::IBase>& who) override;

    private:
        HdmiUtilImpl *pUtil;
    };

    sp<DeathRecipient> mDeathRecipient;

};

extern "C" IHdmiUtil* HIDL_FETCH_IHdmiUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace provider
} // namespace hdmiutil
} // namespace realtek
} // namespace vendor


#endif // VENDOR_REALTEK_HDMIUTIL_V1_0_HDMIUTIL_H
