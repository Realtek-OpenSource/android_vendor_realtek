LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.dptxutil.provider@1.0-impl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += DPTxUtilImpl.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libhardware_legacy
LOCAL_SHARED_LIBRARIES += libhardware
LOCAL_SHARED_LIBRARIES += libsysutils
LOCAL_SHARED_LIBRARIES += libHDMILib
LOCAL_SHARED_LIBRARIES += libDPTXServiceCore
#LOCAL_SHARED_LIBRARIES += libRTKHWMBinderapi
#LOCAL_SHARED_LIBRARIES += libMCPControl
#LOCAL_SHARED_LIBRARIES += librtk_ion
#LOCAL_SHARED_LIBRARIES += libion

LOCAL_SHARED_LIBRARIES += vendor.realtek.dptxutil.provider@1.0

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += hardware/libhardware/include
LOCAL_C_INCLUDES += hardware/realtek
LOCAL_C_INCLUDES += hardware/realtek/dptx
# legacy bad workaround.
LOCAL_C_INCLUDES += hardware/realtek/hdmitx
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_libs/common
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_libs/common/IPC/generate/include/system
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_libs/common/IPC/include
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/DPTxLib
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/HDMILib
LOCAL_C_INCLUDES += device/realtek/proprietary/libs/DPTXServiceCore
#LOCAL_C_INCLUDES += device/realtek/proprietary/libs/Include
#LOCAL_C_INCLUDES += device/realtek/proprietary/libs/rtk_ion
#LOCAL_C_INCLUDES += device/realtek/proprietary/libs

include $(BUILD_SHARED_LIBRARY)

#######################################################

include $(CLEAR_VARS)

LOCAL_MODULE := vendor.realtek.dptxutil.provider@1.0-service
LOCAL_INIT_RC := vendor.realtek.dptxutil.provider@1.0-service.rc
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES += service.cpp

LOCAL_SHARED_LIBRARIES += libhidlbase
LOCAL_SHARED_LIBRARIES += libhidltransport
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += vendor.realtek.dptxutil.provider@1.0

include $(BUILD_EXECUTABLE)
