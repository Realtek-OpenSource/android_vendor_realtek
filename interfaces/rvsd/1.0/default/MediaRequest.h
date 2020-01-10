/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */
#ifndef VENDOR_REALTEK_RVSD_V1_0_MEDIAREQUEST_H
#define VENDOR_REALTEK_RVSD_V1_0_MEDIAREQUEST_H

#include <vendor/realtek/rvsd/1.0/IMediaRequest.h>
#include "RT_IPC/RT_IPC.h"

namespace vendor {
namespace realtek {
namespace rvsd {
namespace V1_0 {
namespace implementation {

using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::hidl_vec;
using ::vendor::realtek::rvsd::V1_0::IMediaRequest;

struct MediaRequest : public IMediaRequest {
    MediaRequest();
    ~MediaRequest();

    // Methods from ::vendor::realtek::rvsd::V1_0::IMediaRequest follow.
    Return<int32_t> createConnection() override;
    Return<void> closeConnection() override;
    Return<void> sendCommand(int32_t cmd, const ::android::hardware::hidl_vec<uint8_t>& data, sendCommand_cb _hidl_cb) override;
    Return<void> getResult(bool nowait, getResult_cb _hidl_cb) override;
    Return<bool> isValid() override;
    Return<int32_t> getSocket() override;

private:
    RT_IPC mIpc;
};

} // namespace implementation
} // namespace V1_0
} // namespace rvsd
} // namespace realtek
} // namespace vendor

#endif //VENDOR_REALTEK_RVSD_V1_0_MEDIAREQUEST_H
