/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_RVSD_V1_0_RVSD_H
#define VENDOR_REALTEK_RVSD_V1_0_RVSD_H

#include <utils/Mutex.h>
#include <vendor/realtek/rvsd/1.0/IRvsd.h>
#include <vendor/realtek/rvsd/1.0/IRvsdCallback.h>
#include <vendor/realtek/rvsd/1.0/IMediaRequest.h>

#define ENABLE_MULTI_RVSD_IN_HIDL
#define MAX_RVSD_INSTANCE_NUM   4

namespace vendor {
namespace realtek {
namespace rvsd {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::rvsd::V1_0::IRvsd;
using ::vendor::realtek::rvsd::V1_0::IRvsdCallback;
using ::vendor::realtek::rvsd::V1_0::IMediaRequest;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::graphics::bufferqueue::V1_0::IGraphicBufferProducer;

class RvsdEventListener;

struct Rvsd : public IRvsd {
    Rvsd();
    ~Rvsd();

    // Methods from ::vendor::realtek::rvsd::V1_0::IRvsd follow.
    Return<void> setLayerOrder(int32_t instIdx, uint32_t order) override;
    Return<void> setPlaySpeed(int32_t instIdx, int32_t speed) override;
    Return<void> setVideoSurfaceTexture(int32_t instIdx, const sp<IGraphicBufferProducer>& gbp) override;
    Return<void> getPlaybackPts(int32_t instIdx, getPlaybackPts_cb _hidl_cb) override;
    Return<void> getVideoBufferFullness(int32_t instIdx, getVideoBufferFullness_cb _hidl_cb) override;
    Return<int32_t> getVideoPlaneInstance(int32_t instIdx) override;
    Return<::android::sp<IMediaRequest>> getMediaRequestInstance() override;
    Return<int32_t> createRVSDInstance(const sp<IRvsdCallback>& callback, int32_t id) override;
    Return<int32_t> destroyRVSDInstance(int32_t instIdx) override;

    void initVar();
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    int32_t checkInstIdxValid(int32_t instIdx);
#endif

private:
    Mutex mInstLock;
    Mutex mMediaReqLock;
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    RVSD *mRvsd[MAX_RVSD_INSTANCE_NUM];
    sp<IRvsdCallback> mCb[MAX_RVSD_INSTANCE_NUM];
    sp<RvsdEventListener> mListener[MAX_RVSD_INSTANCE_NUM];
    int32_t createdRVSDInstNum;
#else
    RVSD *mRvsd;
    sp<IRvsdCallback> mCb;
    sp<RvsdEventListener> mListener;
#endif
};

extern "C" IRvsd* HIDL_FETCH_IRvsd(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace rvsd
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_RVSD_V1_0_RVSD_H
