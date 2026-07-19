# JNI / native entry points
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

-keep class com.anubis.loader.core.NativeCore { *; }
-keep class com.anubis.jnihook.** { *; }

-keep class com.anubis.reflection.** { *; }
-keep @com.anubis.reflection.annotation.BClass class * {*;}
-keep @com.anubis.reflection.annotation.BClassName class * {*;}
-keep @com.anubis.reflection.annotation.BClassNameNotProcess class * {*;}
-keepclasseswithmembernames class * {
    @com.anubis.reflection.annotation.BField.* <methods>;
    @com.anubis.reflection.annotation.BFieldNotProcess.* <methods>;
    @com.anubis.reflection.annotation.BFieldSetNotProcess.* <methods>;
    @com.anubis.reflection.annotation.BFieldCheckNotProcess.* <methods>;
    @com.anubis.reflection.annotation.BMethod.* <methods>;
    @com.anubis.reflection.annotation.BStaticField.* <methods>;
    @com.anubis.reflection.annotation.BStaticMethod.* <methods>;
    @com.anubis.reflection.annotation.BMethodCheckNotProcess.* <methods>;
    @com.anubis.reflection.annotation.BConstructor.* <methods>;
    @com.anubis.reflection.annotation.BConstructorNotProcess.* <methods>;
}

-keep class com.anubis.loader.entity.** { *; }
-keep class com.anubis.loader.core.system.pm.BPackage { *; }
-keep class com.anubis.loader.core.system.pm.BPackageUserState { *; }
-keep class com.anubis.loader.entity.pm.InstallOption { *; }
-keep class com.anubis.loader.proxy.** { *; }

-keep class com.anubis.loader.** { *; }
-keep class * implements android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }
-keepclassmembers class * {
    public static ** asInterface(android.os.IBinder);
}

-keep class mirror.** { *; }
-keep class black.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
