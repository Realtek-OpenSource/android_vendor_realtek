LOCAL_PATH := $(call my-dir)

#######################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := vendor.realtek.hdmiutil.provider@1.0-impl.so
LOCAL_MODULE_TAGS:= optional eng
LOCAL_MODULE_CLASS := VENDOR_SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_SHARED_LIBRARIES)
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE := vendor.realtek.hdmiutil.provider@1.0-impl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

include $(BUILD_PREBUILT)
#######################################################

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.hdmiutil.provider@1.0-service
LOCAL_INIT_RC := vendor.realtek.hdmiutil.provider@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += service.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += vendor.realtek.hdmiutil.provider@1.0

include $(BUILD_EXECUTABLE)
