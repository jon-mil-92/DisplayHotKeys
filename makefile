header:
	mkdir -p jni

	javac -h jni src/main/java/com/dhk/io/GetDisplay.java
	rm -f src/main/java/com/dhk/io/GetDisplay.class

	javac -h jni src/main/java/com/dhk/io/SetDisplay.java
	rm -f src/main/java/com/dhk/io/SetDisplay.class

dll:
	# Update the JDK include paths to your JDK install location
	g++ --std=c++20 jni/com_dhk_io_GetDisplay.cpp jni/DisplayConfig.cpp -I"C:\jdk25\include" -I"C:\jdk25\include\win32" -shared -o GetDisplay.dll -static
	g++ --std=c++20 jni/com_dhk_io_SetDisplay.cpp jni/DisplayConfig.cpp -I"C:\jdk25\include" -I"C:\jdk25\include\win32" -shared -o SetDisplay.dll -static

clean:
	rm -f jni/com_dhk_io_GetDisplay.h
	rm -f jni/com_dhk_io_SetDisplay.h
	rm -f *.dll

all: clean header dll
