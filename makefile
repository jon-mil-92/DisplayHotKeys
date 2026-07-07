CXX ?= g++
CXXSTD ?= --std=c++20
OPT ?= -O2
WINDRES ?= windres
RCFLAGS ?= --codepage=65001 -O coff
TOOLCHAIN_FLAGS ?=
COMMON_DEFS = -DUNICODE -D_UNICODE -D_WIN32 -D_WINDOWS -DWIN32_LEAN_AND_MEAN -D_WIN32_WINNT=0x0A00 -DWINVER=0x0A00
INCLUDES = -I"C:\jdk21\include" -I"C:\jdk21\include\win32" -I"C:\msys64\mingw64\include" -I"C:\msys64\mingw64\include\c++\16.1.0" -I"C:\msys64\mingw64\lib\gcc\x86_64-w64-mingw32\16.1.0\include" -I"C:\msys64\mingw64\lib\gcc\x86_64-w64-mingw32\16.1.0\include-fixed"
LDFLAGS = -shared -static

clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f jni/com_dhk_io_DisplayEventNotifier.h
	rm -f *.res.o
	rm -f *.dll
	rm -f launch4j/GetDisplay.dll launch4j/SetDisplay.dll launch4j/DisplayEventNotifier.dll

header:
	mkdir -p jni

	javac -h jni src/main/java/com/dhk/io/GetDisplay.java
	rm -f src/main/java/com/dhk/io/GetDisplay.class

	javac -h jni src/main/java/com/dhk/io/SetDisplay.java
	rm -f src/main/java/com/dhk/io/SetDisplay.class

	javac -h jni src/main/java/com/dhk/io/DisplayChangeListener.java src/main/java/com/dhk/io/ShellRestartListener.java src/main/java/com/dhk/io/DisplayEventNotifier.java
	rm -f src/main/java/com/dhk/io/DisplayChangeListener.class
	rm -f src/main/java/com/dhk/io/ShellRestartListener.class
	rm -f src/main/java/com/dhk/io/DisplayEventNotifier.class

dll:
	# Update the JDK include paths to your JDK install location
	# TOOLCHAIN_FLAGS is empty by default to avoid passing unsupported options to g++ in some environments
	# Compile the version-info resources so each DLL carries its Details-tab metadata, then link them into the DLLs
	$(WINDRES) $(RCFLAGS) jni/GetDisplay.rc GetDisplay.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp GetDisplay.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o GetDisplay.dll
	$(WINDRES) $(RCFLAGS) jni/SetDisplay.rc SetDisplay.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp SetDisplay.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o SetDisplay.dll
	$(WINDRES) $(RCFLAGS) jni/DisplayEventNotifier.rc DisplayEventNotifier.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_DisplayEventNotifier.cpp jni/DisplayConfig.cpp DisplayEventNotifier.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o DisplayEventNotifier.dll

	mkdir -p launch4j
	cp -f GetDisplay.dll SetDisplay.dll DisplayEventNotifier.dll launch4j/


all: clean header dll
