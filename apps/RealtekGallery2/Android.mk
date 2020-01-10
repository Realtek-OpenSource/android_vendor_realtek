LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

$(info $(shell ($(LOCAL_PATH)/multilan.sh $(LOCAL_PATH) )))

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += com.android.gallery3d.common2_RTK
LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit
LOCAL_STATIC_JAVA_LIBRARIES += mp4parser
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v8-renderscript
LOCAL_STATIC_JAVA_LIBRARIES += libjcifs_tsb
LOCAL_STATIC_JAVA_LIBRARIES += RtkDisplayDeviceCtrl
LOCAL_STATIC_ANDROID_LIBRARIES := RtkMediaExt

LOCAL_RENDERSCRIPT_TARGET_API := 18
LOCAL_RENDERSCRIPT_COMPATIBILITY := 18
LOCAL_RENDERSCRIPT_FLAGS := -rs-package-name=android.support.v8.renderscript

# Keep track of previously compiled RS files too (from bundled GalleryGoogle).
prev_compiled_rs_files := $(call all-renderscript-files-under, src)

# We already have these files from GalleryGoogle, so don't install them.
LOCAL_RENDERSCRIPT_SKIP_INSTALL := $(prev_compiled_rs_files)

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(prev_compiled_rs_files)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd)
LOCAL_SRC_FILES += src/com/android/music/IMediaPlaybackService.aidl

LOCAL_CERTIFICATE := platform

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_PACKAGE_NAME := RealtekGallery2
LOCAL_PROGUARD_ENABLED:= disabled

#LOCAL_VENDOR_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Gallery Gallery3D GalleryNew3D Gallery2

#LOCAL_SDK_VERSION := current
LOCAL_PRIVATE_PLATFORM_APIS := true

# If this is an unbundled build (to install seprately) then include
# the libraries in the APK, otherwise just put them in /system/lib and
# leave them out of the APK
ifneq (,$(TARGET_BUILD_APPS))
  LOCAL_JNI_SHARED_LIBRARIES := libjni_eglfence_RTK libjni_filtershow_filters_RTK librsjni libjni_jpegstream_RTK librtk-mediaplayer_jni librtk-display_ctrl_jni
else
  LOCAL_REQUIRED_MODULES := libjni_eglfence_RTK libjni_filtershow_filters_RTK libjni_jpegstream_RTK librtk-mediaplayer_jni librtk-display_ctrl_jni
endif

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjcifs_tsb:libs/jcifs-1.3.18.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under, jni)

ifeq ($(strip $(LOCAL_PACKAGE_OVERRIDES)),)

# Use the following include to make gallery test apk
include $(call all-makefiles-under, $(LOCAL_PATH))

endif
