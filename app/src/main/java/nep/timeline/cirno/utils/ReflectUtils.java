package nep.timeline.cirno.utils;

import java.lang.reflect.Method;
import java.util.Objects;

import nep.timeline.cirno.log.Log;

public class ReflectUtils {
    public static Object[] findParameterTypesOrDefault(Class<?> clazz, String methodName, Object... parameter) {
        try {
            boolean foundAny = false;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    foundAny = true;
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Log.i("[ReflectUtils-DEBUG] " + methodName + " 候选: " + java.util.Arrays.toString(parameterTypes));

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
                            Log.i("[ReflectUtils-DEBUG] " + methodName + " 匹配成功, 参数数量: " + parameterTypes.length);
                            return parameterTypes;
                        }
                    }
                }
            }
            if (!foundAny) {
                Log.w("[ReflectUtils-DEBUG] " + methodName + " 未找到任何同名方法");
            }
        } catch (Exception e) {
            Log.e("Find parameter", e);
        }

        return parameter;
    }
}
