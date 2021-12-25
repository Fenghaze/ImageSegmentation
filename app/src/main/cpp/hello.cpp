#include <jni.h>
#include <string>
#include "clipper.h"

extern "C" jstring
Java_org_pytorch_imagesegmentation_MainActivity_getStrFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement getStrFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" int
Java_org_pytorch_imagesegmentation_OcrProcessor_getindex(JNIEnv *env, jclass clazz){
    int a = getindex();
    return a;
}