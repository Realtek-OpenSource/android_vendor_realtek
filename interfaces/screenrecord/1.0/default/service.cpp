/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.screenrecord@1.0-service"

#include <vendor/realtek/screenrecord/1.0/IScreenRecordService.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::screenrecord::V1_0::IScreenRecordService;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("ScreenRecordService HIDL Service is starting.");
    // The ScreenRecordService HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IScreenRecordService>();
}
