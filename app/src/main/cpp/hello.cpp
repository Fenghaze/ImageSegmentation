#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_org_pytorch_imagesegmentation_MainActivity_getStrFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement getStrFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}