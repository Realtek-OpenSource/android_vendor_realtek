/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "FactoryUtilImpl" 

#include <log/log.h>

#include "FactoryUtilImpl.h"

#include "rtk_common.h"
#include "rtk_factory.h"

namespace vendor {
namespace realtek {
namespace factoryutil {
namespace V1_0 {
namespace implementation {

void factory_log_callback(char *fmt) {
    ALOGD("%s",fmt);
}

FactoryUtilImpl::FactoryUtilImpl()
{
    ALOGD("FactoryUtilImpl()");
    set_android_log_callback(&factory_log_callback);
}

FactoryUtilImpl::~FactoryUtilImpl()
{
    ALOGD("~FactoryUtilImpl()");
}

Return<int32_t> FactoryUtilImpl::factorySave()
{
    ALOGD("%s : E",__FUNCTION__);
    if(rtk_file_lock()) return -1;
    factory_flush(0, 0);
    if(rtk_file_unlock()) return -1;

    return 0;
}

Return<int32_t> FactoryUtilImpl::factoryLoad()
{
    ALOGD("%s : E",__FUNCTION__);
    if(rtk_file_lock()) return -1;
    ALOGD("%s : %d",__FUNCTION__,__LINE__);
    factory_load();
    if(rtk_file_unlock()) return -1;

    return 0;
}

IFactoryUtil* HIDL_FETCH_IFactoryUtil(const char* /*name*/) {
    FactoryUtilImpl* factoryutil = new FactoryUtilImpl();
    if(factoryutil == nullptr) {
        ALOGE("%s: cannot allocate factoryutil!", __FUNCTION__);
        return nullptr;
    }
    return factoryutil;
}

} // namespace implementation
} // namespace V1_0
} // namespace factoryutil
} // namespace realtek
} // namespace vendor
