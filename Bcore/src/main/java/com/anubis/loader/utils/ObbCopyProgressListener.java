package com.anubis.loader.utils;

public interface ObbCopyProgressListener {
    void onProgress(int percent, long copiedBytes, long totalBytes, String currentFileName);
}
