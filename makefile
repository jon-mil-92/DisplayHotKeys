# Builds the JNI display libraries, the notification area icon library, and the app launcher.
# Update JAVA_HOME and MINGW to the local JDK and MinGW-w64 install locations.
CXX ?= g++
CXXSTD ?= --std=c++20
OPT ?= -O2
WINDRES ?= windres
RCFLAGS ?= --codepage=65001 -O coff

# Empty by default to avoid passing unsupported options to g++ in some environments
TOOLCHAIN_FLAGS ?=

JAVA_HOME ?= C:\jdk21
JAVAC ?= $(subst \,/,$(JAVA_HOME))/bin/javac
MINGW ?= C:\msys64\mingw64
GCCVER := $(shell $(CXX) -dumpfullversion)

# Package directory of the Java types the JNI headers are generated from
IO_DIR = src/main/java/com/dhk/io

COMMON_DEFS = -DUNICODE -D_UNICODE -D_WIN32 -D_WINDOWS -DWIN32_LEAN_AND_MEAN -D_WIN32_WINNT=0x0A00 -DWINVER=0x0A00

INCLUDES = -I"$(JAVA_HOME)\include" -I"$(JAVA_HOME)\include\win32" -I"$(MINGW)\include" \
	-I"$(MINGW)\include\c++\$(GCCVER)" -I"$(MINGW)\lib\gcc\x86_64-w64-mingw32\$(GCCVER)\include" \
	-I"$(MINGW)\lib\gcc\x86_64-w64-mingw32\$(GCCVER)\include-fixed"

CXXFLAGS = $(CXXSTD) $(OPT) $(COMMON_DEFS) $(INCLUDES) $(TOOLCHAIN_FLAGS)
LDFLAGS = -shared -static
LAUNCHER_LDFLAGS = -static -mwindows -municode -s -Wl,--gc-sections

.PHONY: clean header dll launcher all

# Remove every generated header, intermediate resource object, and built binary
clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f jni/com_dhk_io_DisplayEventNotifier.h
	rm -f jni/com_dhk_io_SystemTrayIcon.h
	rm -f *.res.o
	rm -f *.dll
	rm -f distribution/GetDisplay.dll
	rm -f distribution/SetDisplay.dll
	rm -f distribution/DisplayEventNotifier.dll
	rm -f distribution/SystemTrayIcon.dll
	rm -f distribution/DisplayHotKeysLauncher.exe

# Generate the JNI headers. javac -h runs with no classpath, so each library's types compile together standalone,
# and the .class files emitted as a side effect are removed since the application build compiles the real classes
header:
	mkdir -p jni

	$(JAVAC) -h jni $(IO_DIR)/GetDisplay.java
	rm -f $(IO_DIR)/GetDisplay.class

	$(JAVAC) -h jni $(IO_DIR)/SetDisplay.java
	rm -f $(IO_DIR)/SetDisplay.class

	$(JAVAC) -h jni $(IO_DIR)/DisplayChangeListener.java $(IO_DIR)/ShellRestartListener.java \
		$(IO_DIR)/DisplayEventNotifier.java
	rm -f $(IO_DIR)/DisplayChangeListener.class
	rm -f $(IO_DIR)/ShellRestartListener.class
	rm -f $(IO_DIR)/DisplayEventNotifier.class

	$(JAVAC) -h jni $(IO_DIR)/TrayIconListener.java $(IO_DIR)/TrayIconRenderer.java $(IO_DIR)/SystemTrayIcon.java
	rm -f $(IO_DIR)/TrayIconListener.class
	rm -f $(IO_DIR)/TrayIconRenderer.class
	rm -f $(IO_DIR)/SystemTrayIcon.class

# Build the JNI libraries. Each version-info resource is compiled first so its DLL carries the Details-tab metadata
dll:
	$(WINDRES) $(RCFLAGS) jni/GetDisplay.rc GetDisplay.res.o
	$(CXX) $(CXXFLAGS) $(LDFLAGS) jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp jni/ArrangeDisplay.cpp \
		GetDisplay.res.o -ldxgi -o GetDisplay.dll

	$(WINDRES) $(RCFLAGS) jni/SetDisplay.rc SetDisplay.res.o
	$(CXX) $(CXXFLAGS) $(LDFLAGS) jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp jni/ArrangeDisplay.cpp \
		SetDisplay.res.o -o SetDisplay.dll

	$(WINDRES) $(RCFLAGS) jni/DisplayEventNotifier.rc DisplayEventNotifier.res.o
	$(CXX) $(CXXFLAGS) $(LDFLAGS) jni/com_dhk_io_DisplayEventNotifier.cpp jni/DisplayConfig.cpp \
		DisplayEventNotifier.res.o -o DisplayEventNotifier.dll

	$(WINDRES) $(RCFLAGS) jni/SystemTrayIcon.rc SystemTrayIcon.res.o
	$(CXX) $(CXXFLAGS) $(LDFLAGS) jni/com_dhk_io_SystemTrayIcon.cpp SystemTrayIcon.res.o \
		-lshell32 -lgdi32 -lshcore -o SystemTrayIcon.dll

	# The resources are linked in by now, so drop the intermediates instead of leaving them in the project root
	rm -f GetDisplay.res.o SetDisplay.res.o DisplayEventNotifier.res.o SystemTrayIcon.res.o

	mkdir -p distribution
	cp -f GetDisplay.dll SetDisplay.dll DisplayEventNotifier.dll SystemTrayIcon.dll distribution/

# Build the unelevated launcher that starts the elevated app through its scheduled task. The resource script embeds
# the asInvoker manifest, so the launcher itself never triggers elevation
launcher:
	mkdir -p distribution

	# Unlike the libraries, nothing loads this from the project root, so link it straight into the packaging folder
	$(WINDRES) $(RCFLAGS) jni/DisplayHotKeysLauncher.rc DisplayHotKeysLauncher.res.o
	$(CXX) $(CXXFLAGS) -ffunction-sections -fdata-sections jni/DisplayHotKeysLauncher.cpp \
		DisplayHotKeysLauncher.res.o $(LAUNCHER_LDFLAGS) -lole32 -loleaut32 -ltaskschd -lshell32 -luuid \
		-o distribution/DisplayHotKeysLauncher.exe

	# The resource is linked in by now, so drop the intermediate instead of leaving it in the project root
	rm -f DisplayHotKeysLauncher.res.o

# Full clean rebuild of the headers, libraries, and launcher
all: clean header dll launcher
