/*
 * Copyright (c) 2017 Realtek Semiconductor Corp.
 */
#undef NDEBUG
#define LOG_NDEBUG 0
#define LOG_TAG "RvsdImpl" 

#include <log/log.h>

#include <gui/bufferqueue/1.0/H2BGraphicBufferProducer.h>

#include <rvsd_entry.h>
#include <eventListener.h>

#include "Rvsd.h"
#include "MediaRequest.h"

namespace vendor {
namespace realtek {
namespace rvsd {
namespace V1_0 {
namespace implementation {

using ::android::hardware::graphics::bufferqueue::V1_0::utils::H2BGraphicBufferProducer;

class RvsdEventListener : public eventListener {
public:
    RvsdEventListener(const wp<IRvsdCallback>& callback) { mCb = callback; };
    ~RvsdEventListener() {};
    void notifyListener(int msg, int ext1, int ext2)
    {
        if (mCb != nullptr)
        {
            sp<IRvsdCallback> cb = mCb.promote();
            if (cb != nullptr)
            {
                cb->notify(msg, ext1, ext2);
            }
        }
    };
private:
    wp<IRvsdCallback> mCb;
};

Rvsd::Rvsd()
{
    ALOGV("%s", __func__);
    initVar();
}

Rvsd::~Rvsd()
{
    ALOGV("%s", __func__);
}

void Rvsd::initVar()
{
    int i = 0;


#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    for (i=0;i<MAX_RVSD_INSTANCE_NUM;i++)
    {
        mRvsd[i] = NULL;
        mCb[i] = nullptr;
        mListener[i] = nullptr;
    }
    createdRVSDInstNum = 0;
#else
    mRvsd = NULL;
    mCb = nullptr;
    mListener = nullptr;
#endif
}

#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
int32_t Rvsd::checkInstIdxValid(int32_t instIdx)
{
    Mutex::Autolock l(mInstLock);
    if ((instIdx < 0) || (instIdx >= MAX_RVSD_INSTANCE_NUM))
    {
        ALOGE("%d.checkInstIdxValid().invalid instIdx:%d.\n",__LINE__,instIdx);
        return 1;
    }

    if (mRvsd[instIdx] == NULL)
    {
        ALOGE("%d.checkInstIdxValid().mRvsd[%d] is NULL.\n",__LINE__,instIdx);
        return 1;
    }
    return 0;
}
#endif

Return<void> Rvsd::setLayerOrder(int32_t instIdx, uint32_t order)
{
    ALOGD("%d.setLayerOrder().instIdx:%d.order:%u.\n",__LINE__,instIdx,order);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return Void();
    }
    mRvsd[instIdx]->setLayerOrderValue(order);
#else
    mRvsd->setLayerOrderValue(order);
#endif
    return Void();
}

Return<void> Rvsd::setPlaySpeed(int32_t instIdx, int32_t speed)
{
    ALOGD("%d.setPlaySpeed().instIdx:%d.speed:%d.\n",__LINE__,instIdx,speed);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return Void();
    }
    mRvsd[instIdx]->setPlaySpeedValue(speed);
#else
    mRvsd->setPlaySpeedValue(speed);
#endif
    return Void();
}

Return<void> Rvsd::setVideoSurfaceTexture(int32_t instIdx, const sp<IGraphicBufferProducer>& gbp)
{
    ALOGD("%d.setVideoSurfaceTexture().instIdx:%d.gbp:%p.\n",__LINE__,instIdx,gbp.get());
    sp<::android::IGraphicBufferProducer> iGbp = new H2BGraphicBufferProducer(gbp);
    sp<Surface> surface = new Surface(iGbp);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return Void();
    }
    mRvsd[instIdx]->setVideoSurfaceTexture(surface);
#else
    mRvsd->setVideoSurfaceTexture(surface);
#endif
    return Void();
}

Return<void> Rvsd::getPlaybackPts(int32_t instIdx, getPlaybackPts_cb _hidl_cb)
{
    long long videoPts = 0;
    long long audioPts = 0;
    ALOGD("%d.getPlaybackPts().instIdx:%d.\n",__LINE__,instIdx);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return Void();
    }
    mRvsd[instIdx]->getPlaybackPts(&videoPts, &audioPts);
#else
    mRvsd->getPlaybackPts(&videoPts, &audioPts);
#endif
    ALOGD("%d.getPlaybackPts().instIdx:%d.videoPts:%lld.audioPts:%lld.\n",__LINE__,instIdx,videoPts,audioPts);
    _hidl_cb((int64_t)videoPts, (int64_t)audioPts);
    return Void();
}

Return<void> Rvsd::getVideoBufferFullness(int32_t instIdx, getVideoBufferFullness_cb _hidl_cb)
{
    long size = 0;
    long fullness = 0;
    ALOGD("%d.getVideoBufferFullness().instIdx:%d.\n",__LINE__,instIdx);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return Void();
    }
    mRvsd[instIdx]->getVideoBufferFullness(&size, &fullness);
#else
    mRvsd->getVideoBufferFullness(&size, &fullness);
#endif
    ALOGD("%d.getVideoBufferFullness().instIdx:%d.size:%ld.fullness:%ld.\n",__LINE__,instIdx,size,fullness);
    _hidl_cb((int32_t)size, (int32_t)fullness);
    return Void();
}

Return<int32_t> Rvsd::getVideoPlaneInstance(int32_t instIdx)
{
    int32_t ret = -1;
    ALOGD("%d.getVideoPlaneInstance().instIdx:%d.\n",__LINE__,instIdx);
#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (checkInstIdxValid(instIdx) != 0)
    {
        return ret;
    }
    ret = (int32_t)mRvsd[instIdx]->getVideoPlaneInstance();
#else
    ret = (int32_t)mRvsd->getVideoPlaneInstance();
#endif
    ALOGV("%s() = %d", __func__, ret);
    return ret;
}

Return<::android::sp<IMediaRequest>> Rvsd::getMediaRequestInstance()
{
    Mutex::Autolock l(mMediaReqLock);
    sp<IMediaRequest> mediaReq = new MediaRequest();
    if(mediaReq.get() == nullptr) {
        ALOGE("%s: cannot allocate mediarequest!", __func__);
        return nullptr;
    }
    return mediaReq;
}

Return<int32_t> Rvsd::createRVSDInstance(const sp<IRvsdCallback>& callback, int32_t id)
{
    int i = 0;

    Mutex::Autolock l(mInstLock);
    ALOGD("%d.createRVSDInstance().begin.\n",__LINE__);

#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if (createdRVSDInstNum >= MAX_RVSD_INSTANCE_NUM)
    {
        ALOGE("%d.createRVSDInstance().createdRVSDInstNum:%d >= MAX_RVSD_INSTANCE_NUM:%d.\n",__LINE__,createdRVSDInstNum,MAX_RVSD_INSTANCE_NUM);
        return -1;
    }

    for (i=0;i<MAX_RVSD_INSTANCE_NUM;i++)
    {
        if (mRvsd[i] == NULL)
        {
            mRvsd[i] = new RVSD(id);
            createdRVSDInstNum++;
            mCb[i] = callback;
            mListener[i] = new RvsdEventListener(mCb[i]);
            mRvsd[i]->setListener(mListener[i]);
            mRvsd[i]->start();
            ALOGD("%d.createRVSDInstance().end.create mRvsd[%d]:0x%x.createdRVSDInstNum:%d.\n",__LINE__,i,mRvsd[i],createdRVSDInstNum);
            return i;
        }
    }

    ALOGE("%d.createRVSDInstance().unexpected error.createdRVSDInstNum:%d.\n",__LINE__,createdRVSDInstNum);
#else
    if (mRvsd != NULL)
    {
        ALOGE("%d.createRVSDInstance().mRVSD:0x%x existed.\n",__LINE__,mRvsd);
        return -1;
    }
    mRvsd = new RVSD;
    mCb = callback;
    mListener = new RvsdEventListener(mCb);
    mRvsd->setListener(mListener);
    mRvsd->start();
    return 0;
#endif
    return -1;
}

Return<int32_t> Rvsd::destroyRVSDInstance(int32_t instIdx)
{
    Mutex::Autolock l(mInstLock);
    ALOGD("%d.destroyRVSDInstance().begin.instIdx:%d.\n",__LINE__,instIdx);

#if defined(ENABLE_MULTI_RVSD_IN_HIDL)
    if ((instIdx < 0) || (instIdx >= MAX_RVSD_INSTANCE_NUM))
    {
        ALOGE("%d.destroyRVSDInstance().invalid instIdx:%d.\n",__LINE__,instIdx);
        return 1;
    }

    if (mRvsd[instIdx] == NULL)
    {
        ALOGE("%d.destroyRVSDInstance().mRvsd[%d] is NULL.\n",__LINE__,instIdx);
        return 1;
    }

    mRvsd[instIdx]->stop();
    mCb[instIdx] = nullptr;
    mListener[instIdx] = nullptr;
    delete mRvsd[instIdx];
    createdRVSDInstNum--;
    mRvsd[instIdx] = NULL;
#else
    mRvsd->stop();
    mListener = NULL;
    delete mRvsd;
    mRvsd = NULL;
#endif

    ALOGD("%d.destroyRVSDInstance().end.instIdx:%d.\n",__LINE__,instIdx);
    return 0;
}

IRvsd* HIDL_FETCH_IRvsd(const char* /*name*/) {
    ALOGD("%d.HIDL_FETCH_IRvsd.bf new Rvsd().\n",__LINE__);
    Rvsd* rvsd = new Rvsd();
    ALOGD("%d.HIDL_FETCH_IRvsd.af new Rvsd().rvsd:0x%x.\n",__LINE__,rvsd);
    if(rvsd == nullptr) {
        ALOGE("%s: cannot allocate rvsd!", __func__);
        return nullptr;
    }
    return rvsd;
}

} // namespace implementation
} // namespace V1_0
} // namespace rvsd
} // namespace realtek
} // namespace vendor
