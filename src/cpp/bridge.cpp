//
//  bridge-lib.cpp
//  cross
//
//  Created by Ali Asadpoor on 1/15/19.
//  Copyright © 2019 Shaidin. All rights reserved.
//

#include "../../extern/core/src/bridge.h"
#include "../../extern/core/src/cross.h"

#include <jni.h>

JavaVM* jvm_;
JNIEnv *env_;
jobject me_;
jobject tme_;
jmethodID need_restart_;
jmethodID load_view_;
jmethodID call_function_;
jmethodID get_preference_;
jmethodID set_preference_;
jmethodID async_message_;
jmethodID add_param_;
jmethodID post_http_;
jmethodID create_image_;
jmethodID reset_image_;
jmethodID exit_;


jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jvm_ = vm;
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
    env_->DeleteGlobalRef(tme_);
}

void bridge::NeedRestart()
{
    env_->CallVoidMethod(me_, need_restart_);
}

void bridge::LoadView(const std::int32_t sender, const std::int32_t view_info, const char* html)
{
    jstring jHtml = env_->NewStringUTF(html);
    env_->CallVoidMethod(me_, load_view_, sender, view_info, jHtml);
    env_->DeleteLocalRef(jHtml);
}

void bridge::CallFunction(const char* function)
{
    jstring jFunction = env_->NewStringUTF(function);
    env_->CallVoidMethod(me_, call_function_, jFunction);
    env_->DeleteLocalRef(jFunction);
}

std::string bridge::GetPreference(const char* key)
{
    jstring jKey = env_->NewStringUTF(key);
    jstring jValue = (jstring)env_->CallObjectMethod(me_, get_preference_, jKey);
    const char* szValue = env_->GetStringUTFChars(jValue, nullptr);
    std::string value = szValue;
    env_->ReleaseStringUTFChars(jValue, szValue);
    env_->DeleteLocalRef(jKey);
    env_->DeleteLocalRef(jValue);
    return value;
}

void bridge::SetPreference(const char* key, const char* value)
{
    jstring jKey = env_->NewStringUTF(key);
    jstring jValue = env_->NewStringUTF(value);
    env_->CallVoidMethod(me_, set_preference_, jKey, jValue);
    env_->DeleteLocalRef(jKey);
    env_->DeleteLocalRef(jValue);
}

void bridge::AsyncMessage(std::int32_t receiver, const char* id, const char* command, const char* info)
{
    JNIEnv* env_;
    jvm_->AttachCurrentThread(&env_, nullptr);
    jstring jId = env_->NewStringUTF(id);
    jstring jCommand = env_->NewStringUTF(command);
    jstring jInfo = env_->NewStringUTF(info);
    env_->CallVoidMethod(tme_, async_message_, receiver, jId, jCommand, jInfo);
    env_->DeleteLocalRef(jId);
    env_->DeleteLocalRef(jCommand);
    env_->DeleteLocalRef(jInfo);
    jvm_->DetachCurrentThread();
}

void bridge::AddParam(const char *key, const char *value)
{
    jstring jKey = env_->NewStringUTF(key);
    jstring jValue = env_->NewStringUTF(value);
    env_->CallVoidMethod(me_, add_param_, jKey, jValue);
    env_->DeleteLocalRef(jKey);
    env_->DeleteLocalRef(jValue);
}

void bridge::PostHttp(const std::int32_t sender, const char* id, const char* command, const char *url)
{
    jstring jId = env_->NewStringUTF(id);
    jstring jCommand = env_->NewStringUTF(command);
    jstring jUrl = env_->NewStringUTF(url);
    env_->CallVoidMethod(me_, post_http_, sender, jId, jCommand, jUrl);
    env_->DeleteLocalRef(jId);
    env_->DeleteLocalRef(jCommand);
    env_->DeleteLocalRef(jUrl);
}

void bridge::CreateImage(const char* id, const char* parent)
{
    jstring jId = env_->NewStringUTF(id);
    jstring jParent = env_->NewStringUTF(parent);
    env_->CallVoidMethod(me_, create_image_, jId, jParent);
    env_->DeleteLocalRef(jId);
    env_->DeleteLocalRef(jParent);
}

void bridge::ResetImage(const std::int32_t sender, const std::int32_t index, const char* id)
{
    jstring jId = env_->NewStringUTF(id);
    env_->CallVoidMethod(me_, reset_image_, sender, index, jId);
    env_->DeleteLocalRef(jId);
}

void bridge::Exit()
{
    env_->CallVoidMethod(me_, exit_);
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Setup(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    tme_ = env_->NewGlobalRef(me);
    need_restart_ = env_->GetMethodID(env_->GetObjectClass(me_), "NeedRestart",
            "()V");
    load_view_ = env_->GetMethodID(env_->GetObjectClass(me_), "LoadView",
            "(IILjava/lang/String;)V");
    call_function_ = env_->GetMethodID(env_->GetObjectClass(me_), "CallFunction",
            "(Ljava/lang/String;)V");
    get_preference_ = env_->GetMethodID(env_->GetObjectClass(me_), "GetPreference",
            "(Ljava/lang/String;)Ljava/lang/String;");
    set_preference_ = env_->GetMethodID(env_->GetObjectClass(me_), "SetPreference",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    async_message_ = env_->GetMethodID(env_->GetObjectClass(me_), "AsyncMessage",
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    add_param_ = env_->GetMethodID(env_->GetObjectClass(me_), "AddParam",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    post_http_ = env_->GetMethodID(env_->GetObjectClass(me_), "PostHttp",
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    create_image_ = env_->GetMethodID(env_->GetObjectClass(me_), "CreateImage",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    reset_image_ = env_->GetMethodID(env_->GetObjectClass(me_), "ResetImage",
            "(IILjava/lang/String;)V");
    exit_= env_->GetMethodID(env_->GetObjectClass(me_), "Exit",
            "()V");
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Begin(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Begin();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_End(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::End();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Create(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Create();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Destroy(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Destroy();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Start(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Start();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Stop(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Stop();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Restart(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Restart();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Escape(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    cross::Escape();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Handle(JNIEnv *env, jobject me, const jstring id, const jstring command, const jstring info)
{
    env_ = env;
    me_ = me;
    const char* nId = env_->GetStringUTFChars(id, nullptr);
    const char* nCommand = env_->GetStringUTFChars(command, nullptr);
    const char* nInfo = env_->GetStringUTFChars(info, nullptr);
    cross::Handle(nId, nCommand, nInfo);
    env_->ReleaseStringUTFChars(id, nId);
    env_->ReleaseStringUTFChars(command, nCommand);
    env_->ReleaseStringUTFChars(info, nInfo);
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_HandleAsync(JNIEnv *env, jobject me, const jint sender, const jstring id, const jstring command, const jstring info)
{
    env_ = env;
    me_ = me;
    const char* nId = env_->GetStringUTFChars(id, nullptr);
    const char* nCommand = env_->GetStringUTFChars(command, nullptr);
    const char* nInfo = env_->GetStringUTFChars(info, nullptr);
    cross::HandleAsync(sender, nId, nCommand, nInfo);
    env_->ReleaseStringUTFChars(id, nId);
    env_->ReleaseStringUTFChars(command, nCommand);
    env_->ReleaseStringUTFChars(info, nInfo);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_shaidin_cross_MainActivity_FeedUri(JNIEnv *env, jobject me, const jstring uri)
{
    env_ = env;
    me_ = me;
    jbyteArray jdata = nullptr;
    const char* nuri = env_->GetStringUTFChars(uri, nullptr);
    cross::FeedUri(nuri, [&](const std::vector<unsigned char>& data)
    {
        jdata = env->NewByteArray(data.size());
        env->SetByteArrayRegion(jdata, 0, data.size(), (jbyte*)data.data());
    });
    env_->ReleaseStringUTFChars(uri, nuri);
    return jdata;
}
