/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */

#ifndef VENDOR_REALTEK_SCREENRECORD_V1_0_H
#define VENDOR_REALTEK_SCREENRECORD_V1_0_H

#include <vendor/realtek/screenrecord/1.0/IScreenRecordService.h>
#include <include/RTKScreenRecord.h>

namespace vendor {
namespace realtek {
namespace screenrecord {
namespace V1_0 {
namespace implementation {

using ::vendor::realtek::screenrecord::V1_0::IScreenRecordService;
using ::android::hardware::Return;
using ::android::hardware::Void;

struct ScreenRecordServiceImpl : public IScreenRecordService {
    ScreenRecordServiceImpl();
    ~ScreenRecordServiceImpl();

    Return<bool> Initiate(int32_t hal_format, int32_t width, int32_t height, int32_t type, uint32_t usage) override;
    Return<bool> Release() override;
    Return<bool> ConfigSettings() override;
    Return<int32_t> CreateBuffer() override;
    Return<int32_t> GetBufferCount() override;
    Return<int32_t> GetCaptureFps() override;
    Return<int64_t> GetTimeStamp(int32_t index) override;
    Return<int32_t> GetPhyAddr(int32_t index) override;
    Return<int32_t> AcquiredBuffer() override;
    Return<bool> FreeBuffer(int32_t phyaddr) override;
    Return<bool> FreeAllBuffer() override;

private:
    ScreenRecordService* mRecordService;
};

extern "C" IScreenRecordService* HIDL_FETCH_IScreenRecordService(const char* name);

} // namespace implementation
} // namespace V1_0
} // namespace screenrecord
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_SCREENRECORD_V1_0_H
