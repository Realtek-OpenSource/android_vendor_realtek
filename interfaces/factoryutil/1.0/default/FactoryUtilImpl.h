/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_FACTORYUTIL_V1_0_H
#define VENDOR_REALTEK_FACTORYUTIL_V1_0_H

#include <vendor/realtek/factoryutil/1.0/IFactoryUtil.h>

namespace vendor {
namespace realtek {
namespace factoryutil {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::factoryutil::V1_0::IFactoryUtil;
using ::android::hardware::Return;
using ::android::hardware::Void;

struct FactoryUtilImpl : public IFactoryUtil {
    FactoryUtilImpl();
    ~FactoryUtilImpl();

    Return<int32_t> factorySave() override;
    Return<int32_t> factoryLoad() override;
};

extern "C" IFactoryUtil* HIDL_FETCH_IFactoryUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace factoryutil
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_FACTORYUTIL_V1_0_H
