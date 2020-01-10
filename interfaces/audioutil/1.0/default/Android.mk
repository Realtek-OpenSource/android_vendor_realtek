LOCAL_PATH := $(call my-dir)

ifeq ($(filter kylin hercules thor hank, $(TARGET_BOARD_PLATFORM)),)
$(warning ---------- unsupported platform $(TARGET_BOARD_PLATFORM), no Android.mk under $(LOCAL_PATH) will be included ----------)
else

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.audioutil@1.0-impl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += AudioUtilImpl.cpp

LOCAL_C_INCLUDES += device/realtek/proprietary/libs/hardware/AudioUtil
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/libHDMIRxAudioSource
LOCAL_C_INCLUDES += hardware/realtek/realtek_omx/osal_rtk
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_libs/common/IPC/include
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_libs/common/IPC/generate/include/system
LOCAL_C_INCLUDES += system/core/libion/include
LOCAL_C_INCLUDES += system/core/libsysutils/include/

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
#LOCAL_SHARED_LIBRARIES += libgui
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += android.hidl.memory@1.0
LOCAL_SHARED_LIBRARIES += libhidlmemory

LOCAL_SHARED_LIBRARIES += libAudioUtil_internal
LOCAL_SHARED_LIBRARIES += libHDMIRxAudioSource
LOCAL_SHARED_LIBRARIES += vendor.realtek.audioutil@1.0

ifeq ($(ENABLE_HDMIRX_MULTICHN), YES)
LOCAL_CFLAGS += -DENABLE_HDMIRX_MULTICHN
endif

include $(BUILD_SHARED_LIBRARY)

#######################################################

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.audioutil@1.0-service
LOCAL_INIT_RC := vendor.realtek.audioutil@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += service.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += vendor.realtek.audioutil@1.0

include $(BUILD_EXECUTABLE)

endif
