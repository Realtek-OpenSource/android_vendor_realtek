/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_DPTXUTIL_V1_0_H
#define VENDOR_REALTEK_DPTXUTIL_V1_0_H

#include <utils/Mutex.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>

#include <vendor/realtek/dptxutil/common/1.0/types.h>
#include <vendor/realtek/dptxutil/provider/1.0/IDPTxUtil.h>
#include <vendor/realtek/dptxutil/provider/1.0/IDPTxUtilCallback.h>

#include "DPTxCommon.h"
#include "Monitor.h"
#include "DPTxServiceCore.h"

using namespace android;
using namespace android_dptx_hidl;
using namespace dptx_hidl;

namespace vendor {
namespace realtek {
namespace dptxutil {
namespace provider {
namespace V1_0 {
namespace implementation {

using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::hidl_death_recipient;
using ::android::sp;
using ::android::Mutex;

using ::vendor::realtek::dptxutil::common::V1_0::EDIDData;
using ::vendor::realtek::dptxutil::common::V1_0::DPTxFormatSupport;
using ::vendor::realtek::dptxutil::common::V1_0::EDIDSupportList;
using ::vendor::realtek::dptxutil::common::V1_0::OutputFormat;

using ::vendor::realtek::dptxutil::provider::V1_0::IDPTxUtilCallback;
using ::vendor::realtek::dptxutil::provider::V1_0::IDPTxUtil;

struct DPTxUtilImpl : public IDPTxUtil,
                      public dptx_module_callback_t,
                      public dptx_core_module_callback_t
{
    DPTxUtilImpl();
    ~DPTxUtilImpl();

    // HIDL APIs
    Return<int32_t> setDisplayMode(int32_t mode) override;

    Return<int32_t> hasDPTxBackend() override;

    Return<int32_t> setCallback( int32_t key,
        const sp<IDPTxUtilCallback>& callback) override;

    Return<int32_t> removeCallback(int32_t key) override;

    Return<void> readOutputFormat(readOutputFormat_cb _hidl_cb) override;

    Return<int32_t> saveOutputFormat(const OutputFormat& format) override;

    Return<int32_t> readEdidAutoMode() override;

    Return<int32_t> saveEdidAutoMode(int32_t value) override;

	Return<bool> getDPTxEDIDReady() override;

    Return<int32_t> setOutputFormat(
        const OutputFormat& format,
        int32_t flags) override;

    Return<void> getEDIDSupportList(getEDIDSupportList_cb _hidl_cb) override;

    Return<void> getCurrentOutputFormat(getCurrentOutputFormat_cb _hidl_cb) override;
    // HIDL APIs

    // callback maps
    Mutex                                       mCbLock;
    KeyedVector<int32_t, sp<IDPTxUtilCallback>> mHidlCallbacks;
    std::vector<DPTxFormatSupport>              mEDIDConvertVector;

    DPTxServiceCore *                           mCore;
    Monitor *                                   mMonitor;

    static void sHandleEvent(
        const struct dptx_module_callback* callback,
        int32_t event);

    static void sHandleCoreEvent(
        const struct dptx_core_module_callback* callback,
        int32_t event,
        int32_t vic,
        int32_t type);

    void constructEDIDSupportList();

    class DeathRecipient : public hidl_death_recipient {
    public:
        DeathRecipient(DPTxUtilImpl *util);
        //~DeathRecipient(){}

        void serviceDied(
                uint64_t cookie,
                const wp<::android::hidl::base::V1_0::IBase>& who) override;

    private:
        DPTxUtilImpl *pUtil;
    };

    sp<DeathRecipient> mDeathRecipient;

};

extern "C" IDPTxUtil* HIDL_FETCH_IDPTxUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace provider
} // namespace dptxutil
} // namespace realtek
} // namespace vendor

#endif
