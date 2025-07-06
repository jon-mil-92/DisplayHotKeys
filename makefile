header:
	mkdir -p jni
	
	javac -h jni src/com/dhk/io/EnumDisplayModes.java
	rm -f src/com/dhk/io/EnumDisplayModes.class
	
	javac -h jni src/com/dhk/io/EnumDisplayIds.java
	rm -f src/com/dhk/io/EnumDisplayIds.class
	
	javac -h jni src/com/dhk/io/SetDisplay.java
	rm -f src/com/dhk/io/SetDisplay.class
	
dll:
	# The JNI libraries will only build if JDK 21 is installed in the default path.
	g++ --std=c++20 jni/com_dhk_io_EnumDisplayModes.cpp -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o EnumDisplayModes.dll -static
	g++ --std=c++20 jni/com_dhk_io_EnumDisplayIds.cpp -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o EnumDisplayIds.dll -static
	g++ --std=c++20 jni/com_dhk_io_SetDisplay.cpp -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o SetDisplay.dll -static

clean:
	rm -f jni/com_dhk_io_EnumDisplayIds.h
	rm -f jni/com_dhk_io_EnumDisplayModes.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f *.dll
	
all: clean header dll
	