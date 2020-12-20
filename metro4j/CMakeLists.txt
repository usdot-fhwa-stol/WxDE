cmake_minimum_required (VERSION 2.8)

project (metro4j C Fortran)
#enable_language(Fortran)

# make sure that the default is a RELEASE
if (NOT CMAKE_BUILD_TYPE)
  set (CMAKE_BUILD_TYPE RELEASE CACHE STRING
      "Choose the type of build, options are: None Debug Release."
      FORCE)
endif (NOT CMAKE_BUILD_TYPE)

# Allow the developer to select if Dynamic or Static libraries are built
OPTION (BUILD_SHARED_LIBS "Build Shared Libraries" ON)
# Set the LIB_TYPE variable to STATIC
SET (LIB_TYPE SHARED)
IF (NOT BUILD_SHARED_LIBS)
  # User wants to build Dynamic Libraries, so change the LIB_TYPE variable to CMake keyword 'SHARED'
  SET (LIB_TYPE STATIC)
ENDIF (NOT BUILD_SHARED_LIBS)

# default installation
get_filename_component (default_prefix ".." ABSOLUTE)
set (CMAKE_INSTALL_PREFIX ${default_prefix} CACHE STRING
      "Choose the installation directory; by default it installs in the metro4j directory."
      FORCE)
message (STATUS "CMAKE_INSTALL_PREFIX=" ${CMAKE_INSTALL_PREFIX})

# FFLAGS depend on the compiler
get_filename_component (Fortran_COMPILER_NAME ${CMAKE_Fortran_COMPILER} NAME)

if (Fortran_COMPILER_NAME MATCHES "gfortran.*")
  # gfortran
  set (CMAKE_Fortran_FLAGS_RELEASE "-funroll-all-loops -fno-f2c -O3 -fdefault-integer-8")
  set (CMAKE_Fortran_FLAGS_DEBUG   "-fno-f2c -O0 -g -fdefault-integer-8")
#elseif (Fortran_COMPILER_NAME MATCHES "ifort.*")
#  # ifort (untested)
#  set (CMAKE_Fortran_FLAGS_RELEASE "-f77rtl -O3")
#  set (CMAKE_Fortran_FLAGS_DEBUG   "-f77rtl -O0 -g")
#elseif (Fortran_COMPILER_NAME MATCHES "g77")
#  # g77
#  set (CMAKE_Fortran_FLAGS_RELEASE "-funroll-all-loops -fno-f2c -O3 -m32")
#  set (CMAKE_Fortran_FLAGS_DEBUG   "-fno-f2c -O0 -g -m32")
else (Fortran_COMPILER_NAME MATCHES "gfortran.*")
  message ("CMAKE_Fortran_COMPILER full path: " ${CMAKE_Fortran_COMPILER})
  message ("Fortran compiler: " ${Fortran_COMPILER_NAME})
  message ("No optimized Fortran compiler flags are known, we just try -O2...")
  set (CMAKE_Fortran_FLAGS_RELEASE "-O2")
  set (CMAKE_Fortran_FLAGS_DEBUG   "-O0 -g")
endif (Fortran_COMPILER_NAME MATCHES "gfortran.*")

#CMake 3.0 and later prefer this property to be ON by default.  Projects
#wanting ``@rpath`` in a target's install name may remove any setting of
#the ``INSTALL_NAME_DIR`` and ``CMAKE_INSTALL_NAME_DIR``
#variables.
set(CMAKE_MACOSX_RPATH 1)

find_package (JNI REQUIRED)

message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
message (STATUS "JNI_FOUND=${JNI_FOUND}")
message (STATUS "JAVA_AWT_LIBRARY=${JAVA_AWT_LIBRARY}")
message (STATUS "JAVA_JVM_LIBRARY=${JAVA_JVM_LIBRARY}")
message (STATUS "JAVA_INCLUDE_PATH=${JAVA_INCLUDE_PATH}")
message (STATUS "JAVA_INCLUDE_PATH2=${JAVA_INCLUDE_PATH2}")
message (STATUS "JAVA_AWT_INCLUDE_PATH=${JAVA_AWT_INCLUDE_PATH}")
message (STATUS "CMAKE_CURRENT_SOURCE_DIR=${CMAKE_CURRENT_SOURCE_DIR}")

macro (add_sources)
    file (RELATIVE_PATH _relPath "${CMAKE_SOURCE_DIR}" "${CMAKE_CURRENT_SOURCE_DIR}")
    foreach (_src ${ARGN})
        if (_relPath)
            list (APPEND SRCS "${_relPath}/${_src}")
        else()
            list (APPEND SRCS "${_src}")
        endif()
    endforeach()
    if (_relPath)
        # propagate SRCS to parent directory
        set (SRCS ${SRCS} PARENT_SCOPE)
    endif()
endmacro()

include_directories (${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2} ./src/main/c/include)
add_sources (
    "src/main/c/global.c"
    "src/main/c/macadam.c"
    "src/main/c/metro4j.c"
    "src/main/c/array2matrix.f"
    "src/main/c/balanc.f"
    "src/main/c/constPhys.f"
    "src/main/c/coupla.f"
    "src/main/c/flxsurfz.f"
    "src/main/c/grille.f"
    "src/main/c/initial.f"
    "src/main/c/lib_gen.f"
    "src/main/c/lib_therm.f"
)

message (STATUS ${SRCS})
add_library (metro ${LIB_TYPE} ${SRCS})

install(TARGETS metro
        RUNTIME DESTINATION bin
        LIBRARY DESTINATION lib)
#install(DIRECTORY src/main/c/include DESTINATION include/metro



set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -pedantic -O2")
set (CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/build)
set (CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/build)
set (CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/build)
