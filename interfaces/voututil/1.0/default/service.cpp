/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */

#define LOG_TAG "vendor.realtek.voututil@1.0-service"

#include <vendor/realtek/voututil/1.0/IVoutUtil.h>
#include <hidl/LegacySupport.h>

#include <binder/ProcessState.h>
#include <VoutUtil.h>
#include <cutils/properties.h>
#define PROPERTY_VALUE_MAX 92
using vendor::realtek::voututil::V1_0::IVoutUtil;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    ALOGI("Vout Util HIDL Service is starting.");
    // The Vout Util HAL may communicate to other vendor components via /dev/vndbinder
    android::ProcessState::initWithDriver("/dev/vndbinder");
    char value[PROPERTY_VALUE_MAX];
    bool isZoom = false;
    property_get("persist.vendor.rtk.vout.zoom",value, "0");
    if(value[0]=='1')
        isZoom=true;
    else
        isZoom=false;

	if(isZoom) {
        int zoom_ratio = 95;
        if(property_get("persist.vendor.rtk.vout.zoom.ratio", value, "95") > 0) {
            zoom_ratio = atoi(value);
        }
        ALOGI("Vout Util HIDL Service SetDisplayRatio 95");
        VoutUtil::instance().SetDisplayRatio(zoom_ratio);
	}
    return defaultPassthroughServiceImplementation<IVoutUtil>();
}
