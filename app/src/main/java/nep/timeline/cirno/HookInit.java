package nep.timeline.cirno;

import java.io.File;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import nep.timeline.cirno.master.AndroidHooks;
import nep.timeline.cirno.master.SystemUIHooks;
import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.framework.XposedInstance;
import nep.timeline.cirno.services.StatusBinderHub;

public class HookInit extends XposedModule {
    private boolean systemUIHooksStarted;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        XposedInstance.setModule(this);
        CakeHooker.setXposedModule(this);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String packageName = param.getPackageName();
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String packageName = param.getPackageName();
        if (!"com.android.systemui".equals(packageName) || systemUIHooksStarted) {
            return;
        }

        systemUIHooksStarted = true;
        ClassLoader classLoader = GlobalVars.classLoader = param.getClassLoader();
        CakeHooker.setHostClassLoader(classLoader);

        try {
            SystemUIHooks.start(classLoader);
            StatusBinderHub.setSignal(StatusBinderHub.SIGNAL_SYSTEMUI_HOOK_READY, "1");
        } catch (Throwable throwable) {
            Log.e("Cirno (" + packageName + ") -> Hook failed", throwable);
        }
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        ClassLoader classLoader = GlobalVars.classLoader = param.getClassLoader();
        CakeHooker.setHostClassLoader(classLoader);

        try {
            File source = new File(GlobalVars.LOG_DIR, "current.log");
            File dest = new File(GlobalVars.LOG_DIR, "last.log");
            boolean ignoredDelete = dest.delete();
            boolean ignoredRename = source.renameTo(dest);
            AndroidHooks.start(classLoader);
            StatusBinderHub.setSignal(StatusBinderHub.SIGNAL_ANDROID_HOOK_READY, "1");
        } catch (Throwable throwable) {
            Log.e("Cirno (android) -> Hook failed", throwable);
        }
    }
}
