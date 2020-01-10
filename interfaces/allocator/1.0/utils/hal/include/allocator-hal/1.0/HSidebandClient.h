#include <log/log.h>
#include <SidebandClient.h>
#include <SidebandControl.h>
#include <gui/Surface.h>
#include <media/stagefright/bqhelper/WGraphicBufferProducer.h>
#include <vendor/realtek/allocator/1.0/IHSidebandClient.h>

namespace vendor {
namespace realtek {
namespace allocator {
namespace V1_0 {
namespace hal {
using namespace android;
using TWGraphicBufferProducer = ::android::TWGraphicBufferProducer<
    ::android::hardware::graphics::bufferqueue::V1_0::IGraphicBufferProducer>;

namespace detail {
class HSidebandClient : public IHSidebandClient {
	public:
     HSidebandClient():SB_Client(NULL), SB_Control(NULL), surface(NULL) {}

     virtual ~HSidebandClient() {
         SB_Client.clear();
         SB_Control.clear();
         surface.clear();
     }

     Return<void> getProducer(getProducer_cb hidl_cb) override {
         SB_Client = new SidebandClient();
         if(SB_Client == NULL) {
             ALOGI("[HIDL SIDEBAND] can't get SidebandClient @@@@@");
             hidl_cb(-1, NULL);
             return Void();
         }

         SB_Control = SB_Client->createSurface(String8("Sideband Surface"),100,100,PIXEL_FORMAT_RGB_888);
         
         if(SB_Control == NULL) {
             ALOGI("[HIDL SIDEBAND] can't get SidebandControl @@@@@");
             hidl_cb(-1, NULL);
             return Void();
         }
  
         surface = SB_Control->getSurface();
         SB_Control->setHackSideband(true);
         hidl_cb(0, new TWGraphicBufferProducer(surface->getIGraphicBufferProducer()));
         return Void();
     }
    
     Return<void> getSidebandStream(getSidebandStream_cb hidl_cb) override {
         mNativeHandle = NULL;
         if(SB_Control != NULL) {
             SB_Control->getSidebandStream(&mNativeHandle); 
         }
              
         hidl_cb(0, mNativeHandle);
         return Void(); 
     }

    protected:
      sp<SidebandClient> SB_Client;
      sp<SidebandControl> SB_Control;
      sp<Surface> surface;
      native_handle_t * mNativeHandle = NULL;
};
} //namespace detaul
} //namespace hal
} //namespace V1_0
} //namespace allocator
} //namespace realtek
} //namespace vendor
