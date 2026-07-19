package com.anubis.jnihook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.anubis.jnihook.jni.JniHook;

/**
 * Opens a {@link Class} for native hook wiring: tweaks {@code Class.accessFlags} when possible,
 * then calls {@link JniHook#setAccessible} on all declared methods and fields (recurses nested
 * classes). Matches the reference {@code Fixed-Everything.aar} behavior.
 */
public final class ReflectCore {

    private ReflectCore() {
    }

    public static void set(Class<?> clazz) {
        try {
            Field accessFlagsField = Class.class.getDeclaredField("accessFlags");
            accessFlagsField.setAccessible(true);
            int accessFlags = (Integer) accessFlagsField.get(clazz);
            accessFlagsField.set(clazz, Integer.valueOf(accessFlags | 1));
        } catch (Throwable tr) {
            tr.printStackTrace();
        }

        for (Method m : clazz.getDeclaredMethods()) {
            JniHook.setAccessible(clazz, m);
        }
        for (Field f : clazz.getDeclaredFields()) {
            JniHook.setAccessible(clazz, f);
        }
        for (Class<?> inner : clazz.getDeclaredClasses()) {
            set(inner);
        }
    }
}
