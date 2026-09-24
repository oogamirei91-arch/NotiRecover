cmake_minimum_required(VERSION 3.22.1)

project("security-core")

add_library(
    security-core
    SHARED
    security-core.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    security-core
    ${log-lib}
)
