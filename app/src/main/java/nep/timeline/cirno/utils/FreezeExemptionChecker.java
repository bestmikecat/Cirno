package nep.timeline.cirno.utils;

import nep.timeline.cirno.CommonConstants;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.configs.policy.FreezeExemption;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;

public class FreezeExemptionChecker {
    /**
     * 检查应用是否豁免于冻结。
     * 返回豁免原因，null 表示应被冻结。
     *
     * 优先级：已冻结 → 等待通知 → 前台 → 黑名单(不豁免) → 白名单 → 输入法 → 系统应用 → 内建白名单 → 能力豁免
     */
    public static FreezeExemption check(AppRecord appRecord) {
        if (appRecord == null || appRecord.isFrozen()) {
            return null;
        }

        AppState appState = appRecord.getAppState();
        String pkg = appRecord.getPackageName();
        int userId = appRecord.getUserId();

        if (appRecord.isWaitingNotification()) {
            return FreezeExemption.WAITING_PUSH_RESPONSE;
        }

        if (appState != null && appState.isVisible()) {
            return FreezeExemption.VISIBLE;
        }

        if (AppConfigs.isBlackApp(pkg, userId)) {
            return null;
        }

        if (AppConfigs.isWhiteApp(pkg, userId)) {
            return FreezeExemption.WHITELIST;
        }

        if (appRecord.equals(InputMethodData.currentInputMethodApp)) {
            return FreezeExemption.INPUT;
        }

        if (PKGUtils.isSystemApp(appRecord.getApplicationInfo())) {
            return FreezeExemption.SYSTEM;
        }

        if (CommonConstants.isWhitelistApps(pkg)) {
            return FreezeExemption.WHITELIST;
        }

        if (appState != null) {
            if (AppConfigs.isBackgroundPlayAllowed(pkg, userId) && appState.isAudio()) {
                return FreezeExemption.AUDIO;
            }
            if (AppConfigs.isLocationUseAllowed(pkg, userId) && appState.isLocation()) {
                return FreezeExemption.LOCATION;
            }
            if (AppConfigs.isRecordingAllowed(pkg, userId) && appState.isRecording()) {
                return FreezeExemption.RECORDING;
            }
            if (appState.isVpn()) {
                return FreezeExemption.VPN;
            }
            if (AppConfigs.isNetworkSpeedAllowed(pkg, userId) && appState.isNetworkActive()) {
                return FreezeExemption.NETWORK_SPEED;
            }
        }

        return null;
    }
}
