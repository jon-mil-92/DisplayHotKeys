/*
 * Original Author: Jonathan Miller
 * Version: 1.4.0.0
 *
 * Description: Enumerate the display IDs for the connected displays.
 *
 * License: The MIT License - https://mit-license.org/
 * Copyright (c) 2024 Jonathan Miller
 */

#include <jni.h>

#include "DisplayConfig.h"
#include "com_dhk_io_EnumDisplayIds.h"

using namespace std;

/*
 * This function gets the current number of connected displays.
 *
 * @param env - The structure containing methods to use to to access Java elements.
 * @param obj - The reference to the Java native object instance.
 *
 * @return The current number of connected displays.
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_EnumDisplayIds_queryNumOfConnectedDisplays(JNIEnv*, jobject) {
    // Initialize a structure to hold the active paths as defined in the persistence database for the currently
    // connected displays.
    DisplayConfig displayConfig = getDisplayConfig();

    return displayConfig.numPathInfoArrayElements;
}

/*
 * This function gets the IDs for the connected displays.
 *
 * @param env - The structure containing methods to use to to access Java elements.
 * @param obj - The reference to the Java native object instance.
 *
 * @return An array of IDs for the connected displays.
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_EnumDisplayIds_enumDisplayIds(JNIEnv *env, jobject obj) {
    // Initialize a vector to hold all of the display IDs from the QueryDisplayConfig enumeration.
    vector<string> displayIdsVector = getQueryDisplayConfigDisplayIds();

    // Find the java.lang.String class.
    jclass stringClass = env->FindClass("java/lang/String");

    // Return null if the String class could not be found.
    if (stringClass == NULL) {
        return NULL;
    }

    // Create a jobjectArray of JNI Strings to hold all of the display IDs.
    jobjectArray displayIdsArray = env->NewObjectArray(displayIdsVector.size(), stringClass, NULL);

    // Return null if the JNI String array could not be created.
    if (displayIdsArray == NULL) {
        return NULL;
    }

    // Populate the JNI String array of display ID strings.
    for (int displayIndex = 0; displayIndex < displayIdsVector.size(); displayIndex++) {
        // Create a JNI String with the current display ID string.
        jstring displayId = env->NewStringUTF(displayIdsVector.at(displayIndex).c_str());

        // Return null if the JNI String for the current ID could not be created.
        if (displayId == NULL) {
            // Clean up the previously created JNI Strings for the IDs.
            for (int j = 0; j < displayIndex; j++) {
                jstring displayIdToDelete = (jstring) env->GetObjectArrayElement(displayIdsArray, j);
                env->DeleteLocalRef(displayIdToDelete);
            }

            // Clean up the JNI String array.
            env->DeleteLocalRef(displayIdsArray);

            return NULL;
        }

        // Add the current display ID string to the array of display ID strings, and clean up memory.
        env->SetObjectArrayElement(displayIdsArray, displayIndex, displayId);
        env->DeleteLocalRef(displayId);
    }

    return displayIdsArray;
}
