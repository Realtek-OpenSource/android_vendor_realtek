/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_VOUTUTIL_V1_0_VOUTUTIL_H
#define VENDOR_REALTEK_VOUTUTIL_V1_0_VOUTUTIL_H

#include <vendor/realtek/voututil/1.0/IVoutUtil.h>

namespace vendor {
namespace realtek {
namespace voututil {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::voututil::V1_0::IVoutUtil;
using ::android::hardware::Return;
using ::android::hardware::Void;

struct VoutUtilImpl : public IVoutUtil {
    VoutUtilImpl();
    ~VoutUtilImpl();

    // Methods from ::vendor::realtek::voututil::V1_0::IVoutUtil follow.
    Return<int32_t> setRescaleMode(int32_t videoPlane, int32_t rescaleMode) override;
    Return<int32_t> setFormat3d(int32_t format3d, float fps) override;
    Return<int32_t> setShiftOffset3d(bool exchange_eyeview, bool shift_direction, int32_t delta_offset, int32_t targetPlane) override;
    Return<int32_t> set3Dto2D(int32_t srcformat3d) override;
    Return<int32_t> set3DSub(int32_t sub) override;
    Return<int32_t> applyVoutDisplayWindowSetup(int32_t r, int32_t g, int32_t b, bool isRGB) override;
    Return<int32_t> configureDisplayWindow(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h) override;
    Return<int32_t> configureDisplayWindowZoomWin(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h, int32_t zx, int32_t zy, int32_t zw, int32_t zh) override;
    Return<int32_t> configureDisplayWindowDispZoomWin(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h, int32_t zx, int32_t zy, int32_t zw, int32_t zh) override;
    Return<int32_t> zoom(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h) override;
    Return<int32_t> setDisplayRatio(int32_t ratio) override;
    Return<int32_t> setDisplayPosition(int32_t x, int32_t y, int32_t w, int32_t h) override;
    Return<void> getHWCV1Rect(getHWCV1Rect_cb _hidl_cb) override;
    Return<int32_t> setEnhancedSDR(int32_t flag) override;
    Return<bool> isHDRTv() override;
    Return<bool> isCVBSOn() override;
    Return<int32_t> setCVBSOff(int32_t off) override;
    Return<int32_t> setPeakLuminance(int32_t flag) override;
    Return<int32_t> setHdrSaturation(int32_t flag) override;
    Return<void> queryDisplayWin(int32_t plane, queryDisplayWin_cb _hidl_cb) override;
    Return<int32_t> showVideoWindow(int32_t instanceId, int32_t plane) override;
    Return<int32_t> hideVideoWindow(int32_t instanceId, int32_t plane) override;
    Return<int32_t> setTvResolution(int32_t videoSystem, int32_t videoStandard) override;
    Return<int32_t> applyVideoStandardSetup(bool bUpdateVideo, int32_t vSystem, bool bCheck) override;
};

extern "C" IVoutUtil* HIDL_FETCH_IVoutUtil(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace voututil
} // namespace realtek
} // namespace vendor


#endif // VENDOR_REALTEK_VOUTUTIL_V1_0_VOUTUTIL_H
