package nep.timeline.cirno.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ProcUtils {
    private ProcUtils() {}

    public static String readWchan(int pid) {
        if (pid <= 0)
            return null;

        File file = new File("/proc/" + pid + "/wchan");
        if (!file.exists() || !file.canRead())
            return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line == null ? null : line.trim();
        } catch (IOException ignored) {
            return null;
        }
    }
}
