header:
	mkdir -p jni

	javac -h jni src/main/java/com/dhk/io/GetDisplay.java
	rm -f src/main/java/com/dhk/io/GetDisplay.class

	javac -h jni src/main/java/com/dhk/io/SetDisplay.java
	rm -f src/main/java/com/dhk/io/SetDisplay.class

dll:
	# The JNI libraries will only build if JDK 21 is installed in the default path.
	g++ --std=c++20 jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o GetDisplay.dll -static
	g++ --std=c++20 jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o SetDisplay.dll -static

clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f *.dll

all: clean header dll
