//
//  bridge-lib.cpp
//  cross
//
//  Created by Ali Asadpoor on 1/15/19.
//  Copyright © 2019 Shaidin. All rights reserved.
//

#include "../../../core/src/bridge.h"
#include "../../../core/src/interface.h"

#include <jni.h>

JavaVM* jvm_;
JNIEnv *env_;
jobject me_;
jobject tme_;
jmethodID need_restart_;
jmethodID load_web_view_;
jmethodID load_image_view_;
jmethodID refresh_image_view_;
jmethodID call_function_;
jmethodID get_asset_;
jmethodID get_preference_;
jmethodID set_preference_;
jmethodID post_thread_message_;
jmethodID add_param_;
jmethodID post_http_;
jmethodID play_audio_;
jmethodID exit_;
jintArray j_pixels_;


jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jvm_ = vm;
    interface::Begin();
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
    interface::End();
}

void bridge::NeedRestart()
{
    env_->CallVoidMethod(me_, need_restart_);
}

void bridge::LoadWebView(const __int32_t sender, const __int32_t view_info, const char* html, const char* waves)
{
    jstring jHtml = env_->NewStringUTF(html);
    jstring jWaves = env_->NewStringUTF(waves);
    env_->CallVoidMethod(me_, load_web_view_, sender, view_info, jHtml, jWaves);
    env_->DeleteLocalRef(jHtml);
    env_->DeleteLocalRef(jWaves);
}

void bridge::LoadImageView(const __int32_t sender, const __int32_t view_info, const __int32_t image_width, const char* waves)
{
    jstring jWaves = env_->NewStringUTF(waves);
    env_->CallVoidMethod(me_, load_image_view_, sender, view_info, image_width, jWaves);
    env_->DeleteLocalRef(jWaves);
}

__uint32_t* bridge::GetPixels()
{
    j_pixels_ = (jintArray)env_->GetObjectField(tme_, env_->GetFieldID(env_->GetObjectClass(tme_), "pixels_", "[I"));
    return (__uint32_t*)env_->GetIntArrayElements(j_pixels_, nullptr);
}

void bridge::ReleasePixels(__uint32_t* const pixels)
{
    env_->ReleaseIntArrayElements(j_pixels_, (__int32_t*)pixels, JNI_COMMIT);
    env_->DeleteLocalRef(j_pixels_);
}

void bridge::RefreshImageView()
{
    env_->CallVoidMethod(me_, refresh_image_view_);
}

void bridge::CallFunction(const char* function)
{
    jstring jFunction = env_->NewStringUTF(function);
    env_->CallVoidMethod(me_, call_function_, jFunction);
    env_->DeleteLocalRef(jFunction);
}

std::string bridge::GetAsset(const char* key)
{
    jstring jKey = env_->NewStringUTF(key);
    jstring jValue = (jstring)env_->CallObjectMethod(me_, get_asset_, jKey);
    const char* szValue = env_->GetStringUTFChars(jValue, nullptr);
    std::string value = szValue;
    env_->ReleaseStringUTFChars(jValue, szValue);
    env_->DeleteLocalRef(jKey);
    env_->DeleteLocalRef(jValue);
    return value;
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

void bridge::PostThreadMessage(__int32_t receiver, const char* id, const char* command, const char* info)
{
    JNIEnv* env_;
    jvm_->AttachCurrentThread(&env_, nullptr);
    jstring jId = env_->NewStringUTF(id);
    jstring jCommand = env_->NewStringUTF(command);
    jstring jInfo = env_->NewStringUTF(info);
    env_->CallVoidMethod(tme_, post_thread_message_, receiver, jId, jCommand, jInfo);
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

void bridge::PostHttp(const __int32_t sender, const char* id, const char* command, const char *url)
{
    jstring jId = env_->NewStringUTF(id);
    jstring jCommand = env_->NewStringUTF(command);
    jstring jUrl = env_->NewStringUTF(url);
    env_->CallVoidMethod(me_, post_http_, sender, jId, jCommand, jUrl);
    env_->DeleteLocalRef(jId);
    env_->DeleteLocalRef(jCommand);
    env_->DeleteLocalRef(jUrl);
}

void bridge::PlayAudio(const __int32_t index)
{
    env_->CallVoidMethod(me_, play_audio_, index);
}

void bridge::Exit()
{
    env_->CallVoidMethod(me_, exit_);
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Create(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    tme_ = env_->NewGlobalRef(me);
    need_restart_ = env_->GetMethodID(env_->GetObjectClass(me_), "NeedRestart",
            "()V");
    load_web_view_ = env_->GetMethodID(env_->GetObjectClass(me_), "LoadWebView",
            "(IILjava/lang/String;Ljava/lang/String;)V");
    load_image_view_ = env_->GetMethodID(env_->GetObjectClass(me_), "LoadImageView",
            "(IIILjava/lang/String;)V");
    refresh_image_view_ = env_->GetMethodID(env_->GetObjectClass(me_), "RefreshImageView",
            "()V");
    call_function_ = env_->GetMethodID(env_->GetObjectClass(me_), "CallFunction",
            "(Ljava/lang/String;)V");
    get_asset_ = env_->GetMethodID(env_->GetObjectClass(me_), "GetAsset",
            "(Ljava/lang/String;)Ljava/lang/String;");
    get_preference_ = env_->GetMethodID(env_->GetObjectClass(me_), "GetPreference",
            "(Ljava/lang/String;)Ljava/lang/String;");
    set_preference_ = env_->GetMethodID(env_->GetObjectClass(me_), "SetPreference",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    post_thread_message_ = env_->GetMethodID(env_->GetObjectClass(me_), "PostThreadMessage",
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    add_param_ = env_->GetMethodID(env_->GetObjectClass(me_), "AddParam",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    post_http_ = env_->GetMethodID(env_->GetObjectClass(me_), "PostHttp",
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    play_audio_ = env_->GetMethodID(env_->GetObjectClass(me_), "PlayAudio",
            "(I)V");
    exit_= env_->GetMethodID(env_->GetObjectClass(me_), "Exit",
            "()V");
    interface::Create();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Destroy(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    interface::Destroy();
    env_->DeleteGlobalRef(tme_);
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Start(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    interface::Start();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Stop(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    interface::Stop();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Restart(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    interface::Restart();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Escape(JNIEnv *env, jobject me)
{
    env_ = env;
    me_ = me;
    interface::Escape();
}

extern "C" JNIEXPORT void JNICALL Java_com_shaidin_cross_MainActivity_Handle(JNIEnv *env, jobject me, const jstring id, const jstring command, const jstring info)
{
    env_ = env;
    me_ = me;
    const char* nId = env_->GetStringUTFChars(id, nullptr);
    const char* nCommand = env_->GetStringUTFChars(command, nullptr);
    const char* nInfo = env_->GetStringUTFChars(info, nullptr);
    interface::Handle(nId, nCommand, nInfo);
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
    interface::HandleAsync(sender, nId, nCommand, nInfo);
    env_->ReleaseStringUTFChars(id, nId);
    env_->ReleaseStringUTFChars(command, nCommand);
    env_->ReleaseStringUTFChars(info, nInfo);
}
