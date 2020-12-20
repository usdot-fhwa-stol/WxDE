#ifndef _Included_Metro4J
#define _Included_Metro4J
#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>    

#include "metro4j_gen.h"
    
jint throwNoClassDefError(JNIEnv *env, char *message);   
jint throwNoSuchMethodError(JNIEnv *env, char *className, char *methodName, char *signature);
jint throwNoSuchFieldError(JNIEnv *env, char *message);
jint throwOutOfMemoryError(JNIEnv *env, char *message);
    
#ifdef __cplusplus
}
#endif
#endif