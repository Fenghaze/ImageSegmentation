cmake_minimum_required(VERSION 3.4.1)

add_library(hello SHARED hello.cpp clipper.cpp)

find_library(log-lib log )

target_link_libraries(hello ${log-lib} )

