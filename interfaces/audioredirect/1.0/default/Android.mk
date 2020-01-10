LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.audioredirect@1.0-impl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += AudioRedirectImpl.cpp

LOCAL_C_INCLUDES += device/realtek/proprietary/libs/libaudioRedirect

LOCAL_SHARED_LIBRARIES += libRtkAudioRedirect
LOCAL_SHARED_LIBRARIES += vendor.realtek.audioredirect@1.0

LOCAL_SHARED_LIBRARIES += libstagefright_foundation
LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libgui
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog

#include $(BUILD_SHARED_LIBRARY)

#######################################################

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.audioredirect@1.0-service
LOCAL_INIT_RC := vendor.realtek.audioredirect@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += service.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += vendor.realtek.audioredirect@1.0

#include $(BUILD_EXECUTABLE)
