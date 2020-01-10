LOCAL_PATH := $(call my-dir)

##################################################

include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_STATIC_JAVA_LIBRARIES := android-support-test

LOCAL_CERTIFICATE := platform

LOCAL_PACKAGE_NAME := RtkTests
#LOCAL_INSTRUMENTATION_FOR := rtktest

LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_PACKAGE)

# Build all sub-directories
include $(call all-makefiles-under,$(LOCAL_PATH))
