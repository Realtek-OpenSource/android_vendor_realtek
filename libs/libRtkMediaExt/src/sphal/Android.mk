LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := librtk-mediaplayer.so
LOCAL_MODULE_TAGS:= optional eng
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE := librtk-mediaplayer

include $(BUILD_PREBUILT)

