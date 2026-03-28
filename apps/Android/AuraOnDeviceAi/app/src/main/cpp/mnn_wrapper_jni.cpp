#include <jni.h>
#include "MNN/MNNDefine.h"

extern "C" {

JNIEXPORT jstring JNICALL Java_com_aura_1on_1device_1ai_mnnllm_android_MNN_nativeGetVersion(JNIEnv *env, jobject thiz) {
   return env->NewStringUTF(MNN_VERSION);
}

} // extern "C"
