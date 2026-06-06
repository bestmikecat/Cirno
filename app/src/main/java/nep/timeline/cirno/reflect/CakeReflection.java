package nep.timeline.cirno.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

public final class CakeReflection {
    private CakeReflection() {
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldError(clazz.getName() + "#" + fieldName);
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getInt(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getBoolean(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            findField(clazz, fieldName).setBoolean(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        try {
            findField(clazz, fieldName).setInt(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            findField(clazz, fieldName).set(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            return findMethodBestMatch(clazz, methodName, args).invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            return findConstructorBestMatch(clazz, args).newInstance(args);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] != null ? args[i].getClass() : null;
        }
        return findMethodBestMatch(clazz, methodName, parameterTypes);
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] declared = method.getParameterTypes();
                if (declared.length != parameterTypes.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < declared.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    if (parameterType != null && !wrap(declared[i]).isAssignableFrom(wrap(parameterType))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
    }

    private static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] != null ? args[i].getClass() : null;
        }
        Class<?> current = clazz;
        while (current != null) {
            for (Constructor<?> constructor : current.getDeclaredConstructors()) {
                Class<?>[] declared = constructor.getParameterTypes();
                if (declared.length != parameterTypes.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < declared.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    if (parameterType != null && !wrap(declared[i]).isAssignableFrom(wrap(parameterType))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodError(clazz.getName() + "<init>");
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    public static XposedInterface.HookHandle findAndHookMethod(Class<?> clazz, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof CakeHooker.Callback callback)) {
            throw new IllegalArgumentException("no callback defined");
        }
        Object[] parameterTypes = stripCallback(parameterTypesAndCallback);
        Method method = findMethodBestMatch(clazz, methodName, resolveParameterTypes(classLoader, parameterTypes));
        return CakeHooker.hook(method, callback);
    }

    public static XposedInterface.HookHandle findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        return findAndHookMethod(findClass(className, classLoader), classLoader, methodName, parameterTypesAndCallback);
    }

    public static XposedInterface.HookHandle findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof CakeHooker.Callback callback)) {
            throw new IllegalArgumentException("no callback defined");
        }
        Object[] parameterTypes = stripCallback(parameterTypesAndCallback);
        Constructor<?> constructor = findConstructor(clazz, resolveParameterTypes(clazz.getClassLoader(), parameterTypes));
        return CakeHooker.hook(constructor, callback);
    }

    public static XposedInterface.HookHandle findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        return findAndHookConstructor(findClass(className, classLoader), parameterTypesAndCallback);
    }

    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Constructor<?> constructor = current.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "<init>");
    }

    private static Object[] stripCallback(Object[] parameterTypesAndCallback) {
        Object[] parameterTypes = new Object[parameterTypesAndCallback.length - 1];
        System.arraycopy(parameterTypesAndCallback, 0, parameterTypes, 0, parameterTypes.length);
        return parameterTypes;
    }

    private static Class<?>[] resolveParameterTypes(ClassLoader classLoader, Object[] parameterTypes) {
        Class<?>[] result = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Object type = parameterTypes[i];
            if (type instanceof Class<?> clazz) {
                result[i] = clazz;
            } else if (type instanceof String className) {
                result[i] = findClass(className, classLoader);
            } else {
                throw new IllegalArgumentException("parameter type must either be specified as Class or String");
            }
        }
        return result;
    }

    public static Object[] findParameterTypesOrDefault(Class<?> clazz, String methodName, Object... parameter) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameter.length > parameterTypes.length) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < parameter.length; i++) {
                Object expected = parameter[i];
                if (!java.util.Objects.equals(expected, expected instanceof String ? parameterTypes[i].getName() : parameterTypes[i])) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return parameterTypes;
            }
        }
        return parameter;
    }
}

