# JSch uses reflection for optional crypto providers.
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep line numbers readable in crash reports without disabling obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
