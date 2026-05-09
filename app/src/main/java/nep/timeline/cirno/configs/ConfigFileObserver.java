package nep.timeline.cirno.configs;

import android.os.FileObserver;
import android.os.Handler;

import java.io.File;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.services.ConfigBinderHub;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.log.Log;

public class ConfigFileObserver extends FileObserver {
    private static final String TARGET_FILE = "ApplicationSettings.json";

    public ConfigFileObserver() {
        super(GlobalVars.CONFIG_DIR, FileObserver.DELETE | FileObserver.DELETE_SELF | FileObserver.MODIFY | FileObserver.MOVE_SELF);
        reInit();
        ConfigBinderHub.readConfigSynchronized();
    }

    @Override
    public void onEvent(int event, String path) {
        if (path == null) return;
        Log.d("配置监听：EVENT " + event + "Path " + path);
        Handler handler = Handlers.config;
        handler.removeCallbacksAndMessages(null);
        switch (event & FileObserver.ALL_EVENTS) {
            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF: {
                handler.postDelayed(() -> {
                    ConfigBinderHub.readConfigSynchronized();
                    reInit();
                }, 2000);
                Log.d("配置目录被删除");
                break;
            }
            case FileObserver.MODIFY:
            case FileObserver.MOVE_SELF: {
                if (!TARGET_FILE.equals(path)) break;
                handler.postDelayed(ConfigBinderHub::readConfigSynchronized, 2000);
                Log.d("配置热更新：配置目录被修改");
            }
        }
    }

    public void reInit() {
        File configDir = new File(GlobalVars.CONFIG_DIR);
        if (!configDir.exists())
            configDir.mkdir();
        File logDir = new File(GlobalVars.LOG_DIR);
        if (!logDir.exists())
            logDir.mkdir();
    }
}