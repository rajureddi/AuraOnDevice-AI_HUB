# Install script for directory: /home/rajub/MNN

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/home/rajub/MNN/project/android/build_64")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "1")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/home/rajub/Android/android-ndk-r25c/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/home/rajub/MNN/transformers/llm/engine/include/" FILES_MATCHING REGEX "/[^/]*\\.hpp$")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/MNN" TYPE FILE FILES
    "/home/rajub/MNN/include/MNN/MNNDefine.h"
    "/home/rajub/MNN/include/MNN/Interpreter.hpp"
    "/home/rajub/MNN/include/MNN/HalideRuntime.h"
    "/home/rajub/MNN/include/MNN/Tensor.hpp"
    "/home/rajub/MNN/include/MNN/ErrorCode.hpp"
    "/home/rajub/MNN/include/MNN/ImageProcess.hpp"
    "/home/rajub/MNN/include/MNN/Matrix.h"
    "/home/rajub/MNN/include/MNN/Rect.h"
    "/home/rajub/MNN/include/MNN/MNNForwardType.h"
    "/home/rajub/MNN/include/MNN/AutoTime.hpp"
    "/home/rajub/MNN/include/MNN/MNNSharedContext.h"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/MNN/expr" TYPE FILE FILES
    "/home/rajub/MNN/include/MNN/expr/Expr.hpp"
    "/home/rajub/MNN/include/MNN/expr/ExprCreator.hpp"
    "/home/rajub/MNN/include/MNN/expr/MathOp.hpp"
    "/home/rajub/MNN/include/MNN/expr/NeuralNetWorkOp.hpp"
    "/home/rajub/MNN/include/MNN/expr/Optimizer.hpp"
    "/home/rajub/MNN/include/MNN/expr/Executor.hpp"
    "/home/rajub/MNN/include/MNN/expr/Module.hpp"
    "/home/rajub/MNN/include/MNN/expr/NeuralNetWorkOp.hpp"
    "/home/rajub/MNN/include/MNN/expr/ExecutorScope.hpp"
    "/home/rajub/MNN/include/MNN/expr/Scope.hpp"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "/home/rajub/MNN/project/android/build_64/libMNN.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/home/rajub/Android/android-ndk-r25c/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libMNN.so")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/home/rajub/MNN/project/android/build_64/source/backend/opencl/cmake_install.cmake")
  include("/home/rajub/MNN/project/android/build_64/express/cmake_install.cmake")
  include("/home/rajub/MNN/project/android/build_64/tools/cv/cmake_install.cmake")
  include("/home/rajub/MNN/project/android/build_64/tools/audio/cmake_install.cmake")
  include("/home/rajub/MNN/project/android/build_64/tools/converter/cmake_install.cmake")

endif()

if(CMAKE_INSTALL_COMPONENT)
  set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
file(WRITE "/home/rajub/MNN/project/android/build_64/${CMAKE_INSTALL_MANIFEST}"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
