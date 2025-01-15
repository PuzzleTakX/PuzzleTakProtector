
cmake_minimum_required(VERSION 3.4.1)

add_library(
             antitrace

             SHARED

             src/main/jni/antitrace.cpp )

find_library(
             log-lib
             log )

target_link_libraries(
             antitrace

             ${log-lib} )
