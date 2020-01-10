/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.factoryutil@1.0-service"

#include <vendor/realtek/factoryutil/1.0/IFactoryUtil.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>

using vendor::realtek::factoryutil::V1_0::IFactoryUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("Factory Util HIDL Service is starting.");
    // The Factory Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");

    return defaultPassthroughServiceImplementation<IFactoryUtil>();
}
