package com.anubis.jnihook;

import java.lang.reflect.Method;

/** Reflection helpers for {@code JniHook.cpp}: JNI descriptors and declaring class slashes. */
public final class MethodUtils {

    private MethodUtils() {
    }

    public static String getDesc(Method method) {
        Class<?>[] params = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : params) {
            sb.append(signature(p));
        }
        sb.append(")");
        sb.append(signature(ret));
        return sb.toString();
    }

    /**
     * Class name usable with {@link android.content.Context#getClassLoader()}-style resolution;
     * {@code FindClass} expects slash separators (inner: {@code Outer$Inner} → {@code Outer$Inner}, no dots).
     */
    public static String getDeclaringClass(Method method) {
        return typeToSlashName(method.getDeclaringClass());
    }

    public static String getMethodName(Method method) {
        return method.getName();
    }

    private static String typeToSlashName(Class<?> c) {
        if (c.isArray()) {
            return "[" + typeToSlashName(c.getComponentType());
        }
        if (c.isPrimitive()) {
            return primitiveSig(c).substring(0, 1);
        }
        return binaryNameOf(c).replace('.', '/');
    }

    /** JNI-style dotted binary name (supports {@code Outer$Inner}, not canonical dot-inner). */
    private static String binaryNameOf(Class<?> c) {
        return c.getName();
    }

    private static String signature(Class<?> c) {
        if (c.isArray()) {
            return "[" + signature(c.getComponentType());
        }
        if (c.isPrimitive()) {
            return primitiveSig(c);
        }
        return "L" + binaryNameOf(c).replace('.', '/') + ";";
    }

    private static String primitiveSig(Class<?> p) {
        if (p == void.class) {
            return "V";
        }
        if (p == boolean.class) {
            return "Z";
        }
        if (p == byte.class) {
            return "B";
        }
        if (p == char.class) {
            return "C";
        }
        if (p == short.class) {
            return "S";
        }
        if (p == int.class) {
            return "I";
        }
        if (p == long.class) {
            return "J";
        }
        if (p == float.class) {
            return "F";
        }
        if (p == double.class) {
            return "D";
        }
        throw new IllegalArgumentException("Not primitive: " + p);
    }
}
