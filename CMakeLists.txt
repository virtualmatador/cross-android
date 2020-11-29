cmake_minimum_required(VERSION 3.13)

file(STRINGS "${CMAKE_CURRENT_SOURCE_DIR}/../../manifest.txt" manifest)
foreach(key_value ${manifest})
    string(REGEX MATCH "^[^=]+" key ${key_value})
    string(REPLACE "${key}=" "" value ${key_value})
    set(${key} "${value}")
endforeach()

string(TOLOWER ${cross_target} project_name)
project(${project_name} VERSION ${cross_version}.${cross_release})

include(ExternalProject)

file(GLOB sdk_tools_dir "/usr/local/dev/android-sdk/build-tools/*")
list(SORT sdk_tools_dir)
list(GET sdk_tools_dir -1 sdk_tools_dir)

file(GLOB sdk_platform_dir "/usr/local/dev/android-sdk/platforms/*")
list(SORT sdk_platform_dir)
list(GET sdk_platform_dir -1 sdk_platform_dir)

file(GLOB ndk_tools_dir "/usr/local/dev/android-sdk/ndk/*")
list(SORT ndk_tools_dir)
list(GET ndk_tools_dir -1 ndk_tools_dir)

string(REGEX MATCH "[0123456789]+$" sdk_version ${sdk_platform_dir})

file(GLOB_RECURSE asset_list "../../assets/*")
list(SORT asset_list)

list(APPEND abis "arm64-v8a" "armeabi-v7a" "x86_64" "x86")
foreach(ANDROID_ABI ${abis})
    set(lib_file "lib/${ANDROID_ABI}/libnative.so")
    ExternalProject_Add("${ANDROID_ABI}"
        CMAKE_ARGS
            -D CMAKE_TOOLCHAIN_FILE=${ndk_tools_dir}/build/cmake/android.toolchain.cmake
            -D ANDROID_ABI=${ANDROID_ABI}
            -D CMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR}
        SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lib
    )
    add_custom_command(OUTPUT "${lib_file}"
        DEPENDS "${ANDROID_ABI}"
        COMMAND ${CMAKE_COMMAND} -E make_directory "lib/${ANDROID_ABI}"
        COMMAND ${CMAKE_COMMAND} -E copy "lib${ANDROID_ABI}.so" "${lib_file}"
    )
    list(APPEND native_dep "${lib_file}")
endforeach()

list(APPEND icon_dirs mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi)
foreach(icon_dir ${icon_dirs})
    list(APPEND resources_list "${CMAKE_CURRENT_BINARY_DIR}/res/${icon_dir}/ic_launcher.png")
    if(icon_dir STREQUAL mipmap-mdpi)
        set(RESOLUTION 48)
    elseif(icon_dir STREQUAL mipmap-hdpi)
        set(RESOLUTION 72)
    elseif(icon_dir STREQUAL mipmap-xhdpi)
        set(RESOLUTION 96)
    elseif(icon_dir STREQUAL mipmap-xxhdpi)
        set(RESOLUTION 144)
    elseif(icon_dir STREQUAL mipmap-xxxhdpi)
        set(RESOLUTION 192)
    endif()
    add_custom_command(OUTPUT "res/${icon_dir}/ic_launcher.png"
        DEPENDS "../../icon.svg"
        COMMAND ${CMAKE_COMMAND} -E make_directory "res/${icon_dir}"
        COMMAND inkscape -e "res/${icon_dir}/ic_launcher.png" -w ${RESOLUTION} -h ${RESOLUTION} "${CMAKE_CURRENT_SOURCE_DIR}/../../icon.svg"
    )
endforeach()
list(APPEND resources_list "${CMAKE_CURRENT_BINARY_DIR}/res/layout/main.xml")

add_custom_target(app_png ALL DEPENDS "app.png")
add_custom_command(OUTPUT "app.png"
    DEPENDS "../../icon.svg"
    COMMAND inkscape -e "app.png" -w 512 -h 512 "${CMAKE_CURRENT_SOURCE_DIR}/../../icon.svg"
)

add_custom_target("${cross_identifier}" ALL DEPENDS "${cross_identifier}.apk")
add_custom_command(OUTPUT "${cross_identifier}.apk"
    DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/classes.dex ${CMAKE_CURRENT_BINARY_DIR}/AndroidManifest.xml ${CMAKE_CURRENT_BINARY_DIR}/assets.txt ${asset_list} ${resources_list} ${native_dep}
	COMMAND "${sdk_tools_dir}/aapt" package -f -m -F "unaligned.apk" -M "AndroidManifest.xml" -S "res" -A "${CMAKE_CURRENT_SOURCE_DIR}/../../assets/" -I "${sdk_platform_dir}/android.jar" --min-sdk-version 14 --target-sdk-version ${sdk_version}
	COMMAND "${sdk_tools_dir}/aapt" add "unaligned.apk" "classes.dex" ${native_dep}
	COMMAND "${sdk_tools_dir}/zipalign" -f 4 "unaligned.apk" "${cross_identifier}.apk"
    COMMAND ${CMAKE_COMMAND} -E remove -f "unaligned.apk"
	COMMAND "${sdk_tools_dir}/apksigner" sign --ks "/etc/apk-keystore/${CMAKE_PROJECT_NAME}/apk.keystore" --ks-pass file:"/etc/apk-keystore/${CMAKE_PROJECT_NAME}/pass" "${cross_identifier}.apk"
)
add_custom_command(OUTPUT classes.dex
    DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/java/com/shaidin/cross/MainActivity.java" "${CMAKE_CURRENT_BINARY_DIR}/java/com/shaidin/cross/R.java"
    COMMAND ${CMAKE_COMMAND} -E make_directory "java/com/shaidin/cross"
    COMMAND javac --release 8 -encoding utf8 -d "java" -classpath "${sdk_platform_dir}/android.jar" "java/com/shaidin/cross/MainActivity.java" "java/com/shaidin/cross/R.java"
    COMMAND cd "java" && "${sdk_tools_dir}/dx" --dex --output=../classes.dex .
)
configure_file("src/java/com/shaidin/cross/MainActivity.java" "java/com/shaidin/cross/MainActivity.java" COPYONLY)
add_custom_command(OUTPUT "java/com/shaidin/cross/R.java"
    DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/java/AndroidManifest.xml" ${resources_list}
    COMMAND ${CMAKE_COMMAND} -E make_directory "java/com/shaidin/cross"
	COMMAND "${sdk_tools_dir}/aapt" package -f -m -J "java" -M "java/AndroidManifest.xml" -S "res" -I "${sdk_platform_dir}/android.jar"
)
configure_file("AndroidManifest.xml.in" "java/AndroidManifest.xml" COPYONLY)
list(APPEND xml_commands
	-u /manifest/application/@android:label -v ${cross_target}
	-u /manifest/@package -v ${cross_identifier}
	-u /manifest/@android:versionCode -v ${cross_release}
	-u /manifest/@android:versionName -v ${cross_version}
)
if(cross_internet STREQUAL "true")
    list(APPEND xml_commands
        -s /manifest -t elem -n uses-permission-temp
        -i /manifest/uses-permission-temp -t attr -n android:name -v android.permission.INTERNET
        -r /manifest/uses-permission-temp -v uses-permission
    )
endif()
add_custom_command(OUTPUT AndroidManifest.xml
    DEPENDS AndroidManifest.xml.in ../../manifest.txt
    COMMAND xmlstarlet edit ${xml_commands} "${CMAKE_CURRENT_SOURCE_DIR}/AndroidManifest.xml.in" > AndroidManifest.xml
)
add_custom_command(OUTPUT "assets.txt"
    COMMAND ${CMAKE_COMMAND} -E echo ${asset_list} > assets.txt.tmp
    COMMAND ${CMAKE_COMMAND} -E compare_files assets.txt.tmp assets.txt || ${CMAKE_COMMAND} -E echo ${asset_list} > assets.txt
)
configure_file("res/layout/main.xml" "res/layout/main.xml" COPYONLY)
