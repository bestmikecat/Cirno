package nep.timeline.cirno.utils;

import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;
import com.topjohnwu.superuser.io.SuFileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;

public class RWUtils {
    public static String readConfig(SuFile file) {
        try {
            return IOUtils.toString(() -> SuFileInputStream.open(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("Read Config", e);
        }

        return null;
    }

    public static String readConfig(String name) {
        try {
            return String.join("\n", FileUtils.readLines(new File(name), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("Read Config", e);
        }

        return "";
    }

    public static void writeStringToFile(File file, String value) throws IOException {
        writeStringToFile(file, value + "\n", false);
    }

    public static void writeStringToFile(File file, String value, boolean append) throws IOException {
        FileUtils.write(file, value + "\n", StandardCharsets.UTF_8, append);
    }

    public static void writeStringToFileSU(SuFile file, String value, boolean append) throws IOException {
        try (PrintWriter writer = new PrintWriter(SuFileOutputStream.open(file), append)) {
            writer.write(value);
        }
    }

    public static boolean writeFrozen(String path, int value) {
        try (PrintWriter writer = new PrintWriter(path)) {
            writer.write(Integer.toString(value));
            return true;
        } catch (FileNotFoundException ignored) {
            String label = "";
            Matcher m = Pattern.compile("uid_(\\d+)").matcher(path);
            if (m.find()) {
                int uid = Integer.parseInt(m.group(1));
                List<AppRecord> records = AppService.getByUid(uid);
                if (!records.isEmpty()) {
                    label = " [" + records.get(0).getPackageNameWithUser() + "]";
                }
            }
            Log.w(path + " | 文件不存在" + label + ", 此进程可能已死亡, 或者你的设备不支持cgroup v2");
            return false;
        }
    }
}
