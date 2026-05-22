package nep.timeline.cirno.hooks.systemui.tile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;

public class TileClickHook extends MethodHook {
    private static final String TARGET_CLASS = "com.android.systemui.qs.tileimpl.QSTileImpl";
    private static final String TARGET_METHOD = "click";
    private static final String EXPANDABLE_CLASS = "com.android.systemui.animation.Expandable";
    private static final String ACTION_TILE_CLICK = "nep.timeline.cirno.TILE_CLICK";

    public TileClickHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return TARGET_CLASS;
    }

    @Override
    public String getTargetMethod() {
        return TARGET_METHOD;
    }

    @Override
    public Object[] getTargetParam() {
        Class<?> expandableClass = XposedHelpers.findClassIfExists(EXPANDABLE_CLASS, classLoader);
        if (expandableClass == null) {
            Log.e("QSTileImpl -> Expandable 类未找到");
            return null;
        }
        return new Object[]{expandableClass};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                try {
                    Object tile = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(tile, "mContext");
                    String tileSpec = (String) XposedHelpers.getObjectField(tile, "mTileSpec");
                    Log.d("磁贴被点击: " + tileSpec);

                    String targetPkg = extractPackageFromTileSpec(tileSpec);
                    if (targetPkg == null) {
                        Log.w("非应用磁贴，跳过");
                        return;
                    }

                    if (context == null) {
                        Log.w("无法获取 Context");
                        return;
                    }

                    Log.i("目标应用: " + targetPkg);
                    sendTileClickBroadcast(context, targetPkg);
                } catch (Throwable t) {
                    Log.e("Hook 异常", t);
                }
            }
        };
    }

    private void sendTileClickBroadcast(Context context, String packageName) {
        Intent intent = new Intent(ACTION_TILE_CLICK);
        intent.putExtra("package_name", packageName);
        intent.setPackage("android");
        try {
            context.sendBroadcast(intent);
            Log.i("已发送解冻广播");
        } catch (Throwable t) {
            Log.e("广播发送失败", t);
        }
    }

    private static String extractPackageFromTileSpec(String tileSpec) {
        if (tileSpec == null) {
            return null;
        }
        if (tileSpec.startsWith("custom(") && tileSpec.endsWith(")")) {
            String component = tileSpec.substring(7, tileSpec.length() - 1);
            ComponentName cn = ComponentName.unflattenFromString(component);
            if (cn != null) {
                return cn.getPackageName();
            }
        }
        return null;
    }
}
