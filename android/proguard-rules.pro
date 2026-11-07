# Bixolon Label SDK: libbxl_common.so registers natives by class name (RegisterNatives) and calls
# back into Java, so the SDK classes and members must keep their names after R8.
-keep class com.bixolon.commonlib.** { *; }
-keep interface com.bixolon.commonlib.** { *; }
-keep class com.bixolon.labelprinter.** { *; }
-keep interface com.bixolon.labelprinter.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class com.bixolon.** {
    native <methods>;
}
-dontwarn com.bixolon.**
