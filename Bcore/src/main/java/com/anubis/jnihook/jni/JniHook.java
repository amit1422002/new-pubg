package com.anubis.jnihook.jni;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * JNI shim for {@code JniHook.cpp} (offsets + RegisterNatives). Native code resolves this
 * legacy FQCN ({@code com/anubis/jnihook/jni/JniHook}).
 */
public final class JniHook {

    /** Keep these two ints adjacent; native layer measures ART static-field spacing. */
    public static int NATIVE_OFFSET = 0;
    public static int NATIVE_OFFSET_2 = 0;

    public static native void nativeOffset();

    public static native void nativeOffset2();

    public static native void setAccessible(Class<?> clazz, Method method);

    public static native void setAccessible(Class<?> clazz, Field field);

    private JniHook() {
    }
}

