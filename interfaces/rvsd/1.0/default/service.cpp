/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#undef NDEBUG
#define LOG_NDEBUG 0
#define LOG_TAG "vendor.realtek.rvsd@1.0-service"
#include <log/log.h>

#include <vendor/realtek/rvsd/1.0/IRvsd.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::rvsd::V1_0::IRvsd;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGD("Rvsd Service is starting.");
    // The RVSD HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IRvsd>(8);
}
