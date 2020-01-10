/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.audioredirectutil@1.0-service"

#include <hidl/LegacySupport.h>
#include <binder/ProcessState.h>
#include <vendor/realtek/audioredirectutil/1.0/IAudioRedirectUtil.h>

using vendor::realtek::audioredirectutil::V1_0::IAudioRedirectUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("AudioRedirect Util HIDL Service is starting.");
    // The Factory Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IAudioRedirectUtil>();
}
