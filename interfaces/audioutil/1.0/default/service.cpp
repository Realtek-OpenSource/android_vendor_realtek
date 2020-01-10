/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.audioutil@1.0-service"

#include <vendor/realtek/audioutil/1.0/IAudioUtil.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::audioutil::V1_0::IAudioUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("Audio Util HIDL Service is starting.");
    // The Audio Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IAudioUtil>();
}
