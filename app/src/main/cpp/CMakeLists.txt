#  Copyright (c) 2003-2021 Xsens Technologies B.V. or subsidiaries worldwide.
#  All rights reserved.
#
#  Redistribution and use in source and binary forms, with or without modification,
#  are permitted provided that the following conditions are met:
#
#  1.      Redistributions of source code must retain the above copyright notice,
#           this list of conditions, and the following disclaimer.
#
#  2.      Redistributions in binary form must reproduce the above copyright notice,
#           this list of conditions, and the following disclaimer in the documentation
#           and/or other materials provided with the distribution.
#
#  3.      Neither the names of the copyright holders nor the names of their contributors
#           may be used to endorse or promote products derived from this software without
#           specific prior written permission.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
#  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
#  THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
#  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
#  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR
#  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
#  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.THE LAWS OF THE NETHERLANDS
#  SHALL BE EXCLUSIVELY APPLICABLE AND ANY DISPUTES SHALL BE FINALLY SETTLED UNDER THE RULES
#  OF ARBITRATION OF THE INTERNATIONAL CHAMBER OF COMMERCE IN THE HAGUE BY ONE OR MORE
#  ARBITRATORS APPOINTED IN ACCORDANCE WITH SAID RULES.
#

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        xsens

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        xsens.cpp)

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

# [Import Library]
add_library(dataprocessor SHARED IMPORTED)
set_property(TARGET dataprocessor PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libdataprocessor.so)

add_library(xsensdeviceapi SHARED IMPORTED)
set_property(TARGET xsensdeviceapi PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libxsensdeviceapi.so)

add_library(magfieldmapper SHARED IMPORTED)
set_property(TARGET magfieldmapper PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libmagfieldmapper.so)

add_library(xee_solver SHARED IMPORTED)
set_property(TARGET xee_solver PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libxee_solver.so)

add_library(xstypes SHARED IMPORTED)
set_property(TARGET xstypes PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libxstypes.so)

add_library(dataprocessor_java64 SHARED IMPORTED)
set_property(TARGET dataprocessor_java64 PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libdataprocessor_java64.so)

add_library(magfieldmapper_java64 SHARED IMPORTED)
set_property(TARGET magfieldmapper_java64 PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libmagfieldmapper_java64.so)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        xsens

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}

        dataprocessor

        xsensdeviceapi

        magfieldmapper

        xee_solver

        xstypes

        dataprocessor_java64

        magfieldmapper_java64)
