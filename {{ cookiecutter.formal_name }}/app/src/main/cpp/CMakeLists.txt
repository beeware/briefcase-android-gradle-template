# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.0)

# If you need to link against the CPython library,
# uncomment the following block.
# =======================================================
# Set up the Python library
#set(
#    APP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../..
#)
#include_directories(
#    ${APP_DIR}/include/python3.7m
#)
#add_library(
#    python-lib
#    SHARED IMPORTED
#)
#set_target_properties(
#    python-lib
#   PROPERTIES IMPORTED_LOCATION
#    ${APP_DIR}/libs/${ANDROID_ABI}/libpython3.7m.so
#)
# =======================================================

# Find the Android native logging library
find_library(
    log-lib
    log
)

# Define the sources and link requirements of the native library
add_library(
    native-lib
    SHARED
    native-lib.cpp
)

target_link_libraries(
    native-lib
    ${log-lib}
    # Uncomment the following line to link in the CPython library
    # python-lib
)
