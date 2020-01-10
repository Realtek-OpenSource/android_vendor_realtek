/*
 * Copyright (c) 2018 Realtek Semiconductor Corp.
 */
//#define LOG_NDEBUG 0
#define LOG_TAG "MediaRequest"

#include <log/log.h>
#include "MediaRequest.h"

namespace vendor {
namespace realtek {
namespace rvsd {
namespace V1_0 {
namespace implementation {

MediaRequest::MediaRequest()
{
    ALOGV("%s", __func__);
}

MediaRequest::~MediaRequest()
{
    ALOGV("%s", __func__);
}

Return<int32_t> MediaRequest::createConnection()
{
    ALOGV("%s", __func__);
    if(!mIpc.isValid()){
        mIpc.createConnection();
    }
    return mIpc.getSocket();
}

Return<void> MediaRequest::closeConnection()
{
    ALOGV("%s", __func__);
    if(mIpc.isValid()){
        mIpc.closeConnection();
    }
    return Void();
}

Return<void> MediaRequest::sendCommand(int32_t cmd, const ::android::hardware::hidl_vec<uint8_t>& data, sendCommand_cb _hidl_cb)
{
    ALOGV("%s cmd:%d sz:%d", __func__, cmd, data.size());
    const hidl_vec<uint8_t>& _data = data;
    int cmdSeqNum = -1;
    RT_CMD_ID cmdId = (RT_CMD_ID)cmd;
    int ret = mIpc.sendCommand(cmdId, (char *)_data.data(), _data.size(), cmdSeqNum);
    ALOGV("%s cmd:%d ret:%d seq:%d", __func__, cmd, ret, cmdSeqNum);
    _hidl_cb(ret, cmdSeqNum);
    return Void();
}

Return<void> MediaRequest::getResult(bool nowait, getResult_cb _hidl_cb)
{
    ALOGV("%s nowait:%d", __func__, nowait);
    char *readBuf = NULL;
    int readSize = 0;
    int cmdSeqNum = -1;
    RT_CMD_ID cmdId;
    int ret = mIpc.getResult(cmdId, readBuf, readSize, cmdSeqNum, &nowait);
    hidl_vec<uint8_t> result;
    result.setToExternal(reinterpret_cast<uint8_t *>(readBuf), readSize);
    ALOGV("%s cmd:%d ret:%d seq:%d sz:%d", __func__, cmdId, ret, cmdSeqNum, result.size());
    _hidl_cb(ret, cmdId, cmdSeqNum, result);
    return Void();
}

Return<bool> MediaRequest::isValid()
{
    return mIpc.isValid();
}

Return<int32_t> MediaRequest::getSocket()
{
    return mIpc.getSocket();
}

} // namespace implementation
} // namespace V1_0
} // namespace rvsd
} // namespace realtek
} // namespace vendor
