/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */
#define LOG_NDEBUG 0
#define LOG_TAG "DPTX-HIDL-UtilImpl"

#include <sys/types.h>
#include <signal.h>
#include <log/log.h>
#include <cutils/properties.h>
//#include <utils/backtrace.h>

#include "Debug.h"
#include "DPTxConstDef.h"
#include "DPTxUtilImpl.h"

using namespace android;
using namespace android_dptx_hidl;
using namespace dptx_hidl;

namespace vendor {
namespace realtek {
namespace dptxutil {
namespace provider {
namespace V1_0 {
namespace implementation {

void
DPTxUtilImpl::sHandleCoreEvent(
    const struct dptx_core_module_callback* callback,
    int32_t event,
    int32_t vic,
    int32_t type)
{
    ALOGD("[%s] event: %d vic: %d type: %d",
        __FUNCTION__,
        event,
        vic,
        type);

    INTERFACE_TYPE it = (INTERFACE_TYPE) type;
    ALOGD("[%s] interface type: %s",
            __FUNCTION__,
            _getInterfaceTypeString(it));

    DPTxUtilImpl* instance = const_cast<DPTxUtilImpl*>(
        static_cast<const DPTxUtilImpl*>(callback));

    // handle event internally first
    if(instance != nullptr) {
        Mutex::Autolock l(instance->mCbLock);
        size_t size = instance->mHidlCallbacks.size();
        for(size_t i=0;i<size;i++) {
            sp<IDPTxUtilCallback> cb = instance->mHidlCallbacks.valueAt(i);
            int32_t key = instance->mHidlCallbacks.keyAt(i);
            ALOGD("[%s] deliver event to callback %d cb %p",
                __FUNCTION__,
                key,
                cb.get());
            if(cb.get() != NULL) {
                cb->handleEvent(event,vic,type);
            }
        }
    }

    ALOGD("[%s] complete",__FUNCTION__);
}

void
DPTxUtilImpl::sHandleEvent(
    const struct dptx_module_callback* callback,
    int32_t event)
{
    ALOGD("[%s] event: %d",
        __FUNCTION__,
        event);

    DPTxUtilImpl* instance = const_cast<DPTxUtilImpl*>(
        static_cast<const DPTxUtilImpl*>(callback));

    // handle event internally first
    if(instance != nullptr) {
        if(instance->mCore != NULL) {
            instance->mCore->handleEvent(event);
        }

        {
            Mutex::Autolock l(instance->mCbLock);
            size_t size = instance->mHidlCallbacks.size();
            for(size_t i=0;i<size;i++) {
                sp<IDPTxUtilCallback> cb = instance->mHidlCallbacks.valueAt(i);
                int32_t key = instance->mHidlCallbacks.keyAt(i);
                ALOGD("[%s] deliver event to callback %d cb %p",
                    __FUNCTION__,
                    key,
                    cb.get());
                if(cb.get() != NULL) {
                    cb->handleEvent(event,-1,-1);
                }
            }
        }
    }

    ALOGD("%s complete",__FUNCTION__);
}

DPTxUtilImpl::DeathRecipient::DeathRecipient(DPTxUtilImpl *util)
        : pUtil(util) {}

void DPTxUtilImpl::DeathRecipient::serviceDied(
        uint64_t cookie,
        const wp<::android::hidl::base::V1_0::IBase>& /*who*/)
{
    ALOGD("[%s] cookie %lld",
        __FUNCTION__,
        cookie);

    int32_t key = (int32_t) cookie;
    pUtil->removeCallback(key);
}

DPTxUtilImpl::DPTxUtilImpl()
: dptx_module_callback_t({sHandleEvent}),
  dptx_core_module_callback_t({sHandleCoreEvent})
{
    ALOGD("DPTxUtilImpl()");

#if 0
    // bad workaround, hw service manager need some time to complete registation
    sleep(1);
#endif

    mDeathRecipient = new DeathRecipient(this);
    mCore = new DPTxServiceCore(this);
    mMonitor = Monitor::Instance();

    // do it in Monitor
# if 0
    bool plugged = mMonitor->getPlugged();
    if(plugged) {
        mCore->handleEvent(EVENT_PLUG_IN);
    }
#endif

    mMonitor->setCallback(this);
    mMonitor->onInitialize();
}

DPTxUtilImpl::~DPTxUtilImpl()
{
    ALOGD("~DPTxUtilImpl()");
    mDeathRecipient = NULL;
    if(mCore != NULL) {
        delete mCore;
        mCore = NULL;
    }
}

Return<int32_t> DPTxUtilImpl::setCallback(
    int32_t key,
    const sp<IDPTxUtilCallback>& callback)
{
    Mutex::Autolock l(mCbLock);
    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
    ALOGD("[%s]: E key %d callback %p keyIndex %d",
        __FUNCTION__,
        key,
        callback.get(),
        keyIdx);

    if(keyIdx < 0) {
        mHidlCallbacks.add(key,callback.get());
        Return<bool> linkResult = callback->linkToDeath(mDeathRecipient,key);

        bool linkSuccess = linkResult.isOk() ?
            static_cast<bool>(linkResult) : false;
        if (!linkSuccess) {
            ALOGW("[%s] Couldn't link death recipient for KeyId %d",
                    __FUNCTION__,
                    key);
        }else{
            ALOGD("[%s] linkToDeath success",
                __FUNCTION__);
        }

    }else{
        ALOGD("[%s] callback of key %d exists %p",
            __FUNCTION__,
            key,
            callback.get());
    }
    return 1;
}

Return<int32_t> DPTxUtilImpl::removeCallback(
    int32_t key)
{
    Mutex::Autolock l(mCbLock);

    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
    if(keyIdx >= 0) {
        sp<IDPTxUtilCallback> cb = mHidlCallbacks.valueAt(keyIdx);
        if(cb != NULL) {
            cb->unlinkToDeath(mDeathRecipient);
        }
    }

    size_t rst = mHidlCallbacks.removeItem(key);

    ALOGD("[%s] remove key:%d rst:%d",
        __FUNCTION__,
        key,
        rst);

    return 1;
}

Return<void>
DPTxUtilImpl::readOutputFormat(readOutputFormat_cb _hidl_cb)
{
    DPTX_OUTPUT_FORMAT fmt = {};
    OutputFormat hidlFmt = {};

    CHECK(mCore != NULL);

    mCore->readOutputFormat(&fmt);
    DP_HIDL_OUTPUT_FORMAT_ASSIGN(hidlFmt,fmt);

    int32_t retVal = 1;
    _hidl_cb(retVal,hidlFmt);
    return Void();
}

Return<int32_t> DPTxUtilImpl::setDisplayMode(int32_t /*mode*/)
{
    // TODO
    return 1;
}

Return<int32_t> DPTxUtilImpl::hasDPTxBackend()
{
    CHECK(mCore != NULL);
    return mCore->hasDPTxBackend();
}

Return<int32_t>
DPTxUtilImpl::saveOutputFormat(const OutputFormat& format)
{
    CHECK(mCore != NULL);
    DPTX_OUTPUT_FORMAT fmt = {};
    DP_HIDL_OUTPUT_FORMAT_ASSIGN(fmt,format);
    mCore->saveOutputFormat(fmt);
    return 1;
}

Return<int32_t>
DPTxUtilImpl::readEdidAutoMode()
{
    CHECK(mCore != NULL);
    int32_t rst = mCore->readEdidAutoMode();
    return rst;
}

Return<int32_t>
DPTxUtilImpl::saveEdidAutoMode(int32_t value)
{
    CHECK(mCore != NULL);
    mCore->saveEdidAutoMode(value);
    return 1;
}

Return<bool>
DPTxUtilImpl::getDPTxEDIDReady()
{
    CHECK(mCore != NULL);
    return mCore->mDPTxPlugged;
}

Return<int32_t>
DPTxUtilImpl::setOutputFormat(
    const OutputFormat& format,
    int32_t flags)
{
    CHECK(mCore != NULL);
    DPTX_OUTPUT_FORMAT fmt = {};
    DP_HIDL_OUTPUT_FORMAT_ASSIGN(fmt,format);
    mCore->setOutputFormat(fmt,flags);
    return 1;
}

void
DPTxUtilImpl::constructEDIDSupportList()
{
    CHECK(mCore != NULL);
    mEDIDConvertVector.clear();

    struct dptx_support_list list = {};
    mCore->getEDIDSupportList(&list);

    for(uint32_t i=0;i<list.tx_support_size;i++) {
        DPTxFormatSupport data;
        memcpy(&data,&(list.tx_support[i]),sizeof(DPTxFormatSupport));
        mEDIDConvertVector.push_back(data);
    }
}

Return<void>
DPTxUtilImpl::getEDIDSupportList(getEDIDSupportList_cb _hidl_cb)
{
    constructEDIDSupportList();

    EDIDSupportList list;
    list.mList = mEDIDConvertVector;

    _hidl_cb(1,list);

    return Void();
}

Return<void>
DPTxUtilImpl::getCurrentOutputFormat(getCurrentOutputFormat_cb _hidl_cb)
{
    DPTX_OUTPUT_FORMAT fmt = {};
    OutputFormat hidlFmt = {};

    CHECK(mCore != NULL);
    mCore->getCurrentOutputFormat(&fmt);
    DP_HIDL_OUTPUT_FORMAT_ASSIGN(hidlFmt,fmt);

    _hidl_cb(1,hidlFmt);

    return Void();
}

IDPTxUtil* HIDL_FETCH_IDPTxUtil(const char* /*name*/) {

    ALOGD("%s : E",__FUNCTION__);

    DPTxUtilImpl* util = new DPTxUtilImpl();
    if(util == nullptr) {
        ALOGE("%s: cannot allocate dptxutil!", __FUNCTION__);
        return nullptr;
    }
    return util;
}

}
}
}
}
}
}
