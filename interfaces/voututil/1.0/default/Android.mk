LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := vendor.realtek.voututil@1.0-impl.so
LOCAL_MODULE_TAGS:= optional eng
LOCAL_MODULE_CLASS := VENDOR_SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_SHARED_LIBRARIES)
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE := vendor.realtek.voututil@1.0-impl
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_PREBUILT)

#######################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  vendor.realtek.voututil@1.0-service
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional eng
LOCAL_MODULE :=  vendor.realtek.voututil@1.0-service
LOCAL_INIT_RC := vendor.realtek.voututil@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

include $(BUILD_PREBUILT)

######################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := vendor.realtek.voututil@1.0.so
LOCAL_MODULE_TAGS:= optional eng
LOCAL_MODULE_CLASS := VENDOR_SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_SHARED_LIBRARIES)
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE := vendor.realtek.voututil@1.0
#LOCAL_MODULE_RELATIVE_PATH := hw
#LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_PREBUILT)

