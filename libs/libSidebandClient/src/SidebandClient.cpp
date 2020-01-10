/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "SidebandClient"

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/Singleton.h>
#include <utils/SortedVector.h>
#include <utils/String8.h>
#include <utils/threads.h>

#include <binder/IMemory.h>
#include <binder/IServiceManager.h>

#include <gui/CpuConsumer.h>
#include <gui/IGraphicBufferProducer.h>
#include <ISideband.h>
#include <ISidebandClient.h>
#include <SidebandClient.h>

#include <SidebandService.h>
#include <gui/LayerState.h>

namespace android {
// ---------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(SidebandService);

SidebandService::SidebandService()
: Singleton<SidebandService>() {
}

void SidebandService::registerServiceName(const char *service_name)
{
    ServiceName.clear();
    if(service_name != NULL)
        ServiceName.append(service_name);

//    if(!ServiceName.isEmpty())
//        ALOGI("[Sideband] Register SidebandService Name %s", ServiceName.c_str());
}

void SidebandService::connectLocked() {
    Mutex::Autolock _l(mLock);
    const String16 name("Sideband");
    if(ServiceName.isEmpty()) {
        ALOGI("[Sideband] getSidebandService from origin method");
        while (getService(name, &mSidebandService) != NO_ERROR) {
            usleep(250000);
        }
    } else {
         const String16 Service(ServiceName.c_str());
//         ALOGI("[Sideband] getSidebandService from app register name : %s", ServiceName.c_str());
         while (getService(Service, &mSidebandService) != NO_ERROR) {
            usleep(250000);
        }
    }
    assert(mSidebandService != NULL);

    // Create the death listener.
    class DeathObserver : public IBinder::DeathRecipient {
        SidebandService& mSidebandService;
        virtual void binderDied(const wp<IBinder>& who) {
            ALOGW("SidebandService remote (sideband) died [%p]",
                  who.unsafe_get());
            mSidebandService.sidebandServiceDied();
        }
     public:
        DeathObserver(SidebandService& mgr) : mSidebandService(mgr) { }
    };

    mDeathObserver = new DeathObserver(*const_cast<SidebandService*>(this));
    IInterface::asBinder(mSidebandService)->linkToDeath(mDeathObserver);
}

/*static*/ sp<ISideband> SidebandService::getSidebandService(const char *service_name) {
    SidebandService& instance = SidebandService::getInstance();
    if(service_name != NULL)
        instance.registerServiceName(service_name);

    instance.connectLocked(); 
    Mutex::Autolock _l(instance.mLock);
    if (instance.mSidebandService == NULL) {
        SidebandService::getInstance().connectLocked();
        assert(instance.mSidebandService != NULL);
        ALOGD("SidebandService reconnected");
    }
    return instance.mSidebandService;
}

void SidebandService::sidebandServiceDied()
{
    Mutex::Autolock _l(mLock);
    mSidebandService = NULL;
    mDeathObserver = NULL;
}

// ---------------------------------------------------------------------------

SidebandClient::SidebandClient()
    : mStatus(NO_INIT),/* TODO , mComposer(Composer::getInstance())*/
      mHackSideband(false)
{
}


SidebandClient::SidebandClient(String8 service_name) 
{
    mServiceName.clear();
    if(!service_name.isEmpty())
        mServiceName.append(service_name);

//    if(!mServiceName.isEmpty())
//        ALOGI("[Sideband] Register SidebandClient Name %s", mServiceName.c_str());
}

void SidebandClient::onFirstRef() {
    if(mServiceName.isEmpty())
        mServiceName.append("Sideband");
    sp<ISideband> sm(SidebandService::getSidebandService(mServiceName.c_str()));

    if (sm != 0) {
        // call Sideband.createConnection, return Client.cpp
        sp<ISidebandClient> conn = sm->createConnection();
        if (conn != 0) {
            mClient = conn;
            mStatus = NO_ERROR;
        }
    }
}

SidebandClient::~SidebandClient() {
    dispose();
}

status_t SidebandClient::initCheck() const {
    return mStatus;
}

sp<IBinder> SidebandClient::connection() const {
    return (mClient != 0) ? IInterface::asBinder(mClient) : 0;
}

status_t SidebandClient::linkToSidebandDeath(
        const sp<IBinder::DeathRecipient>& recipient,
        void* cookie, uint32_t flags) {
    if(mServiceName.isEmpty())
        mServiceName.append("Sideband");

    sp<ISideband> sm(SidebandService::getSidebandService(mServiceName.c_str()));

    return IInterface::asBinder(sm)->linkToDeath(recipient, cookie, flags);
}

void SidebandClient::dispose() {
    // this can be called more than once.
    sp<ISidebandClient> client;
    Mutex::Autolock _lm(mLock);
    if (mClient != 0) {
        client = mClient; // hold ref while lock is held
        mClient.clear();
    }
    mStatus = NO_INIT;
}

sp<SidebandControl> SidebandClient::createSurface(
        const String8& name,
        uint32_t w,
        uint32_t h,
        PixelFormat format,
        uint32_t flags)
{
    sp<SidebandControl> sb;
    if (mStatus == NO_ERROR) {
        sp<IBinder> handle;
        sp<IGraphicBufferProducer> gbp;
        status_t err = mClient->createSurface(name, w, h, format, flags,
                &handle, &gbp);
        ALOGE_IF(err, "SidebandClient::createSurface error %s", strerror(-err));
        if (err == NO_ERROR) {
            sb = new SidebandControl(this, handle, gbp);
        }
    }
    return sb;
}

status_t SidebandClient::destroySurface(const sp<IBinder>& sid) {
    if (mStatus != NO_ERROR)
        return mStatus;
    status_t err = mClient->destroySurface(sid);
    return err;
}

status_t SidebandClient::createSidebandStream(const sp<IBinder>& id, native_handle_t ** handle) {
    uint64_t key;
    if (mStatus != NO_ERROR)
        return mStatus;

    status_t err = mClient->getBinderKey(id, &key);
    if (err!=NO_ERROR)
        return err;

    const int numFds = 0;
    const int numInts = 3;/* uint64_t key */

    native_handle* h = native_handle_create(numFds, numInts);
    if (!h)
        return NO_MEMORY;

    memcpy(h->data + numFds, &key, 2*sizeof(int));
    if(mHackSideband) {
        h->data[numFds + 2] = HACK_SIDEBAND;
    } else {
        h->data[numFds + 2] = SIDEBAND;
    }
    *handle = h;

    return err;
}

status_t SidebandClient::deleteSidebandStream(const sp<IBinder>& /*id*/, native_handle_t * /*handle*/) {
    if (mStatus != NO_ERROR)
        return mStatus;
#if 1
    return NO_ERROR;
#else
    return native_handle_delete(handle);
#endif
}

status_t SidebandClient::createDvdSBStream(native_handle_t **handle, int VOLayer) {
    const int numFds = 0;
    const int numInts = 3;
    native_handle_t *h = native_handle_create(numFds, numInts);
    h->data[numFds] = 0;
    h->data[numFds + 1] = 0;
    if(VOLayer == 2)
        h->data[numFds + 2] = DVDPLAYER_V2;
    else
        h->data[numFds + 2] = DVDPLAYER_V1;

    *handle = h;
    return NO_ERROR;
}
/*
status_t SidebandControl::deleteDvdSBStream(native_handle_t *handle) {
    if(handle != NULL)
        native_handle_delete(handle);
    return NO_ERROR;
}
*/

status_t SidebandClient::checkSidebandStream(const native_handle_t *handle, int ID) {
    if(handle == NULL)
       return BAD_VALUE;

    if((handle->data[handle->numFds + 2] & SIDEBAND_MASK) == (unsigned int)ID) {
        return NO_ERROR;
    }

    if(handle->data[handle->numFds + 2] == ID) {
        return NO_ERROR;
    } else
        return BAD_TYPE;

}

sp<SidebandControl> SidebandClient::binderSurface(native_handle_t * h) {
    uint64_t key;
    sp<SidebandControl> sb;
    sp<IBinder> handle;
    sp<IGraphicBufferProducer> gbp;
    memcpy(&key, h->data + h->numFds, 2*sizeof(int));
    if (mStatus == NO_ERROR) {
        status_t err = mClient->binderSurface(key/*sid*/, &handle, &gbp);
        if (err == NO_ERROR) {
            sb = new SidebandControl(this, handle, gbp);
            if (sb.get() != NULL)
                sb->setSidebandStream(h);
        }
    }
    return sb;
}

status_t SidebandClient::getKey(const sp<IBinder>& id, native_handle_t * h, uint64_t * key) {
    if (mStatus != NO_ERROR)
        return mStatus;
    if (key == NULL)
        return status_t(-1);
    if (h) {
        memcpy(key, h->data + h->numFds, 2*sizeof(int));
        return NO_ERROR;
    }
    return mClient->getBinderKey(id, key);
}

status_t SidebandClient::setCrop(const sp<IBinder>& id, const Rect& crop) {
    if (mStatus == NO_ERROR) {
        return mClient->setCrop(id, crop);
    }
    return mStatus;
}

status_t SidebandClient::setPosition(const sp<IBinder>& id, float x, float y) {
    if (mStatus == NO_ERROR) {
        return mClient->setPosition(id, x, y);
    }
    return mStatus;
}

status_t SidebandClient::setSize(const sp<IBinder>& id, uint32_t w, uint32_t h) {
    if (mStatus == NO_ERROR) {
        return mClient->setSize(id, w, h);
    }
    return mStatus;
}

status_t SidebandClient::setLayer(const sp<IBinder>& id, int32_t z) {
    if (mStatus == NO_ERROR) {
        return mClient->setLayer(id, z);
    }
    return mStatus;
}

status_t SidebandClient::requestHwSync(int32_t* resource) {
    if (mStatus == NO_ERROR) {
        return mClient->requestHwSync(resource);
    }
    return mStatus;
}

status_t SidebandClient::requestHwSyncByFd(int32_t* resource, int audioHwSync) {
    if (mStatus == NO_ERROR) {
        return mClient->requestHwSyncByFd(resource, audioHwSync);
    }
    return mStatus;
}

status_t SidebandClient::getHwSyncByResource(int32_t resource, int32_t* audioHwSync) {
    if (mStatus == NO_ERROR) {
        return mClient->getHwSyncByResource(resource, audioHwSync);
    }
    return mStatus;
}

status_t SidebandClient::destroyHwSyncByResource(int32_t resource) {
    if (mStatus == NO_ERROR) {
        return mClient->destroyHwSyncByResource(resource);
    }
    return mStatus;
}

status_t SidebandClient::setAudioHwSync(const sp<IBinder>& id, int32_t audioHwSync) {
    if (mStatus == NO_ERROR) {
        return mClient->setAudioHwSync(id, audioHwSync);
    }
    return mStatus;
}

status_t SidebandClient::hide(const sp<IBinder>& id) {
    if (mStatus == NO_ERROR) {
        return mClient->setFlags(id,
                layer_state_t::eLayerHidden,
                layer_state_t::eLayerHidden);
    }
    return mStatus;
}

status_t SidebandClient::show(const sp<IBinder>& id) {
    if (mStatus == NO_ERROR) {
        return mClient->setFlags(id,
                0,
                layer_state_t::eLayerHidden);
    }
    return mStatus;
}

status_t SidebandClient::setFlags(const sp<IBinder>& id, uint32_t flags,
        uint32_t mask) {
    if (mStatus == NO_ERROR) {
        return mClient->setFlags(id, flags, mask);
    }
    return mStatus;
}

status_t SidebandClient::setResource(const sp<IBinder>& id, uint64_t resource) {
    if (mStatus == NO_ERROR) {
        return mClient->setResource(id, resource);
    }
    return mStatus;
}

status_t SidebandClient::setAlpha(const sp<IBinder>& id, float alpha) {
    if (mStatus == NO_ERROR) {
        return mClient->setAlpha(id, alpha);
    }
    return mStatus;
}

status_t SidebandClient::getSidebandInfo(const sp<IBinder>& id, String8 &info) {
    if(mStatus == NO_ERROR) {
        return mClient->getSidebandInfo(id, info);
    }
    return mStatus;
}

status_t SidebandClient::setHackSideband(const sp<IBinder>& id, bool use) {
    if(mStatus == NO_ERROR) {
        mHackSideband = use;
        return mClient->setHackSideband(id, use);
    }
    return mStatus;
}
status_t SidebandClient::controlBW(void) {
    if(mStatus == NO_ERROR){
        return mClient->controlBW();
    }
    return mStatus;
}
// ----------------------------------------------------------------------------
}; // namespace android
