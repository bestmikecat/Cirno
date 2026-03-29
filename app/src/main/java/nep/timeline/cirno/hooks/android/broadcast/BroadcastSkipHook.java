package nep.timeline.cirno.hooks.android.broadcast;

import android.os.Build;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.ProcessService;
import nep.timeline.cirno.utils.SystemChecker;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class BroadcastSkipHook extends MethodHook {
    public BroadcastSkipHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.am.BroadcastSkipPolicy";
    }

    @Override
    public String getTargetMethod() {
        return "shouldSkipMessage";
    }

    @Override
    public Object[] getTargetParam() {
        // ✅ 返回空数组，让 Xposed 自动适配所有参数组合
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
            Log.d("=== shouldSkipMessage 调用 ===");
    Log.d("参数数量: " + param.args.length);
    for (int i = 0; i < param.args.length; i++) {
        Object arg = param.args[i];
        String typeName = arg == null ? "null" : arg.getClass().getName();
        Log.d("参数[" + i + "]: " + typeName);
        
        if (arg != null) {
            try {
                // 尝试列出该对象的所有字段
                Class<?> clazz = arg.getClass();
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Log.d("  字段: " + fieldName);
                }
            } catch (Exception e) {
                Log.d("  无法反射获取字段");
            }
        }
    }
    Log.d("返回值: " + param.getResult());
    Log.d("========================");
                try {
                    // ✅ 安全地获取返回值
                    Object result = param.getResult();
                    if (result != null) {
                        return;
                    }

                    // ✅ 安全地获取参数
                    if (param.args.length < 2) {
                        return;
                    }

                    Object filter = param.args[1];
                    if (filter == null) {
                        return;
                    }

                    Object receiver = XposedHelpers.getObjectField(filter, "receiverList");
                    if (receiver == null) {
                        return;
                    }

                    Object app = XposedHelpers.getObjectField(receiver, "app");
                    if (app == null) {
                        return;
                    }

                    ProcessRecord processRecord = ProcessService.getProcessRecord(app);
                    if (processRecord == null) {
                        return;
                    }

                    if (processRecord.isFrozen()) {
                        param.setResult("Skipping deliver [Cirno]: frozen process");
                    }
                } catch (Exception e) {
                    Log.e("BroadcastSkipHook 处理失败", e);
                }
            }
        };
    }

    @Override
    public void startHook() {
        try {
            // ✅ 检查 BroadcastSkipPolicy 是否存在
            Class<?> targetClass = XposedHelpers.findClass(getTargetClass(), classLoader);
            if (targetClass == null) {
                Log.w("BroadcastSkipPolicy 不存在");
                return;
            }

            // ✅ 遍历所有 shouldSkipMessage 方法，不管参数是什么
            boolean hooked = false;
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(getTargetMethod())) {
                    try {
                        XposedBridge.hookMethod(method, getTargetHook());
                        Log.i(method.getName() + " [参数: " + method.getParameterCount() + "] -> 成功Hook完毕!");
                        hooked = true;
                    } catch (Exception e) {
                        Log.w("Hook " + method.getName() + " [参数: " + method.getParameterCount() + "] 失败");
                    }
                }
            }

            if (!hooked) {
                Log.w("未能 Hook 任何 shouldSkipMessage 方法");
            }
        } catch (Throwable e) {
            Log.e(getTargetMethod() + " Hook 失败", e);
        }
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }

    @Override
    public boolean isIgnoreError() {
        // ✅ 忽略错误，允许部分失败
        return true;
    }
}