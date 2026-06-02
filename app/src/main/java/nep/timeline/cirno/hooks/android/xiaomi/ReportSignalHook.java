package nep.timeline.cirno.hooks.android.xiaomi;

import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.BinderService;
import nep.timeline.cirno.services.MonitorBinderHub;
import nep.timeline.cirno.services.ProcessService;
import nep.timeline.cirno.utils.SystemChecker;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class ReportSignalHook extends MethodHook {
    public ReportSignalHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.miui.server.greeze.GreezeManagerService";
    }

    @Override
    public String getTargetMethod() {
        return "reportSignal";
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            return new Object[]{int.class, int.class, long.class, int.class};
        return new Object[]{int.class, int.class, long.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                if (BinderService.received) {
                    unhook();
                    return;
                }

                int pid = (int) param.args[1];
                if (pid <= 0)
                    return;

                ProcessRecord processRecord = ProcessService.getProcessRecordByPid(pid);
                if (processRecord == null)
                    return;

                AppRecord appRecord = ProcessService.removeProcessRecordWithoutThaw(processRecord);
                if (appRecord != null)
                    MonitorBinderHub.refreshRunningApps();
                Log.i(processRecord.getPackageName() + " 收到小米信号(pid=" + pid + ")，移除进程记录并跳过解冻");
            }
        };
    }

    @Override
    public boolean isIgnoreError() {
        return !SystemChecker.isXiaomi(classLoader);
    }
}
