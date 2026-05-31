package nep.timeline.cirno.services;

import android.system.ErrnoException;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.netlink.NetlinkClient;
import nep.timeline.cirno.netlink.NetlinkSocketAddress;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.utils.FrozenRW;
import nep.timeline.cirno.utils.StringUtils;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class BinderService {
    private final static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final int NETLINK_UNIT_DEFAULT = 22;
    private static final int NETLINK_UNIT_MAX = 26;
    private static final long TEMP_UNFREEZE_INTERVAL_MS = 3000L;
    public static volatile boolean received = false;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private static Map<String, String> parseParams(String message) {
        Map<String, String> map = new HashMap<>();
        for (String keyValue : message.split(",")) {
            String[] split = keyValue.split("=");
            if (split.length == 2)
                map.put(split[0].trim(), split[1].trim());
        }

        return map;
    }

    public static void start(ClassLoader classLoader) {
        if (!isRunning.compareAndSet(false, true))
            return;

        executorService.execute(() -> {
            try {
                int netlinkUnit;
                int configNetlinkUnit = GlobalVars.globalSettings == null ? 0 : GlobalVars.globalSettings.netlinkUnit;
                if (configNetlinkUnit >= NETLINK_UNIT_DEFAULT && configNetlinkUnit <= NETLINK_UNIT_MAX) {
                    netlinkUnit = configNetlinkUnit;
                } else {
                    File dir = new File("/proc/rekernel");
                    if (dir.exists()) {
                        File[] files = dir.listFiles();
                        if (files == null) {
                            Log.w("找不到ReKernel单元");
                            isRunning.set(false);
                            return;
                        }
                        File unitFile = files[0];
                        netlinkUnit = StringUtils.StringToInteger(unitFile.getName());
                    } else netlinkUnit = NETLINK_UNIT_DEFAULT;
                }

                try (NetlinkClient netlinkClient = new NetlinkClient(classLoader, netlinkUnit)) {
                    if (!netlinkClient.getMDescriptor().valid()) {
                        Log.w("无法连接至ReKernel服务器");
                        isRunning.set(false);
                        return;
                    }

                    netlinkClient.bind((SocketAddress) new NetlinkSocketAddress(100).toInstance(classLoader));

                    try {
                        byte[] message = "#proc_remove\0".getBytes(StandardCharsets.UTF_8);
                        byte[] bytes = new byte[16 + message.length];
                        ByteBuffer procRemoveBuf = ByteBuffer.wrap(bytes);
                        procRemoveBuf.order(ByteOrder.nativeOrder());
                        procRemoveBuf.putInt(bytes.length);
                        procRemoveBuf.putShort((short) 0x11);
                        procRemoveBuf.putShort((short) 0x1);
                        procRemoveBuf.putInt(1);
                        procRemoveBuf.putInt(100);
                        procRemoveBuf.put(message);
                        netlinkClient.sendMessage(bytes, 0, bytes.length);
                    } catch (Throwable ignored) {
                    }

                    Log.i("已连接至ReKernel, " + netlinkUnit + "#100");

                    while (true) {
                        try {
                            ByteBuffer byteBuffer = netlinkClient.recvMessage();
                            String data = new String(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit(), StandardCharsets.UTF_8);
                            if (!data.isEmpty()) {
                                Map<String, String> params = parseParams(data.substring(data.indexOf("type"), data.lastIndexOf(";")));
                                if (params.containsKey("type") && !received) {
                                    Log.i("成功接收到来自ReKernel的消息");
                                    received = true;
                                }
                                Handlers.rekernel.post(() -> {
                                    String type = params.get("type");
                                    if (type == null)
                                        return;

                                    switch (type) {
                                        case "Binder" -> {
                                            String bindertype = params.get("bindertype");
                                            int oneway = StringUtils.StringToInteger(params.get("oneway"));
                                            int targetUid = StringUtils.StringToInteger(params.get("target"));
                                            int fromPid = StringUtils.StringToInteger(params.get("from_pid"));
                                            String rpcName = params.get("rpc_name");
                                            int code = StringUtils.StringToInteger(params.get("code"));
                                            if (oneway == 1 && !bindertype.equals("free_buffer_full"))
                                                return;

                                            List<AppRecord> appRecords = AppService.getByUid(targetUid);
                                            if (appRecords.isEmpty())
                                                return;
                                            for (AppRecord appRecord : appRecords) {
                                                if (appRecord == null)
                                                    continue;

                                                FreezerService.temporaryUnfreezeIfNeed(appRecord, "内核Binder(" + (oneway == 1 ? "ASYNC" : "SYNC") + "), 类型: " + bindertype, TEMP_UNFREEZE_INTERVAL_MS);
                                            }
                                        }
                                        case "Signal" -> {
                                            int dstPid = StringUtils.StringToInteger(params.get("dst_pid"));
                                            int signal = StringUtils.StringToInteger(params.get("signal"));
                                            if (dstPid <= 0)
                                                return;
                                            ProcessRecord processRecord = ProcessService.getProcessRecordByPid(dstPid);
                                            if (processRecord == null)
                                                return;
                                            AppRecord appRecord = processRecord.getAppRecord();
                                            if (appRecord == null)
                                                return;
                                            FreezerHandler.removeAppMessage(appRecord);
                                            Log.i(appRecord.getPackageNameWithUser() + " 收到信号 " + signal + "(pid=" + dstPid + ")，移除待冻结任务");
                                        }
                                        case "Network" -> {
                                            int targetUid = StringUtils.StringToInteger(params.get("target"));
                                            String proto = params.get("proto");
                                            List<AppRecord> appRecords = AppService.getByUid(targetUid);
                                            if (appRecords.isEmpty())
                                                return;
                                            for (AppRecord appRecord : appRecords) {
                                                if (appRecord == null)
                                                    continue;

                                                boolean networkMessageAllowed = AppConfigs.isNetworkMessageAllowed(
                                                    appRecord.getPackageName(),
                                                    appRecord.getUserId()
                                                );
                                                if (!networkMessageAllowed)
                                                    continue;

                                                FreezerService.temporaryUnfreezeIfNeed(appRecord, "内核Network(" + proto + ")", TEMP_UNFREEZE_INTERVAL_MS);
                                            }
                                        }
                                    }
                                });
                            }
                        } catch (ErrnoException | InterruptedIOException |
                                 NumberFormatException ignored) {

                        } catch (Exception e) {
                            Log.e("ReKernel", e);
                        }
                    }
                }
            } catch (ErrnoException | IOException e) {
                Log.w("无法连接至ReKernel服务器");
                isRunning.set(false);
            } catch (Throwable throwable) {
                Log.w("ReKernel", throwable);
                isRunning.set(false);
            }
        });
    }
}
