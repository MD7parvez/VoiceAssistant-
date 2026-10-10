cmake_minimum_required(VERSION 3.22.1)
project(fieldai_whisper LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(
    whisper_cpp
    GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
    GIT_TAG v1.7.6
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(whisper_cpp)

add_library(fieldai_whisper SHARED whisper_jni.cpp)
find_library(log-lib log)
target_link_libraries(fieldai_whisper PRIVATE whisper ${log-lib})
