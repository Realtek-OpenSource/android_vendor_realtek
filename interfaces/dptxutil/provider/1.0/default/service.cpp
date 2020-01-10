/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.dptxutil.provider@1.0-service"

#include <vendor/realtek/dptxutil/provider/1.0/IDPTxUtil.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::dptxutil::provider::V1_0::IDPTxUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGD("DPTx Util HIDL Service is starting.");
    // The DPTx Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");
    return defaultPassthroughServiceImplementation<IDPTxUtil>();
}
