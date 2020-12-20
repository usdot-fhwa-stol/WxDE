#include <jni.h>

#include <stdlib.h>
#include <string.h>

#include "metro4j_gen.h"
#include "metro4j.h"
#include "macadam.h"

jint throwNoClassDefError(JNIEnv *env, char *message) {
    jclass exClass;
    char *className = "java/lang/NoClassDefFoundError";

    exClass = (*env)->FindClass(env, className);
    if (exClass == NULL) {
        return throwNoClassDefError(env, className);
    }

    return (*env)->ThrowNew(env, exClass, message);
}

jint throwNoSuchMethodError(JNIEnv *env, char *className, char *methodName, char *signature) {
    jclass exClass;
    char *exClassName = "java/lang/NoSuchMethodError";
    char *msgBuf;
    jint retCode;
    size_t nMallocSize;

    exClass = (*env)->FindClass(env, exClassName);
    if (exClass == NULL) {
        return throwNoClassDefError(env, exClassName);
    }

    nMallocSize = strlen(className)
            + strlen(methodName)
            + strlen(signature) + 8;

    msgBuf = malloc(nMallocSize);
    if (msgBuf == NULL) {
        return throwOutOfMemoryError
                (env, "throwNoSuchMethodError: allocating msgBuf");
    }
    memset(msgBuf, 0, nMallocSize);

    strcpy(msgBuf, className);
    strcat(msgBuf, ".");
    strcat(msgBuf, methodName);
    strcat(msgBuf, ".");
    strcat(msgBuf, signature);

    retCode = (*env)->ThrowNew(env, exClass, msgBuf);
    free(msgBuf);
    return retCode;
}

jint throwNoSuchFieldError(JNIEnv *env, char *message) {
    jclass exClass;
    char *className = "java/lang/NoSuchFieldError";

    exClass = (*env)->FindClass(env, className);
    if (exClass == NULL) {
        return throwNoClassDefError(env, className);
    }

    return (*env)->ThrowNew(env, exClass, message);
}

jint throwOutOfMemoryError(JNIEnv *env, char *message) {
    jclass exClass;
    char *className = "java/lang/OutOfMemoryError";

    exClass = (*env)->FindClass(env, className);
    if (exClass == NULL) {
        return throwNoClassDefError(env, className);
    }

    return (*env)->ThrowNew(env, exClass, message);
}

/*
 * Class:     metro4j_Metro4J
 * Method:    Do_Metro
 * Signature: (ZDD[DJ[J[D[D[D[D[D[D[D[D[D[D[D[D[D[D[J[ZDJJZJ)Lmetro4j/MetroResult;
 */
JNIEXPORT jobject JNICALL Java_metro4j_Metro4J_Do_1Metro
(JNIEnv* env, jclass clazz, jboolean bFlat, jdouble dMLat, jdouble dMLon, jdoubleArray dpZones, jlong nNbrOfZone, jlongArray npMateriau, jdoubleArray dpTA, jdoubleArray dpQP, jdoubleArray dpFF, jdoubleArray dpPS, jdoubleArray dpFsPy, jdoubleArray dpFI, jdoubleArray dpFT, jdoubleArray dpTYP, jdoubleArray dpRc, jdoubleArray dpTAO, jdoubleArray dpRTO, jdoubleArray dpDTO, jdoubleArray dpAH, jdoubleArray dpTimeO, jlongArray npSWO, jbooleanArray bpNoObs, jdouble dDeltaT, jlong nLenObservation, jlong nNbrTimeSteps, jboolean bSilent, jlong tmtTyp) {

    double outRT;
    double outSST;
    long outRC;
    double outSN;
    double outRA;
    long bRet;

//    jsize dpZonesCount = (*env)->GetArrayLength(env, dpZones);
//    jsize npMateriauCount = (*env)->GetArrayLength(env, npMateriau);
//    jsize dpTACount = (*env)->GetArrayLength(env, dpTA);
//    jsize dpQPCount = (*env)->GetArrayLength(env, dpQP);
//    jsize dpFFCount = (*env)->GetArrayLength(env, dpFF);
//    jsize dpPSCount = (*env)->GetArrayLength(env, dpPS);
//    jsize dpFsPyCount = (*env)->GetArrayLength(env, dpFsPy);
//    jsize dpFICount = (*env)->GetArrayLength(env, dpFI);
//    jsize dpFTCount = (*env)->GetArrayLength(env, dpFT);
//    jsize dpTYPCount = (*env)->GetArrayLength(env, dpTYP);
//    jsize dpRcCount = (*env)->GetArrayLength(env, dpRc);
//    jsize dpTAOCount = (*env)->GetArrayLength(env, dpTAO);
//    jsize dpRTOCount = (*env)->GetArrayLength(env, dpRTO);
//    jsize dpDTOCount = (*env)->GetArrayLength(env, dpDTO);
//    jsize dpAHCount = (*env)->GetArrayLength(env, dpAH);
//    jsize dpTimeOCount = (*env)->GetArrayLength(env, dpTimeO);
//    jsize npSWOCount = (*env)->GetArrayLength(env, npSWO);
//    jsize bpNoObsCount = (*env)->GetArrayLength(env, bpNoObs);

    double *dpZonesPtr = (*env)->GetDoubleArrayElements(env, dpZones, 0);
    long *npMateriauPtr = (*env)->GetLongArrayElements(env, npMateriau, 0);
    double *dpTAPtr = (*env)->GetDoubleArrayElements(env, dpTA, 0);
    double *dpQPPtr = (*env)->GetDoubleArrayElements(env, dpQP, 0);
    double *dpFFPtr = (*env)->GetDoubleArrayElements(env, dpFF, 0);
    double *dpPSPtr = (*env)->GetDoubleArrayElements(env, dpPS, 0);
    double *dpFsPyPtr = (*env)->GetDoubleArrayElements(env, dpFsPy, 0);
    double *dpFIPtr = (*env)->GetDoubleArrayElements(env, dpFI, 0);
    double *dpFTPtr = (*env)->GetDoubleArrayElements(env, dpFT, 0);
    double *dpTYPPtr = (*env)->GetDoubleArrayElements(env, dpTYP, 0);
    double *dpRcPtr = (*env)->GetDoubleArrayElements(env, dpRc, 0);
    double *dpTAOPtr = (*env)->GetDoubleArrayElements(env, dpTAO, 0);
    double *dpRTOPtr = (*env)->GetDoubleArrayElements(env, dpRTO, 0);
    double *dpDTOPtr = (*env)->GetDoubleArrayElements(env, dpDTO, 0);
    double *dpAHPtr = (*env)->GetDoubleArrayElements(env, dpAH, 0);
    double *dpTimeOPtr = (*env)->GetDoubleArrayElements(env, dpTimeO, 0);
    long *npSWOPtr = (*env)->GetLongArrayElements(env, npSWO, 0);
    long *bpNoObsPtr = (*env)->GetLongArrayElements(env, bpNoObs, 0);

    Do_Metro(bFlat, dMLat, dMLon, dpZonesPtr, nNbrOfZone, npMateriauPtr, dpTAPtr, dpQPPtr, dpFFPtr, dpPSPtr, dpFsPyPtr, dpFIPtr, dpFTPtr, dpTYPPtr, dpRcPtr, dpTAOPtr, dpRTOPtr, dpDTOPtr, dpAHPtr, dpTimeOPtr, npSWOPtr, bpNoObsPtr, dDeltaT, nLenObservation, nNbrTimeSteps, bSilent, &outRT, &outSST, &outRC, &outSN, &outRA, &bRet, tmtTyp);

    jclass metro4j_MetroResult_class = (*env)->FindClass(env, "metro4j/MetroResult");
    if (!metro4j_MetroResult_class) {
        throwNoClassDefError(env, "metro4j/MetroResult");
    }

    jclass metro4j_MetroResult_init;
    metro4j_MetroResult_init = (*env)->GetMethodID(env, metro4j_MetroResult_class, "<init>", "()V");
    if (!metro4j_MetroResult_init) {
        throwNoSuchMethodError(env, metro4j_MetroResult_class, "<init>", "()V");
    }

    jobject result = (*env)->NewObject(env, metro4j_MetroResult_class, metro4j_MetroResult_init);
    if (!result) {
        // not sure what to do here
    }

    //jfieldID valId = (*env)->GetFieldID(metro4j_MetroResult_class, "val", "I");
    //jfieldID staticValId = (*env)->GetStaticFieldID(metro4j_MetroResult_class, "staticValue", "I");
    /*
     public double outRT;
    public double outSST;
    public double outRC;
    public double outSN;
    public double outRA;
    public double bRet;*/
    jfieldID outRTID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "outRT", "D");
    (*env)->SetDoubleField(env, result, outRTID, outRT);

    jfieldID outSSTID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "outSST", "D");
    (*env)->SetDoubleField(env, result, outSSTID, outSST);

    jfieldID outRCID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "outRC", "J");
    (*env)->SetLongField(env, result, outRCID, outRC);

    jfieldID outSNID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "outSN", "D");
    (*env)->SetDoubleField(env, result, outSNID, outSN);

    jfieldID outRAID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "outRA", "D");
    (*env)->SetDoubleField(env, result, outRAID, outRA);

    jfieldID bRetID = (*env)->GetFieldID(env, metro4j_MetroResult_class, "bRet", "J");
    (*env)->SetLongField(env, result, bRetID, bRet);

    return result;
}

/////*
//// * Class:     MetroJNI
//// * Method:    print_input
//// * Signature: (ZDD[DJ[J[D[D[D[D[D[D[D[D[D[D[D[D[D[D[J[ZDJJ)V
//// */
//JNIEXPORT void JNICALL Java_metro4j_print_1input
//(JNIEnv* env, jclass clazz, jboolean, jdouble, jdouble, jdoubleArray, jlong, jlongArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jlongArray, jbooleanArray, jdouble, jlong, jlong) {
//    /* null for now*/
//}

///*
// * Class:     MetroJNI
// * Method:    get_ra
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1ra
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_ra();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_sn
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1sn
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_sn();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_rc
// * Signature: ()[J
// */
//JNIEXPORT jlongArray JNICALL Java_metro4j_get_1rc
//(JNIEnv* env, jclass clazz) {
//    struct longStruct arrayWrapper = get_rc();
//    jlongArray jarray = (*env)->NewLongArray(env, arrayWrapper.nSize);
//    (*env)->SetLongArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.plArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_rt
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1rt
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_rt();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_ir
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1ir
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_ir();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_sf
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1sf
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_sf();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_fv
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1fv
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_fv();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_fc
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1fc
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_fc();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_fa
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1fa
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_fa();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_g
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1g
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_g();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_bb
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1bb
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_bb();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_fp
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1fp
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_fp();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_echec
// * Signature: ()[J
// */
//JNIEXPORT jlongArray JNICALL Java_metro4j_get_1echec
//(JNIEnv* env, jclass clazz) {
//    struct longStruct arrayWrapper = get_echec();
//    jlongArray jarray = (*env)->NewLongArray(env, arrayWrapper.nSize);
//    (*env)->SetLongArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.plArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_sst
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1sst
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_sst();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_depth
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1depth
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_depth();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_nbr_levels
// * Signature: ()J
// */
//JNIEXPORT jlong JNICALL Java_metro4j_get_1nbr_1levels
//(JNIEnv* env, jclass clazz) {
//    return get_nbr_levels();   
//}
//
///*
// * Class:     MetroJNI
// * Method:    get_lt
// * Signature: ()[D
// */
//JNIEXPORT jdoubleArray JNICALL Java_metro4j_get_1lt
//(JNIEnv* env, jclass clazz) {
//    struct doubleStruct arrayWrapper = get_lt();
//    jdoubleArray jarray = (*env)->NewDoubleArray(env, arrayWrapper.nSize);
//    (*env)->SetDoubleArrayRegion(env, jarray, 0, arrayWrapper.nSize, arrayWrapper.pdArray);
//    return jarray;
//}
