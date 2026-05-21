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

    public TileClickHook(ClassLoader classLoader) {
        Class<?> tileImplClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.tileimpl.QSTileImpl", classLoader);
        Class<?> expandableClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.animation.Expandable", classLoader);

        if (tileImplClass == null || expandableClass == null) {
            log("QSTileImpl 或 Expandable 类未找到");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(tileImplClass, "click",
                    expandableClass, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object tile = param.thisObject;
                                String tileSpec = (String) XposedHelpers.getObjectField(tile, "mTileSpec");

                                String targetPkg = extractPackageFromTileSpec(tileSpec);
                                if (targetPkg == null) {
                                    return;
                                }

                                Context context = (Context) XposedHelpers.getObjectField(tile, "mContext");
                                if (context == null) {
                                    return;
                                }

                                String logMsg = SDF.format(new Date()) + " TILE -> 磁贴被点击: " + tileSpec + ", 目标应用: " + targetPkg;
                                Intent intent = new Intent("nep.timeline.cirno.TILE_CLICK");
                                intent.putExtra("package_name", targetPkg);
                                intent.putExtra("log_msg", logMsg);
                                intent.setPackage("android");
                                context.sendBroadcast(intent);
                                XposedBridge.log(logMsg);
                            } catch (Throwable t) {
                                XposedBridge.log("TILE -> Hook 异常: " + t.getMessage());
                            }
                        }
                    });
            log("TileClickHook 注册成功");
        } catch (Throwable t) {
            XposedBridge.log("TILE -> Hook 注册失败: " + t);
            for (Method m : tileImplClass.getDeclaredMethods()) {
                if (m.getName().equals("click")) {
                    XposedBridge.log("TILE -> 可用签名: " + Arrays.toString(m.getParameterTypes()));
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

    private static void log(String msg) {
        XposedBridge.log(SDF.format(new Date()) + " TILE -> " + msg);
    }
}
