/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.hdmiutil.provider@1.0-service"

#include <vendor/realtek/hdmiutil/provider/1.0/IHdmiUtil.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::hdmiutil::provider::V1_0::IHdmiUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGD("Hdmi Util HIDL Service is starting.");
    // The Hdmi Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");
    return defaultPassthroughServiceImplementation<IHdmiUtil>();
}
