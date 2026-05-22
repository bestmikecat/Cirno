package nep.timeline.cirno.hooks.systemui.tile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TileClickHook {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
    private static final String ACTION_TILE_CLICK = "nep.timeline.cirno.TILE_CLICK";

    public TileClickHook(ClassLoader classLoader) {
        Class<?> tileImplClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.tileimpl.QSTileImpl", classLoader);
        Class<?> expandableClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.animation.Expandable", classLoader);

        if (tileImplClass == null || expandableClass == null) {
            fallbackLog("QSTileImpl 或 Expandable 类未找到");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(tileImplClass, "click",
                    expandableClass, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object tile = param.thisObject;
                                Context context = (Context) XposedHelpers.getObjectField(tile, "mContext");
                                String tileSpec = (String) XposedHelpers.getObjectField(tile, "mTileSpec");
                                log(context, "d", null, "磁贴被点击: " + tileSpec);

                                String targetPkg = extractPackageFromTileSpec(tileSpec);
                                if (targetPkg == null) {
                                    log(context, "w", null, "非应用磁贴，跳过");
                                    return;
                                }

                                if (context == null) {
                                    fallbackLog("无法获取 Context");
                                    return;
                                }

                                log(context, "i", null, "目标应用: " + targetPkg);
                                log(context, "i", targetPkg, "已发送解冻广播");
                            } catch (Throwable t) {
                                fallbackLog("Hook 异常: " + t.getMessage());
                            }
                        }
                    });
            fallbackLog("TileClickHook 注册成功");
        } catch (Throwable t) {
            fallbackLog("Hook 注册失败: " + t);
            for (Method m : tileImplClass.getDeclaredMethods()) {
                if (m.getName().equals("click")) {
                    fallbackLog("可用签名: " + Arrays.toString(m.getParameterTypes()));
                }
            }
        }
    }

    private static String extractPackageFromTileSpec(String tileSpec) {
        if (tileSpec == null) return null;
        if (tileSpec.startsWith("custom(") && tileSpec.endsWith(")")) {
            String component = tileSpec.substring(7, tileSpec.length() - 1);
            ComponentName cn = ComponentName.unflattenFromString(component);
            if (cn != null) return cn.getPackageName();
        }
        return null;
    }

    private static void log(Context context, String level, String packageName, String msg) {
        String formatted = SDF.format(new Date()) + " TILE -> " + msg;
        if (context == null) {
            fallbackLog(msg);
            return;
        }
        Intent intent = new Intent("nep.timeline.cirno.TILE_CLICK");
        intent.putExtra("log_level", level);
        intent.putExtra("log_msg", formatted);
        if (packageName != null) {
            intent.putExtra("package_name", packageName);
        }
        intent.setPackage("android");
        try {
            context.sendBroadcast(intent);
        } catch (Throwable t) {
            fallbackLog(msg + " | 广播发送失败: " + t.getMessage());
            return;
        }
    }

    private static void fallbackLog(String msg) {
        try {
            String formatted = SDF.format(new Date()) + " TILE -> " + msg;
            XposedBridge.log(formatted);
        } catch (Throwable ignored) {
        }
    }
}
