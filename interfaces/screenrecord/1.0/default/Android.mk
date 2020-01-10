LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.screenrecord@1.0-impl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += ScreenRecordServiceImpl.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libRtkScreenRecord
LOCAL_SHARED_LIBRARIES += libRTKAllocator
LOCAL_SHARED_LIBRARIES += vendor.realtek.screenrecord@1.0

LOCAL_C_INCLUDES += bootable/recovery/factory
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/libRTKScreenrecord
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/libRTKAllocator/include

#include $(BUILD_SHARED_LIBRARY)

#######################################################

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.screenrecord@1.0-service
LOCAL_INIT_RC := vendor.realtek.screenrecord@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += service.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += vendor.realtek.screenrecord@1.0

#include $(BUILD_EXECUTABLE)
