package nep.timeline.cirno.utils;

import java.lang.reflect.Method;
import java.util.Objects;

import nep.timeline.cirno.log.Log;

public class ReflectUtils {
    public static Object[] findParameterTypesOrDefault(Class<?> clazz, String methodName, Object... parameter) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();

                    if (parameter.length <= parameterTypes.length) {
                        boolean isCompatible = true;

                        for (int i = 0; i < parameter.length; i++) {
                            Object obj = parameter[i];

                            if (!Objects.equals(obj, obj instanceof String ? parameterTypes[i].getName() : parameterTypes[i])) {
                                isCompatible = false;
                                break;
                            }
                        }

                        if (isCompatible) {
                            return parameterTypes;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Find parameter", e);
        }

        return parameter;
    }
}
