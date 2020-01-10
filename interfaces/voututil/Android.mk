LOCAL_PATH := $(call my-dir)

ifeq ($(filter kylin hercules thor hank, $(TARGET_BOARD_PLATFORM)),)
$(warning ---------- unsupported platform $(TARGET_BOARD_PLATFORM), no Android.mk under $(LOCAL_PATH) will be included ----------)
else
include $(LOCAL_PATH)/1.0/default/Android.mk
endif
