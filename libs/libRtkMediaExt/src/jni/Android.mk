LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := librtk-mediaplayer_jni.so
LOCAL_MODULE_TAGS:= optional eng
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE := librtk-mediaplayer_jni

include $(BUILD_PREBUILT)

