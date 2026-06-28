CXX ?= g++
CXXSTD ?= --std=c++20
TOOLCHAIN_FLAGS ?=
COMMON_DEFS = -DUNICODE -D_UNICODE -D_WIN32 -D_WINDOWS -DWIN32_LEAN_AND_MEAN
INCLUDES = -I"C:\jdk21\include" -I"C:\jdk21\include\win32" -I"C:\msys64\mingw64\include" -I"C:\msys64\mingw64\include\c++\16.1.0" -I"C:\msys64\mingw64\lib\gcc\x86_64-w64-mingw32\16.1.0\include" -I"C:\msys64\mingw64\lib\gcc\x86_64-w64-mingw32\16.1.0\include-fixed"
LDFLAGS = -shared -static

header:
	mkdir -p jni

	javac -h jni src/main/java/com/dhk/io/GetDisplay.java
	rm -f src/main/java/com/dhk/io/GetDisplay.class

	javac -h jni src/main/java/com/dhk/io/SetDisplay.java
	rm -f src/main/java/com/dhk/io/SetDisplay.class

	javac -h jni src/main/java/com/dhk/io/DisplayChangeListener.java src/main/java/com/dhk/io/DisplayEventNotifier.java
	rm -f src/main/java/com/dhk/io/DisplayChangeListener.class
	rm -f src/main/java/com/dhk/io/DisplayEventNotifier.class

dll:
	# Update the JDK include paths to your JDK install location
	# TOOLCHAIN_FLAGS is empty by default to avoid passing unsupported options to g++ in some environments.
	$(CXX) $(CXXSTD) jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o GetDisplay.dll
	$(CXX) $(CXXSTD) jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o SetDisplay.dll
	$(CXX) $(CXXSTD) jni/com_dhk_io_DisplayEventNotifier.cpp jni/DisplayConfig.cpp $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o DisplayEventNotifier.dll

clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f jni/com_dhk_io_DisplayEventNotifier.h
	rm -f *.dll

all: clean header dll
