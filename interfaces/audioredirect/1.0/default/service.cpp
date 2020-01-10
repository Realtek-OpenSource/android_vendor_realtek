/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.audioredirect@1.0-service"

#include <vendor/realtek/audioredirect/1.0/IAudioRedirect.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::audioredirect::V1_0::IAudioRedirect;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("Audio Redirect HIDL Service is starting.");
    // The Audio Redirect HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IAudioRedirect>();
}
