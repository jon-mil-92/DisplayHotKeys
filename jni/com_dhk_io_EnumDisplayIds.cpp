/*
 * Enumerates the display IDs for the connected displays.
 *
 * Author: Jonathan Miller
 * License: The MIT License - https://mit-license.org/
 *
 * Copyright © 2025 Jonathan Miller
 */

#include <jni.h>
#include "DisplayConfig.h"
#include "com_dhk_io_EnumDisplayIds.h"

using namespace std;

/*
 * Gets the current number of connected displays.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 *
 * @return The current number of connected displays
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_EnumDisplayIds_queryNumOfConnectedDisplays(JNIEnv*, jobject) {
    /*
     * Initialize a structure to hold the active paths as defined in the persistence database for the currently
     * connected displays.
     */
    DisplayConfig displayConfig = getDisplayConfig();

    return displayConfig.numPathInfoArrayElements;
}

/*
 * Gets the IDs for the connected displays.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 *
 * @return An array of IDs for the connected displays
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_EnumDisplayIds_enumDisplayIds(JNIEnv *env, jobject obj) {
    vector<string> displayIdsVector = getQueryDisplayConfigDisplayIds();
    jclass stringClass = env->FindClass("java/lang/String");

    if (stringClass == NULL) {
        return NULL;
    }

    jobjectArray displayIdsArray = env->NewObjectArray(displayIdsVector.size(), stringClass, NULL);

    if (displayIdsArray == NULL) {
        return NULL;
    }

    for (int displayIndex = 0; displayIndex < displayIdsVector.size(); displayIndex++) {
        jstring displayId = env->NewStringUTF(displayIdsVector.at(displayIndex).c_str());

        if (displayId == NULL) {
            for (int j = 0; j < displayIndex; j++) {
                jstring displayIdToDelete = (jstring) env->GetObjectArrayElement(displayIdsArray, j);
                env->DeleteLocalRef(displayIdToDelete);
            }

            env->DeleteLocalRef(displayIdsArray);

            return NULL;
        }

        env->SetObjectArrayElement(displayIdsArray, displayIndex, displayId);
        env->DeleteLocalRef(displayId);
    }

    return displayIdsArray;
}
