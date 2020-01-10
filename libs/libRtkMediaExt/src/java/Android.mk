LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := RtkMediaExt
#LOCAL_VENDOR_MODULE := true
#LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_SRC_FILES := $(call all-java-files-under, com/rtk)
include $(BUILD_STATIC_JAVA_LIBRARY)
