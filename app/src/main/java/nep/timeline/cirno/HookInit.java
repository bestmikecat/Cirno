package nep.timeline.cirno;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import nep.timeline.cirno.master.AndroidHooks;
import nep.timeline.cirno.master.SystemUIHooks;
import nep.timeline.cirno.services.StatusBinderHub;

public class HookInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageParam) {
        String packageName = packageParam.packageName;
        ClassLoader classLoader = GlobalVars.classLoader = packageParam.classLoader;

        if ("com.android.systemui".equals(packageName)) {
            try {
                SystemUIHooks.start(classLoader);
            } catch (Throwable throwable) {
                XposedBridge.log("Cirno (" + packageName + ") -> Hook failed:");
                XposedBridge.log(throwable);
            }
            return;
        }

        if (BuildConfig.APPLICATION_ID.equals(packageName)) {
            Class<?> globalVars = XposedHelpers.findClassIfExists(GlobalVars.class.getTypeName(), classLoader);

            if (globalVars == null) {
                XposedBridge.log(GlobalVars.TAG + " -> Failed to set module active.");
                return;
            }

            XposedHelpers.setStaticBooleanField(globalVars, "isModuleActive", true);
            XposedHelpers.setStaticIntField(globalVars, "XposedVersion", XposedBridge.getXposedVersion());
            return;
        }

        if (!packageName.equals("android"))
            return;

        try {
            File source = new File(GlobalVars.LOG_DIR, "current.log");
            File dest = new File(GlobalVars.LOG_DIR, "last.log");
            boolean ignoredDelete = dest.delete();
            boolean ignoredRename = source.renameTo(dest);
            AndroidHooks.start(classLoader);
            StatusBinderHub.setSignal(StatusBinderHub.SIGNAL_ANDROID_HOOK_READY, "1");
        } catch (Throwable throwable) {
            XposedBridge.log("Cirno (" + packageName + ") -> Hook failed:");
            XposedBridge.log(throwable);
        }
    }
}
