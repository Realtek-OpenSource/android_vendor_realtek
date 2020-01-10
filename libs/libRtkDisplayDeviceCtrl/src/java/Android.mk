LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := RtkDisplayDeviceCtrl
LOCAL_VENDOR_MODULE := true
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
#LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_SRC_FILES := $(call all-java-files-under, com/realtek)
include $(BUILD_STATIC_JAVA_LIBRARY)
