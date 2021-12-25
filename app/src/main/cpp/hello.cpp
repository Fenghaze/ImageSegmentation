#include <jni.h>
#include <string>
#include "clipper.h"
#include "polygon.h"
#include <jni.h>

extern "C" jstring
Java_org_pytorch_imagesegmentation_MainActivity_getStrFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement getStrFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_pytorch_imagesegmentation_OcrProcessor_unclip(JNIEnv *env, jclass clazz, jdouble x1,
                                                       jdouble y1, jdouble x2, jdouble y2,
                                                       jdouble x3, jdouble y3, jdouble x4,
                                                       jdouble y4, jint unclip_ratio) {
    // TODO: implement unclip()

    //二维数组转为vector
    Polygon::point vertex[4];
    Polygon::point p1(x1, y1);
    Polygon::point p2(x2, y2);
    Polygon::point p3(x3, y3);
    Polygon::point p4(x4, y4);
    vertex[0] = p1;
    vertex[1] = p2;
    vertex[2] = p3;
    vertex[3] = p4;
    auto poly = new Polygon::polygon(4, vertex);
    auto distance = poly->area() * unclip_ratio / poly->perimeter(); //多边形面积*ratio/多边形周长

    ClipperLib::Path path;
    for(int i=0; i<4; i++){
        ClipperLib::IntPoint p(vertex[i].x, vertex[i].y);
        path << p;
    }
    ClipperLib::ClipperOffset offset;
    offset.AddPath(path, static_cast<ClipperLib::JoinType>(1), static_cast<ClipperLib::EndType>(0));

    ClipperLib::Paths solution;
    offset.Execute(solution, distance);     //轮廓缩放,solution中包含缩放后的轮廓
    int size = (int)solution[0].size();
    int arr[size][2];
    //solution存放到arr数组
    for(int i=0; i<size; i++){
        auto x = solution[0][i].X;
        auto y = solution[0][i].Y;
        arr[i][0] = (int)x;
        arr[i][1] = (int)y;
    }

    // Get the int array class
    jclass cls = env->FindClass("[I");
    jintArray inival = env->NewIntArray(size);

    // Create the returnable jobjectArray with an initial value
    jobjectArray outer = env->NewObjectArray(size, cls, inival);
    for (int i = 0; i < size; i++) {
        jintArray inner = env->NewIntArray(2);
        env->SetIntArrayRegion(inner, 0, 2, arr[i]);
        // set inner's values
        env->SetObjectArrayElement(outer, i, inner);
        env->DeleteLocalRef(inner);
    }
    //返回二维数组
    return outer;
}