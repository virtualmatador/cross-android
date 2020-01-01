include ../../manifest.txt
bundle_path := $(subst .,/,$(cross_identifier))
sdk_tools_dir := $(shell ls -d ~/Android/Sdk/build-tools/* | tail -n 1)
sdk_platforms_dir := $(shell ls -d ~/Android/Sdk/platforms/* | tail -n 1)
jre_dir := ~/android-studio/jre
java_sources := $(wildcard src/java/com/shaidin/cross/*.java)
assets := $(shell find -L assets/ -type f)
ifeq ($(wildcard src/java/com/shaidin/cross/R.java),)
	java_sources := $(java_sources) src/java/com/shaidin/cross/R.java
endif

ifneq ($(MAKECMDGOALS),clean)
	native_libs := lib/arm64-v8a/libnative-lib.so lib/armeabi-v7a/libnative-lib.so lib/x86/libnative-lib.so lib/x86_64/libnative-lib.so
endif
ndk_path := $(shell ls -d ~/Android/Sdk/ndk/* | tail -n 1)
CC := $(ndk_path)/toolchains/llvm/prebuilt/linux-x86_64/bin/clang
CXX := $(ndk_path)/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++
sysroot := $(ndk_path)/toolchains/llvm/prebuilt/linux-x86_64/sysroot
abi_armeabi-v7a := armv7a-linux-androideabi
abi_arm64-v8a := aarch64-linux-android
abi_x86 := i686-linux-android
abi_x86_64 := x86_64-linux-android
lib_version := $(shell ls -d $(ndk_path)/platforms/* | tail -n 1 | sed 's/.*\/android-//')
core_objects := $(patsubst ../core/src/%.cpp, %.o, $(wildcard ../core/src/*.cpp))
main_objects := $(patsubst ../../src/%.cpp, %.o, $(wildcard ../../src/*.cpp))
bridge_object := bridge-lib.o

.PHONY: run clean

bin/$(cross_identifier).apk: src/java/com/shaidin/cross/R.java classes.dex AndroidManifest.xml $(native_libs) $(assets)
	mkdir -p bin
	$(sdk_tools_dir)/aapt package -f -m -F bin/unaligned.apk -M AndroidManifest.xml -S res -A assets/ -I $(sdk_platforms_dir)/android.jar --min-sdk-version 21 --target-sdk-version $(lib_version)
	$(sdk_tools_dir)/aapt add bin/unaligned.apk classes.dex
	for native_lib in $(native_libs);do $(sdk_tools_dir)/aapt add bin/unaligned.apk $${native_lib}; done
	$(sdk_tools_dir)/zipalign -f 4 bin/unaligned.apk bin/aligned.apk
	rm -f bin/unaligned.apk
	export PATH="$$PATH:$(jre_dir)/bin" && $(sdk_tools_dir)/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android bin/aligned.apk
	mv bin/aligned.apk $@

AndroidManifest.xml: AndroidManifest.xml.in
	cp $^ $@
	xmlstarlet ed -L \
		-u "/manifest/application/@android:label" -v $(word 3,$(subst ., ,$(cross_identifier))) \
		-u "/manifest/application/@android:versionCode" -v $(cross_release_number) \
		-u "/manifest/application/@android:versionName" -v $(cross_version) \
		$@
ifeq ($(cross_internet), true)
	xmlstarlet ed -L \
		-s "/manifest" -t elem -n uses-permission-temp \
		-i /manifest/uses-permission-temp -t attr -n android:name -v android.permission.INTERNET \
		-r /manifest/uses-permission-temp -v uses-permission \
		$@
endif

classes.dex: obj/
	cd obj && export PATH="$$PATH:$(jre_dir)/bin" && $(sdk_tools_dir)/dx --min-sdk-version=26 --dex --output=../$@ .

obj/: $(java_sources) AndroidManifest.xml
	mkdir -p _$@
	$(jre_dir)/bin/javac -d _$@ -classpath $(sdk_platforms_dir)/android.jar $(java_sources)
	rm -rf $@
	mv _$@ $@

src/java/com/shaidin/cross/R.java: AndroidManifest.xml res
	$(sdk_tools_dir)/aapt package -f -m -J src/java -M  $(word 1, $^) -S  $(word 2, $^) -I $(sdk_platforms_dir)/android.jar

define NATIVE_COMPILE_RULE
$(1)$(2)$(3)$(subst $() \,,$(shell $(CXX) --sysroot=$(sysroot) --target=$(abi_$(2))$(lib_version) -std=c++14 -MM $(4)$(5:.o=.cpp)))
	mkdir -p $(1)$(2)$(3)
	$(CXX) --sysroot=$(sysroot) --target=$(abi_$(2))$(lib_version) -std=c++14 -fPIC -c -o $$@ $$<
endef
define NATIVE_LINK_RULE
$(1)/$(2)/$(3): $(addprefix build/$(2)/core/,$(core_objects)) $(addprefix build/$(2)/main/,$(main_objects)) build/$(2)/$(bridge_object)
	mkdir -p $(1)/$(2)
	$(CXX) --sysroot=$(sysroot) --target=$(abi_$(2))$(lib_version) -std=c++14 -shared -static-libstdc++ -o $$@ $$^
$(foreach obj, $(core_objects), $(eval $(call NATIVE_COMPILE_RULE,build/,$(2),/core/,../core/src/,$(obj))))
$(foreach obj, $(main_objects), $(eval $(call NATIVE_COMPILE_RULE,build/,$(2),/main/,../../src/,$(obj))))
$(eval $(call NATIVE_COMPILE_RULE,build/,$(2),/,src/cpp/,$(bridge_object)))
endef
$(foreach native_lib, $(native_libs), $(eval $(call NATIVE_LINK_RULE,$(word 1,$(subst /, ,$(native_lib))),$(word 2,$(subst /, ,$(native_lib))),$(word 3,$(subst /, ,$(native_lib))))))

run:
	~/Android/Sdk/emulator/emulator -avd Pixel_2_API_29 > /dev/null 2>&1 &
	~/Android/Sdk/platform-tools/adb wait-for-device
	~/Android/Sdk/platform-tools/adb install bin/$(cross_identifier).apk
	~/Android/Sdk/platform-tools/adb logcat -b all -c
	~/Android/Sdk/platform-tools/adb shell am start -n com.shaidin.cross/.MainActivity
	~/Android/Sdk/platform-tools/adb logcat | grep cross-app

clean:
	rm -rf _obj/ obj/ bin/ lib/ build/
	rm -f classes.dex
	rm -f AndroidManifest.xml
	rm -f src/com/shaidin/cross/R.java
