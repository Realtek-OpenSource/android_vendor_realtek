/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "ScreenRecordServiceImpl" 

#include <log/log.h>

#include "ScreenRecordServiceImpl.h"

#include "rtk_common.h"
#include "rtk_factory.h"

namespace vendor {
namespace realtek {
namespace screenrecord {
namespace V1_0 {
namespace implementation {

ScreenRecordServiceImpl::ScreenRecordServiceImpl()
    : mRecordService(NULL)
{
    ALOGD("Create ScreenRecordServiceImpl instance");
}

ScreenRecordServiceImpl::~ScreenRecordServiceImpl()
{
    ALOGD("Destroy ScreenRecordServiceImpl instance");
}

Return<bool> ScreenRecordServiceImpl::Initiate(int32_t hal_format, int32_t width, int32_t height, int32_t type, uint32_t usage)
{
    bool ret = false;
    if(mRecordService != NULL) {
        ALOGW("ScreenRecordService has already been initiated, other processes may be using it.");
        ALOGW("Now release it first");
        Release();
    }
    mRecordService = new ScreenRecordService(hal_format, width, height, type, usage);
    if(mRecordService != NULL)
        ret = true;

    return ret;
}

Return<bool> ScreenRecordServiceImpl::Release()
{
    bool ret = true;
    if(mRecordService != NULL) {
        delete mRecordService;
        mRecordService = NULL;
    }
    ALOGI("Release ScreenRecordService %s", ret?"success":"failed");

    return ret;
}

Return<bool> ScreenRecordServiceImpl::ConfigSettings()
{
    bool ret = false;
    if(mRecordService != NULL) {
        ret = mRecordService->ConfigSettings();
    }

    return ret;
}

Return<int32_t> ScreenRecordServiceImpl::CreateBuffer()
{
    int32_t index = -1;
    if(mRecordService != NULL) {
        SRBuffer *buf = mRecordService->CreateBuffer();
        if(buf != NULL)
            index = (int32_t)buf->GetIndex();
    }

    return index;
}

Return<int32_t> ScreenRecordServiceImpl::GetBufferCount()
{
    int32_t count = 0;
    if(mRecordService != NULL) {
        count = (int32_t)mRecordService->GetBufferCount();
    }

    return count;
}

Return<int32_t> ScreenRecordServiceImpl::GetCaptureFps()
{
    int32_t fps = 0;
    if(mRecordService != NULL) {
        fps = (int32_t)mRecordService->GetCaptureFps();
    }

    return fps;
}

Return<int64_t> ScreenRecordServiceImpl::GetTimeStamp(int32_t index)
{
    int64_t time = 0;
    if(mRecordService != NULL) {
        time = (int64_t)mRecordService->GetTimeStamp(index);
    }

    return time;
}

Return<int32_t> ScreenRecordServiceImpl::GetPhyAddr(int32_t index)
{
    int32_t phyaddr = 0;
    if(mRecordService != NULL) {
        phyaddr = (int32_t)mRecordService->GetPhyAddr(index);
    }
    
    return phyaddr;
}

Return<int32_t> ScreenRecordServiceImpl::AcquiredBuffer()
{
    int32_t index = -1;
    if(mRecordService != NULL) {
        SRBuffer* buf = mRecordService->AcquiredBuffer();
        if(buf != NULL)
            index = (int32_t)buf->GetIndex();
    }

    return index;
}

Return<bool> ScreenRecordServiceImpl::FreeBuffer(int32_t index)
{
    bool ret = false;
    if(mRecordService != NULL) {
        ret = mRecordService->FreeBuffer(index);
    }

    return ret;
}

Return<bool> ScreenRecordServiceImpl::FreeAllBuffer()
{
    bool ret = false;
    if(mRecordService != NULL) {
        ret = mRecordService->FreeAllBuffer();
    }

    return ret;
}

IScreenRecordService* HIDL_FETCH_IScreenRecordService(const char* /*name*/) {
    ScreenRecordServiceImpl* screenrecord = new ScreenRecordServiceImpl();
    if(screenrecord == nullptr) {
        ALOGE("%s: cannot allocate screenrecord!", __FUNCTION__);
        return nullptr;
    }

    return screenrecord;
}

} // namespace implementation
} // namespace V1_0
} // namespace screenrecord
} // namespace realtek
} // namespace vendor
