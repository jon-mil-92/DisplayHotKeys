CXX ?= g++
CXXSTD ?= --std=c++20
OPT ?= -O2
WINDRES ?= windres
RCFLAGS ?= --codepage=65001 -O coff
TOOLCHAIN_FLAGS ?=
JAVA_HOME ?= C:\jdk21
JAVAC ?= $(subst \,/,$(JAVA_HOME))/bin/javac
MINGW ?= C:\msys64\mingw64
GCCVER := $(shell $(CXX) -dumpfullversion)
COMMON_DEFS = -DUNICODE -D_UNICODE -D_WIN32 -D_WINDOWS -DWIN32_LEAN_AND_MEAN -D_WIN32_WINNT=0x0A00 -DWINVER=0x0A00
INCLUDES = -I"$(JAVA_HOME)\include" -I"$(JAVA_HOME)\include\win32" -I"$(MINGW)\include" -I"$(MINGW)\include\c++\$(GCCVER)" -I"$(MINGW)\lib\gcc\x86_64-w64-mingw32\$(GCCVER)\include" -I"$(MINGW)\lib\gcc\x86_64-w64-mingw32\$(GCCVER)\include-fixed"
LDFLAGS = -shared -static
LAUNCHER_LDFLAGS = -static -mwindows -municode -s -Wl,--gc-sections

clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f jni/com_dhk_io_DisplayEventNotifier.h
	rm -f *.res.o
	rm -f *.dll
	rm -f distribution/GetDisplay.dll distribution/SetDisplay.dll distribution/DisplayEventNotifier.dll
	rm -f distribution/DisplayHotKeysLauncher.exe

header:
	mkdir -p jni

	$(JAVAC) -h jni src/main/java/com/dhk/io/GetDisplay.java
	rm -f src/main/java/com/dhk/io/GetDisplay.class

	$(JAVAC) -h jni src/main/java/com/dhk/io/SetDisplay.java
	rm -f src/main/java/com/dhk/io/SetDisplay.class

	$(JAVAC) -h jni src/main/java/com/dhk/io/DisplayChangeListener.java src/main/java/com/dhk/io/ShellRestartListener.java src/main/java/com/dhk/io/DisplayEventNotifier.java
	rm -f src/main/java/com/dhk/io/DisplayChangeListener.class
	rm -f src/main/java/com/dhk/io/ShellRestartListener.class
	rm -f src/main/java/com/dhk/io/DisplayEventNotifier.class

dll:
	# Update the include paths to your JDK / mingw install locations
	# TOOLCHAIN_FLAGS is empty by default to avoid passing unsupported options to g++ in some environments
	# Compile the version-info resources so each DLL carries its Details-tab metadata, then link them into the DLLs
	$(WINDRES) $(RCFLAGS) jni/GetDisplay.rc GetDisplay.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp jni/ArrangeDisplay.cpp GetDisplay.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -ldxgi -o GetDisplay.dll
	$(WINDRES) $(RCFLAGS) jni/SetDisplay.rc SetDisplay.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp jni/ArrangeDisplay.cpp SetDisplay.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o SetDisplay.dll
	$(WINDRES) $(RCFLAGS) jni/DisplayEventNotifier.rc DisplayEventNotifier.res.o
	$(CXX) $(CXXSTD) $(OPT) jni/com_dhk_io_DisplayEventNotifier.cpp jni/DisplayConfig.cpp DisplayEventNotifier.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LDFLAGS) -o DisplayEventNotifier.dll

	# The resources are linked in by now, so drop the intermediates instead of leaving them in the project root
	rm -f GetDisplay.res.o SetDisplay.res.o DisplayEventNotifier.res.o

	mkdir -p distribution
	cp -f GetDisplay.dll SetDisplay.dll DisplayEventNotifier.dll distribution/

launcher:
	# Build the unelevated launcher that starts the elevated app through its scheduled task
	# The resource script embeds the asInvoker manifest, so the launcher itself never triggers elevation
	mkdir -p distribution

	# Unlike the libraries, nothing loads this from the project root, so link it straight into the packaging folder
	$(WINDRES) $(RCFLAGS) jni/DisplayHotKeysLauncher.rc DisplayHotKeysLauncher.res.o
	$(CXX) $(CXXSTD) $(OPT) -ffunction-sections -fdata-sections jni/DisplayHotKeysLauncher.cpp DisplayHotKeysLauncher.res.o $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS) $(LAUNCHER_LDFLAGS) -lole32 -loleaut32 -ltaskschd -lshell32 -luuid -o distribution/DisplayHotKeysLauncher.exe

	# The resource is linked in by now, so drop the intermediate instead of leaving it in the project root
	rm -f DisplayHotKeysLauncher.res.o

all: clean header dll launcher
