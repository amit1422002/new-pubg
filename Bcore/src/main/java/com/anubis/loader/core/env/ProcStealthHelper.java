package com.anubis.loader.core.env;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Disabled — fake /proc mirroring and maps scrubbing are intentionally off.
 */
public final class ProcStealthHelper {

    private ProcStealthHelper() {
    }

    public static void writeCmdline(File procDir, String processName) throws IOException {
    }

    public static void writeComm(File procDir, String processName) throws IOException {
    }

    public static void writeSanitizedMaps(File procDir) {
    }

    public static void writeExtendedProcFiles(File procDir) {
    }

    public static void refreshSanitizedMapsForCurrentProcess() {
    }

    public static void refreshMapsOnlyForCurrentProcess() {
    }

    static List<String> filterMapsLines(List<String> lines) {
        return lines != null ? lines : Collections.emptyList();
    }

    static boolean shouldHideMapsLine(String line) {
        return false;
    }

    static boolean shouldHideProcContentLine(String line) {
        return false;
    }

    static String sanitizeProcContentLine(String line) {
        return line != null ? line : "";
    }
}
