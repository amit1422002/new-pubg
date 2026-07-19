LOCAL_PATH := $(call my-dir)


ifeq ($(TARGET_ARCH),arm64)
include $(CLEAR_VARS)
LOCAL_MODULE := sock64
LOCAL_SRC_FILES := Main64.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_CFLAGS := -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w
LOCAL_CFLAGS += -fno-rtti -fno-exceptions -fpermissive
LOCAL_CPPFLAGS := -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -Werror -s -std=c++17
LOCAL_CPPFLAGS += -Wno-error=c++11-narrowing -fms-extensions -fno-rtti -fno-exceptions -fpermissive
LOCAL_CPPFLAGS += -DXT_MEM_STEALTH -DXT_MEM_ALLOW_VM_FALLBACK
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all, -llog
LOCAL_ARM_MODE := arm
LOCAL_LDFLAGS += -Wl,--gc-sections
LOCAL_LDFLAGS += -L$(SYSROOT)/usr/lib -lz -llog
include $(BUILD_EXECUTABLE)
endif



