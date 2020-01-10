/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "VoutUtilImpl" 

#include <log/log.h>

#include <android/hidl/allocator/1.0/IAllocator.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>

#include "VoutUtilImpl.h"

#include <VoutUtil.h>

using ::android::hidl::allocator::V1_0::IAllocator;
using ::android::hidl::memory::V1_0::IMemory;
using ::android::hardware::hidl_memory;

namespace vendor {
namespace realtek {
namespace voututil {
namespace V1_0 {
namespace implementation {

VoutUtilImpl::VoutUtilImpl()
{
    ALOGV("%s", __FUNCTION__);
}

VoutUtilImpl::~VoutUtilImpl()
{
    ALOGV("%s", __FUNCTION__);
}

Return<int32_t> VoutUtilImpl::setRescaleMode(int32_t videoPlane, int32_t rescaleMode)
{
    ALOGV("%s", __FUNCTION__);
	struct VO_RECTANGLE tmpRect;
    int32_t ret = (int32_t)VoutUtil::instance().SetRescaleMode((VO_VIDEO_PLANE)videoPlane, (VO_RESCALE_MODE)rescaleMode, tmpRect);
    return ret;
}

Return<int32_t> VoutUtilImpl::setFormat3d(int32_t format3d, float fps)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetFormat3d(format3d, fps);
    return ret;
}

Return<int32_t> VoutUtilImpl::setShiftOffset3d(bool exchange_eyeview, bool shift_direction, int32_t delta_offset, int32_t targetPlane)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetShiftOffset3d(exchange_eyeview, shift_direction, delta_offset, (VO_VIDEO_PLANE)targetPlane);
    return ret;
}

Return<int32_t> VoutUtilImpl::set3Dto2D(int32_t srcformat3d)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().set3Dto2D((VO_3D_MODE_TYPE)srcformat3d);
    return ret;
}

Return<int32_t> VoutUtilImpl::set3DSub(int32_t sub)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().set3DSub((u_char) sub);
    return ret;
}

Return<int32_t> VoutUtilImpl::applyVoutDisplayWindowSetup(int32_t r, int32_t g, int32_t b, bool isRGB)
{
    ALOGV("%s", __FUNCTION__);
    VO_COLOR color;
    color.c1 = r;
    color.c2 = g;
    color.c3 = b;
    color.isRGB = isRGB;
    int32_t ret = (int32_t)VoutUtil::instance().ApplyVoutDisplayWindowSetup(color);
    return ret;
}

Return<int32_t> VoutUtilImpl::configureDisplayWindow(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h)
{
    ALOGV("%s", __FUNCTION__);
    VO_COLOR color;
    VO_RECTANGLE rect;
    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    int32_t ret = (int32_t)VoutUtil::instance().ConfigureDisplayWindow((VO_VIDEO_PLANE)plane, rect, rect, color, false);
    return ret;
}

Return<int32_t> VoutUtilImpl::configureDisplayWindowZoomWin(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h, int32_t zx, int32_t zy, int32_t zw, int32_t zh)
{
    ALOGV("%s", __FUNCTION__);
    VO_COLOR color;
    VO_RECTANGLE rect;
    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    VO_RECTANGLE zoom;
    zoom.x = zx;
    zoom.y = zy;
    zoom.width = zw;
    zoom.height = zh;
    int32_t ret = (int32_t)VoutUtil::instance().ConfigureDisplayWindowZoomWin((VO_VIDEO_PLANE)plane, rect, zoom, rect, color, false);
    return ret;
}

Return<int32_t> VoutUtilImpl::configureDisplayWindowDispZoomWin(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h, int32_t zx, int32_t zy, int32_t zw, int32_t zh)
{
    ALOGV("%s", __FUNCTION__);
    VO_COLOR color;
    VO_RECTANGLE rect;
    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    VO_RECTANGLE zoom;
    zoom.x = zx;
    zoom.y = zy;
    zoom.width = zw;
    zoom.height = zh;
    int32_t ret = (int32_t)VoutUtil::instance().ConfigureDisplayWindowDispZoomWin((VO_VIDEO_PLANE)plane, rect, zoom, rect, color, false);
    return ret;
}

Return<int32_t> VoutUtilImpl::zoom(int32_t plane, int32_t x, int32_t y, int32_t w, int32_t h)
{
    ALOGV("%s", __FUNCTION__);
    VO_RECTANGLE rect;
    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    int32_t ret = (int32_t)VoutUtil::instance().Zoom((VO_VIDEO_PLANE)plane, (VO_RECTANGLE)rect);
    return ret;
}

Return<int32_t> VoutUtilImpl::setDisplayRatio(int32_t ratio)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetDisplayRatio(ratio);
    return ret;
}

Return<int32_t> VoutUtilImpl::setDisplayPosition(int32_t x, int32_t y, int32_t w, int32_t h)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetDisplayPosition(x, y, w, h);
    return ret;
}

Return<void> VoutUtilImpl::getHWCV1Rect(getHWCV1Rect_cb _hidl_cb)
{
    ALOGV("%s", __FUNCTION__);
    VO_RECTANGLE rect;
	int32_t ret = VoutUtil::instance().getHWCV1Rect(&rect);
	_hidl_cb(ret, rect.x, rect.y, rect.width, rect.height);
    return Void();
}

Return<int32_t> VoutUtilImpl::setEnhancedSDR(int32_t flag)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetEnhancedSDR((ENUM_SDRFLAG)flag);
    return ret;
}

Return<bool> VoutUtilImpl::isHDRTv()
{
    ALOGV("%s", __FUNCTION__);
    VIDEO_RPC_VOUT_QUERYTVCAP cap;
    VoutUtil::instance().QueryTVCapability(&cap);
    bool ret = (cap.isHDRtv == 1);
    return ret;
}

Return<bool> VoutUtilImpl::isCVBSOn()
{
    ALOGV("%s", __FUNCTION__);
    bool ret = (bool)VoutUtil::instance().isCVBSOn();
    return ret;
}

Return<int32_t> VoutUtilImpl::setCVBSOff(int32_t off)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().setCVBSOff(off);
    return ret;
}

Return<int32_t> VoutUtilImpl::setPeakLuminance(int32_t flag)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetPeakLuminance(flag);
    return ret;
}

Return<int32_t> VoutUtilImpl::setHdrSaturation(int32_t flag)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().SetHdrSaturation(flag);
    return ret;
}

Return<void> VoutUtilImpl::queryDisplayWin(int32_t plane, queryDisplayWin_cb _hidl_cb)
{
    ALOGV("%s", __FUNCTION__);
	VIDEO_RPC_VOUT_QUERY_DISP_WIN_IN in = { (VO_VIDEO_PLANE)plane };
	hidl_memory mem;
    android::sp<IAllocator> allocator = IAllocator::getService("ashmem");
	allocator->allocate(sizeof(VIDEO_RPC_VOUT_QUERY_DISP_WIN_OUT), [&] (bool success, const hidl_memory& memory)
	{
		if(success){
			mem = memory;
        }
	});
    android::sp<IMemory> iMem = mapMemory(mem);
	iMem->update();
	VIDEO_RPC_VOUT_QUERY_DISP_WIN_OUT *out = reinterpret_cast<VIDEO_RPC_VOUT_QUERY_DISP_WIN_OUT*>((void *)iMem->getPointer());
	int ret = VoutUtil::instance().QueryDisplayWin(in, out);
	iMem->commit();
	_hidl_cb(ret, mem);
    return Void();
}

Return<int32_t> VoutUtilImpl::showVideoWindow(int32_t instanceId, int32_t plane)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().ShowVideoWindow((long)instanceId, (VO_VIDEO_PLANE)plane);
    return ret;
}

Return<int32_t> VoutUtilImpl::hideVideoWindow(int32_t instanceId, int32_t plane)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().HideVideoWindow((long)instanceId, (VO_VIDEO_PLANE)plane);
    return ret;
}

Return<int32_t> VoutUtilImpl::setTvResolution(int32_t videoSystem, int32_t videoStandard)
{
    ALOGV("%s", __FUNCTION__);
    int32_t ret = (int32_t)VoutUtil::instance().setTvResolution((ENUM_VIDEO_SYSTEM)videoSystem, (ENUM_VIDEO_STANDARD)videoStandard);
    return ret;
}

Return<int32_t> VoutUtilImpl::applyVideoStandardSetup(bool bUpdateVideo, int32_t vSystem, bool bCheck)
{
    ALOGV("%s", __FUNCTION__);
	VIDEO_RPC_VOUT_CONFIG_TV_SYSTEM tvSystemConfig;
	VIDEO_RPC_VOUT_CONFIG_TV_SYSTEM nandTvSystemConfig;
	memset(&tvSystemConfig, 0 ,sizeof(struct VIDEO_RPC_VOUT_CONFIG_TV_SYSTEM));
	memset(&nandTvSystemConfig, 0 ,sizeof(struct VIDEO_RPC_VOUT_CONFIG_TV_SYSTEM));
    int32_t ret = (int32_t)VoutUtil::instance().ApplyVideoStandardSetup(&tvSystemConfig, &nandTvSystemConfig, bUpdateVideo, (ENUM_VIDEO_SYSTEM)vSystem, bCheck);
    return ret;
}

IVoutUtil* HIDL_FETCH_IVoutUtil(const char* /*name*/) {
    VoutUtilImpl* voututil = new VoutUtilImpl();
    if(voututil == nullptr) {
        ALOGE("%s: cannot allocate voututil!", __FUNCTION__);
        return nullptr;
    }
    return voututil;
}

} // namespace implementation
} // namespace V1_0
} // namespace voututil
} // namespace realtek
} // namespace vendor
