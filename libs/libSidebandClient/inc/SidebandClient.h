#ifndef REALTEK_GUI_SIDEBAND_CLIENT_H
#define REALTEK_GUI_SIDEBAND_CLIENT_H

#include <stdint.h>
#include <sys/types.h>

#include <binder/IBinder.h>
#include <binder/IMemory.h>

#include <utils/RefBase.h>
#include <utils/Singleton.h>
#include <utils/SortedVector.h>
#include <utils/threads.h>

#include <ui/FrameStats.h>
#include <ui/PixelFormat.h>

#include <SidebandControl.h>
#include <cutils/native_handle.h>

namespace android {

// ---------------------------------------------------------------------------

class Composer;
class ISidebandClient;
class IGraphicBufferProducer;
class Region;

// ---------------------------------------------------------------------------

class SidebandClient : public RefBase
{
    // TODO friend class Composer;
public:
    enum {
        DVDPLAYER_V1 = 0x534232,
        DVDPLAYER_V2 = 0x534233,
        SIDEBAND = 0x53423100,
        HACK_SIDEBAND = 0x53423101,
        SIDEBAND_MASK = 0xffffff00,
    };
                SidebandClient();
                SidebandClient(String8 service_name);
    virtual     ~SidebandClient();

    // Always make sure we could initialize
    status_t    initCheck() const;

    // Return the connection of this client
    sp<IBinder> connection() const;

    // Forcibly remove connection before all references have gone away.
    void        dispose();

    // TODO what about function name? callback when the composer is dies
    status_t linkToSidebandDeath(const sp<IBinder::DeathRecipient>& recipient,
            void* cookie = NULL, uint32_t flags = 0);

    // ------------------------------------------------------------------------
    // sideband creation / destruction

    //! Create a sideband
    sp<SidebandControl> createSurface(
            const String8& name,// name of the sideband
            uint32_t w,         // width in pixel
            uint32_t h,         // height in pixel
            PixelFormat format, // pixel-format desired
            uint32_t flags = 0  // usage flags
    );

    status_t    destroySurface(const sp<IBinder>& id);

    status_t    createSidebandStream(const sp<IBinder>& id, native_handle_t ** handle);
    status_t    deleteSidebandStream(const sp<IBinder>& id, native_handle_t * handle);
    static status_t    createDvdSBStream(native_handle_t **handle, int VOLayer = 1);
    static status_t    checkSidebandStream(const native_handle_t *handle, int id);
//   status_t    deleteDvdSBStream(native_handle_t *handle);

    sp<SidebandControl>    binderSurface(native_handle_t * handle);

    //sp<SidebandControl>    binderSurface(uint64_t key /*const sp <IBinder>& id*/);


    status_t    hide(const sp<IBinder>& id);
    status_t    show(const sp<IBinder>& id);
    status_t    setFlags(const sp<IBinder>& id, uint32_t flags, uint32_t mask);
    status_t    setLayer(const sp<IBinder>& id, int32_t layer);
    status_t    requestHwSync(int32_t* resource);
    status_t    requestHwSyncByFd(int32_t* resource, int audioHwSync);
    status_t    getHwSyncByResource(int32_t resource, int32_t* audioHwSync);
    status_t    destroyHwSyncByResource(int32_t resource);
    status_t    setAudioHwSync(const sp<IBinder>& id, int32_t audioHwSync);
    status_t    setAlpha(const sp<IBinder>& id, float alpha=1.0f);
    status_t    setPosition(const sp<IBinder>& id, float x, float y);
    status_t    setSize(const sp<IBinder>& id, uint32_t w, uint32_t h);
    status_t    setCrop(const sp<IBinder>& id, const Rect& crop);

    status_t    setResource(const sp<IBinder>& id, uint64_t resource);
    status_t    getKey(const sp<IBinder>& id, native_handle_t * handle, uint64_t * key);
    status_t    getSidebandInfo(const sp<IBinder>& id, String8 &Info);
    status_t    setHackSideband(const sp<IBinder>& id, bool use);
    status_t    controlBW(void);
private:
    virtual void onFirstRef();
    mutable     Mutex                       mLock;
    status_t                                mStatus;
    sp<ISidebandClient>                     mClient;
    String8                                 mServiceName;
    bool                                    mHackSideband;
    // TODO Composer&                       mComposer;
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // REALTEK_GUI_SIDEBAND_CLIENT_H
