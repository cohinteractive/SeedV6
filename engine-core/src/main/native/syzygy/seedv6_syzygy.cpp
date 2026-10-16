#include <jni.h>
#include <string>
#include "fathom/tbprobe.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_ohinteractive_seedv6_search_tablebase_SyzygyNative_initialize(JNIEnv *env, jclass, jbyteArray bytes) {
    jsize length = env->GetArrayLength(bytes);
    std::string path(length, '\0');
    env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(path.data()));
    if(env->ExceptionCheck()) return 0;
    return tb_init(path.c_str()) ? TB_LARGEST : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ohinteractive_seedv6_search_tablebase_SyzygyNative_root(JNIEnv*, jclass, jlong a, jlong b, jlong c, jlong d, jint status) {
    uint64_t b0 = a, b1 = b, b2 = c, black = d, occupied = b0 | b1 | b2;
    unsigned ep = (status >> 5) & 63;
    // Match Board.enPassantSquare's side-dependent validity, including an absent raw field.
    if((ep >> 3) != ((status & 1) ? 2u : 5u)) ep = 0;
    return tb_probe_root(occupied & ~black, black,
        b0 & ~b1 & ~b2, ~b0 & b1 & ~b2, b0 & b1 & ~b2,
        ~b0 & ~b1 & b2, b0 & ~b1 & b2, ~b0 & b1 & b2,
        (status >> 11) & 127, (status >> 1) & 15, ep, (status & 1) == 0, nullptr);
}
