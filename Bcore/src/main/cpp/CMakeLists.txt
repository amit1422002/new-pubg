# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
include_directories(JniHook)
include_directories(./)
include_directories(./esp_protocol)

aux_source_directory(./ SRC1)
aux_source_directory(Utils SRC2)
aux_source_directory(Hook SRC3)
aux_source_directory(JniHook SRC4)
add_compile_options(-w)
add_library( # Sets the name of the library.
        artpalette
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        BoxCore.cpp
        FarlightEspSocketServer.cpp
        DeltaForceEspSocketServer.cpp
        hidden_api.cpp
        SandHook/ElfImg.cpp
        Hook/DexFileHook.cpp
        Hook/SystemPropertiesHook.cpp
        Hook/LogScrubHook.cpp
        AnogsThunderHook.cpp
        ${SRC1}
        ${SRC2}
        ${SRC3}
        ${SRC4}
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        artpalette
        ${log-lib}
        z
        dl
        android)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_link_libraries(artpalette ${CMAKE_SOURCE_DIR}/Dobby/arm64-v8a/libdobby.a)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_link_libraries(artpalette ${CMAKE_SOURCE_DIR}/Dobby/armeabi-v7a/libdobby.a)
endif()
