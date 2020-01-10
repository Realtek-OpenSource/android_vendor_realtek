/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "HDMI-HIDL-HDMICoreImpl"

#include <sys/types.h>
#include <signal.h>
#include <log/log.h>
//#include <utils/RefBase.h>
#include <cutils/properties.h>
//#include <utils/backtrace.h>
#include "HdmiUtilImpl.h"
#include "Debug.h"

#define LOG_LINE()                              \
    do {                                        \
        ALOGD("LLINELOGGER [%s]: %d",__FUNCTION__,__LINE__); \
    } while(0)

namespace vendor {
namespace realtek {
namespace hdmiutil {
namespace provider {
namespace V1_0 {
namespace implementation {

void
HdmiUtilImpl::sHandleCoreEvent(
    const struct hdmi_core_module_callback* callback,
    int32_t event,
    int32_t vic,
    int32_t mode,
    int32_t interfaceType)
{

    HdmiUtilImpl* instance = const_cast<HdmiUtilImpl*>(
        static_cast<const HdmiUtilImpl*>(callback));

    if(instance != nullptr) {

        ALOGV("%s event %s start",
            __FUNCTION__,
            getEventString(event));

        // deliver to client side
        {
            Mutex::Autolock l(instance->mCbLock);
            size_t size = instance->mHidlCallbacks.size();
            for(size_t i=0;i<size;i++) {
                sp<IHdmiUtilCallback> cb = instance->mHidlCallbacks.valueAt(i);
                int32_t key = instance->mHidlCallbacks.keyAt(i);
                ALOGV("[%s] deliver event to callback %d cb %p",
                    __FUNCTION__,
                    key,
                    cb.get());
                if(cb.get() != NULL) {
                    cb->handleEvent(event,vic,mode,interfaceType);
                }
            }
        }
    }

    ALOGV("%s complete",__FUNCTION__);
}

void
HdmiUtilImpl::sHandleEvent(
    const struct hdmi_module_callback* callback,
    int32_t event)
{

    ALOGV("[%s] : E",__FUNCTION__);

    HdmiUtilImpl* instance = const_cast<HdmiUtilImpl*>(
        static_cast<const HdmiUtilImpl*>(callback));

    if(instance != nullptr) {

        // let HDMIServiceCore handle event first
        if(instance->mCore != NULL) {
            // monitor thread.
            instance->mCore->waitHDCPAuthState();

            {
                //Mutex::Autolock l(instance->mCore->mLock);
                instance->mCore->handleEvent(event);
            }
        }

        ALOGV("%s event %d start",
            __FUNCTION__,
            event);

        // then , deliver to client side
        {
            Mutex::Autolock l(instance->mCbLock);
            size_t size = instance->mHidlCallbacks.size();
            for(size_t i=0;i<size;i++) {
                sp<IHdmiUtilCallback> cb = instance->mHidlCallbacks.valueAt(i);
                int32_t key = instance->mHidlCallbacks.keyAt(i);
                ALOGV("[%s] deliver event to callback %d cb %p",
                    __FUNCTION__,
                    key,
                    cb.get());
                if(cb.get() != NULL) {
                    cb->handleEvent(event,-1,-1,-1);
                }
            }
        }
    }

    ALOGV("%s complete",__FUNCTION__);
}

HdmiUtilImpl::DeathRecipient::DeathRecipient(HdmiUtilImpl *util)
        : pUtil(util) {}

void HdmiUtilImpl::DeathRecipient::serviceDied(
        uint64_t cookie,
        const wp<::android::hidl::base::V1_0::IBase>& /*who*/) {
    ALOGD("[%s] cookie %lld",__FUNCTION__,cookie);
    int32_t key = (int32_t) cookie;
    pUtil->removeCallback(key);
}

HdmiUtilImpl::HdmiUtilImpl()
: hdmi_module_callback_t({sHandleEvent}),
  hdmi_core_module_callback_t({sHandleCoreEvent}),
  mHDMIPlugged(false)
{
    ALOGD("HdmiUtilImpl()");

#if 0
    // bad workaround, hw service manager need some time to complete registation
    sleep(1);
#endif

    mDeathRecipient = new DeathRecipient(this);
    mCore = new HDMIServiceCore(this);
    mMonitor = HDMIMonitor::Instance();

    // do it in monitor
#if 0
    bool plugged = mMonitor->getHDMIPlugged();
    if(plugged) {
        mCore->handleEvent(EVENT_PLUG_IN);
    }
#endif

    mMonitor->setCallback(this);
    // HDCP extension
    mMonitor->setHDCPCallback(mCore->mHDCPCore);
    // when all callbacks are set, init monitor
    mMonitor->onInitialize();
}

HdmiUtilImpl::~HdmiUtilImpl()
{
    ALOGD("~HdmiUtilImpl()");
    mDeathRecipient = NULL;
    if(mCore != NULL) {
        delete mCore;
        mCore = NULL;
    }
}

Return<int32_t> HdmiUtilImpl::removeCallback(
    int32_t key)
{
    Mutex::Autolock l(mCbLock);

    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
    if(keyIdx >= 0) {
        sp<IHdmiUtilCallback> cb = mHidlCallbacks.valueAt(keyIdx);
        if(cb != NULL) {
            cb->unlinkToDeath(mDeathRecipient);
        }
    }

    size_t rst = mHidlCallbacks.removeItem(key);

#if 0
    //backtrace("HDMICB");
    ALOGD("[%s] remove key:%d rst:%d",
        __FUNCTION__,
        key,
        rst);
#endif

    return 1;
}

Return<int32_t> HdmiUtilImpl::setCallback(
    int32_t key,
    const sp<IHdmiUtilCallback>& callback)
{
    Mutex::Autolock l(mCbLock);
    ssize_t keyIdx = mHidlCallbacks.indexOfKey(key);
#if 0
    ALOGD("[%s]: E key %d callback %p keyIndex %d",
        __FUNCTION__,
        key,
        callback.get(),
        keyIdx);
#endif
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
#if 0
            ALOGD("[%s] linkToDeath success",
                __FUNCTION__);
#endif
        }

    }else{
#if 0
        ALOGD("[%s] callback of key %d exists %p",
            __FUNCTION__,
            key,
            callback.get());
#endif
    }
    //ALOGD("[%s] : E",__FUNCTION__);
    return 1;
}

Return<int32_t> HdmiUtilImpl::readEdidAutoMode()
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    int32_t rst = mCore->readEdidAutoMode();
    return rst;
}

Return<int32_t> HdmiUtilImpl::saveEdidAutoMode(int32_t value)
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    mCore->saveEdidAutoMode(value);
    return 1;
}

Return<int32_t> HdmiUtilImpl::saveColorModeEnum(int32_t val)
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    mCore->saveColorModeEnum(val);
    return 1;
}

Return<int32_t> HdmiUtilImpl::readColorModeEnum()
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    return mCore->readColorModeEnum();
}

Return<int32_t> HdmiUtilImpl::readHDRAutoProperty()
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    return mCore->readHDRAutoProperty();
}

Return<int32_t> HdmiUtilImpl::saveOutputFormat(const OutputFormat &format)
{
    ALOGD("%s vic:%d freq_shift:%d color:%d color_depth:%d (and more...)",
        __FUNCTION__,
        format.vic,
        format.freq_shift,
        format.color,
        format.color_depth);

    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);

    HDMI_OUTPUT_FORMAT fmt;
    HIDL_OUTPUT_FORMAT_ASSIGN(fmt,format);
    mCore->saveOutputFormat(fmt);

    return 1;
}

Return<void> HdmiUtilImpl::readOutputFormat(readOutputFormat_cb _hidl_cb)
{
    HDMI_OUTPUT_FORMAT fmt = {};
    OutputFormat format = {};

    CHECK(mCore != NULL);
    status_t rst = OK;

    {
        Mutex::Autolock l(mCore->mLock);
        rst = mCore->readOutputFormat(&fmt);
    }

    if(rst == OK) {
        HIDL_OUTPUT_FORMAT_ASSIGN(format,fmt);
    }

    int32_t retVal = (rst==OK)?1:-1;
    _hidl_cb(retVal,format);
    return Void();
}

Return<int32_t> HdmiUtilImpl::getColorSet2(
    int32_t vic,
    int32_t hdr,
    int32_t policy)
{
    ALOGD("Call %s",__FUNCTION__);

    COLOR_TYPE color = COLOR_NONE;
    COLOR_TYPE color_depth = DEPTH_NONE;

    CHECK(mCore != NULL);

    {
        Mutex::Autolock l(mCore->mLock);
        mCore->resolveColorSet(
            &color,
            &color_depth,
            vic,
            (HDMI_HDR_MODE) hdr,
            policy);
    }

    if(color_depth == DEPTH_NONE || color == COLOR_NONE) {
        return -1;
    }else{
        int32_t color_rst = GEN_COLOR_RST(color,color_depth);
        return color_rst;
    }
}

Return<void> HdmiUtilImpl::getColorSet(
    int32_t vic,
    int32_t hdr,
    int32_t policy,
    getColorSet_cb _hidl_cb)
{
    ALOGD("Call %s",__FUNCTION__);

    COLOR_TYPE color = COLOR_NONE;
    COLOR_TYPE color_depth = DEPTH_NONE;

    CHECK(mCore != NULL);

    {
        Mutex::Autolock l(mCore->mLock);
        mCore->resolveColorSet(
            &color,
            &color_depth,
            vic,
            (HDMI_HDR_MODE) hdr,
            policy);
    }

    ColorSet rst={-1,-1};
    rst.color = color;
    rst.color_depth = color_depth;

    _hidl_cb(1,rst);

    return Void();
}

Return<void> HdmiUtilImpl::getCurrentOutputFormat(getCurrentOutputFormat_cb _hidl_cb)
{
    HDMI_OUTPUT_FORMAT fmt = {};
    OutputFormat format = {};

    CHECK(mCore != NULL);
    status_t rst = OK;

    {
        // [TODO] disable for now
        // cause might cause potential dead lock between DPTx
        // Mutex::Autolock l(mCore->mLock);
        rst = mCore->getCurrentOutputFormat(&fmt);
    }

    if(rst == OK) {
        HIDL_OUTPUT_FORMAT_ASSIGN(format,fmt);
    }

    int32_t retVal = (rst==OK)?1:-1;
    _hidl_cb(retVal,format);
    return Void();
}

Return<bool> HdmiUtilImpl::checkIfHDMIPlugged()
{
    bool plugged = mMonitor->getHDMIPlugged();
    return plugged;
}

Return<bool> HdmiUtilImpl::getHDMIEDIDReady()
{
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    bool ready = mCore->isEDIDReady();
    return ready;
}

Return<void> HdmiUtilImpl::getEDIDSupportList(getEDIDSupportList_cb _hidl_cb)
{
    constructEDIDSupportList();

    EDIDSupportList supportList;
    supportList.mList = mEDIDConvertVector;

    _hidl_cb(1, supportList);
    return Void();
}

Return<void> HdmiUtilImpl::getTVCapInfo(getTVCapInfo_cb _hidl_cb)
{
    struct sink_capabilities_t * pSinkCap = mCore->getTVCapInfo();

    TVCapInfo info;
    memset(&info,0x0,sizeof(TVCapInfo));

    info.mConnected     = mCore->isSinkConnected();
    info.mEotf          =  pSinkCap->vout_edid_data.et;
    info.mDV            = (pSinkCap->vout_edid_data.dolby_len>0)?1:0;
    info.mMetaDataLen   =  pSinkCap->vout_edid_data.metadata_len;
    info.mMaxLuminance  =  pSinkCap->vout_edid_data.max_luminance;
    info.mAvgLuminance  =  pSinkCap->vout_edid_data.max_frame_avg;
    info.mMinLuminance  =  pSinkCap->vout_edid_data.min_luminance;

    _hidl_cb(1,info);

    return Void();
}

#if 0
Return<int32_t> HdmiUtilImpl::setHDMIVideoColorMode(
    int32_t mode)
{
    return 0;
}
#endif

Return<void> HdmiUtilImpl::getEDIDRawData(getEDIDRawData_cb _hidl_cb)
{
    uint8_t buffer[EDID_DATA_LENGTH] = {};

    CHECK(mCore != NULL);
    {
        Mutex::Autolock l(mCore->mLock);
        //ALOGD("HDMIEDID2 pBuf %p",pBuf);
        mCore->getEDID(buffer);
    }
#if 0
    for(int i=0;i<EDID_DATA_LENGTH;i++) {
        ALOGD("%s HDMIEDID2-2 %d [%p]",__FUNCTION__,pBuf[i],&(pBuf[i]));
    }
#endif

    std::vector<uint8_t> buffer_vec;
    for(int i=0;i<EDID_DATA_LENGTH;i++) {
        //ALOGD("%s HDMIEDID %.2x",__FUNCTION__,buffer[i]);
        buffer_vec.push_back(buffer[i]);
    }

    EDIDData data;
    data.mData = buffer_vec;

    _hidl_cb(1, data);

    return Void();
}

Return<bool> HdmiUtilImpl::isTVSupport3D()
{
    return false;
}

Return<int32_t> HdmiUtilImpl::getSinkCapability()
{
    CHECK(mCore != NULL);
    ALOGD("Call [%s]",__FUNCTION__);
    mCore->getSinkCapability();
    return 0;
}

Return<int32_t> HdmiUtilImpl::setHDMIFormat3D(
    int32_t format3d,
    float fps)
{
    CHECK(mCore != NULL);
    mCore->waitHDCPAuthState();
    {
        Mutex::Autolock l(mCore->mLock);
        mCore->setHDMIFormat3D(format3d,fps);
    }
    return 0;
}

// new API designed for new hdmi ioctl
Return<int32_t> HdmiUtilImpl::setOutputFormat(
    const OutputFormat &format,
    int32_t flags)
{
    CHECK(mCore != NULL);
    mCore->waitHDCPAuthState();
    {
        Mutex::Autolock l(mCore->mLock);

        if(flags & EXTRA_FORCE_OUTPUT_FORMAT) {
            HDMI_OUTPUT_FORMAT fmt;
            HIDL_OUTPUT_FORMAT_ASSIGN(fmt,format);
            mCore->forceSetOutputFormat(fmt,flags,false);

        }else if(flags & (EXTRA_RESET_COLOR_MODE|EXTRA_RESET_TV_SYSTEM)){
            mCore->setOutputFormatViaHDR(
                (HDMI_HDR_MODE) format.hdr,
                flags);
        }else{
            mCore->setOutputFormat(
                format.vic,
                format.freq_shift,
                format.color,
                format.color_depth,
                format._3d_format,
                format.hdr,
                flags);
        }
    }
    return 0;
}

Return<int32_t> HdmiUtilImpl::setHDREotfMode(int32_t mode)
{
    ALOGD("%s mode:%d",__FUNCTION__,mode);

    CHECK(mCore != NULL);
    mCore->waitHDCPAuthState();
    {
        Mutex::Autolock l(mCore->mLock);
        mCore->setHDREotfMode(mode);
    }
    return 1;
}

Return<int32_t> HdmiUtilImpl::checkHDRModeSupported(int32_t mode)
{
    ALOGD("%s mode:%d",__FUNCTION__,mode);

    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);

    bool rst = mCore->checkHDRModeSupported( (HDMI_HDR_MODE) mode);
    return (rst)?1:0;
}

Return<int32_t> HdmiUtilImpl::isUnderDoviMode(int32_t mode)
{
    ALOGV("%s mode:%d",__FUNCTION__,mode);
    CHECK(mCore != NULL);
    Mutex::Autolock l(mCore->mLock);
    bool rst = mCore->isUnderDoviMode((HDMI_HDR_MODE) mode);
    return (rst)?1:0;
}

Return<int32_t> HdmiUtilImpl::setHDMIEnable(int32_t enable)
{
    CHECK(mCore != NULL);
    mCore->waitHDCPAuthState();
    {
        Mutex::Autolock l(mCore->mLock);
        mCore->setHDMIEnable(enable);
    }
    return 1;
}

Return<int32_t> HdmiUtilImpl::acquireLock(int32_t type)
{
    CHECK(mCore != NULL);
    //Mutex::Autolock l(mCore->mLock);
    return mCore->tryAcquireLock(type);
}

Return<int32_t> HdmiUtilImpl::releaseLock(int32_t type)
{
    CHECK(mCore != NULL);
    //Mutex::Autolock l(mCore->mLock);
    mCore->releaseLock(type);
    return 1; //Void();
}

Return<int32_t> HdmiUtilImpl::setHDCPVersion(int32_t version)
{
    CHECK(mCore != NULL);
    mCore->waitHDCPAuthState();
    {
        //Mutex::Autolock l(mCore->mLock);
        mCore->setHDCPVersion(version);
    }
    return 1;
}

Return<int32_t> HdmiUtilImpl::getHDCPVersion()
{
    CHECK(mCore != NULL);
    int32_t hdcp = HDCP_NONE;
    mCore->getHDCPVersion(&hdcp);
    return hdcp;
}

Return<int32_t> HdmiUtilImpl::getConfiguredHDCPVersion()
{
    CHECK(mCore != NULL);
    int32_t hdcp = HDCP_NONE;
    mCore->getConfiguredHDCPVersion(&hdcp);
    return hdcp;
}

void HdmiUtilImpl::constructEDIDSupportList()
{
    CHECK(mCore != NULL);
    // TODO : disable lock to prevent potential dead lock with hidl client side
    //Mutex::Autolock l(mCore->mLock);
    mEDIDConvertVector.clear();

    struct hdmi_support_list *pList = mCore->getEDIDSupportList();

    for(uint32_t i=0;i<pList->tx_support_size;i++) {
        HDMIFormatSupport data;
        CHECK(sizeof(HDMIFormatSupport) == sizeof(hdmi_format_support));
        memcpy(&data,&(pList->tx_support[i]),sizeof(HDMIFormatSupport));
        mEDIDConvertVector.push_back(data);
    }
}

#if 0
Return<int32_t> HdmiUtilImpl::checkTVSystem(
    int32_t inputTvSystem,
    int32_t colorspace_deepcolor)
{
    return 0;
}

Return<void> HdmiUtilImpl::getVideoFormat(getVideoFormat_cb _hidl_cb)
{
    //_hidl_cb(1, mFormats);
    return Void();
}

Return<int32_t> HdmiUtilImpl::getTVSystemForRestored()
{
    return 0;
}

#endif

IHdmiUtil* HIDL_FETCH_IHdmiUtil(const char* /*name*/) {

    ALOGD("%s : E",__FUNCTION__);

    HdmiUtilImpl* hdmiutil = new HdmiUtilImpl();
    if(hdmiutil == nullptr) {
        ALOGE("%s: cannot allocate hdmiutil!", __FUNCTION__);
        return nullptr;
    }
    return hdmiutil;
}

}
}
}
}
}
}
