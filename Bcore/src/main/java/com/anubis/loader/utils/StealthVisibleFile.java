package com.anubis.loader.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;

/**
 * File whose {@link #getAbsolutePath()} looks like a normal guest install while I/O uses the real vfs tree.
 */
public final class StealthVisibleFile extends File {

    private final File mReal;

    public StealthVisibleFile(File real, String visibleAbsolutePath) {
        super(visibleAbsolutePath);
        mReal = real;
    }

    public static StealthVisibleFile wrap(File real, String visibleAbsolutePath) {
        if (real == null || visibleAbsolutePath == null) {
            return null;
        }
        return new StealthVisibleFile(real, visibleAbsolutePath);
    }

    public File realFile() {
        return mReal;
    }

    @Override
    public String getAbsolutePath() {
        return super.getAbsolutePath();
    }

    @Override
    public String getPath() {
        return super.getPath();
    }

    @Override
    public String toString() {
        return getAbsolutePath();
    }

    @Override
    public File getParentFile() {
        String parent = getParent();
        if (parent == null) {
            return null;
        }
        File realParent = mReal.getParentFile();
        return realParent != null ? new StealthVisibleFile(realParent, parent) : new File(parent);
    }

    @Override
    public boolean exists() {
        return mReal.exists();
    }

    @Override
    public boolean isDirectory() {
        return mReal.isDirectory();
    }

    @Override
    public boolean isFile() {
        return mReal.isFile();
    }

    @Override
    public long length() {
        return mReal.length();
    }

    @Override
    public long lastModified() {
        return mReal.lastModified();
    }

    @Override
    public boolean canRead() {
        return mReal.canRead();
    }

    @Override
    public boolean canWrite() {
        return mReal.canWrite();
    }

    @Override
    public boolean mkdir() {
        return mReal.mkdir();
    }

    @Override
    public boolean mkdirs() {
        return mReal.mkdirs();
    }

    @Override
    public boolean createNewFile() throws IOException {
        return mReal.createNewFile();
    }

    @Override
    public boolean delete() {
        return mReal.delete();
    }

    @Override
    public String[] list() {
        return mReal.list();
    }

    @Override
    public String[] list(FilenameFilter filter) {
        return mReal.list(filter);
    }

    @Override
    public File[] listFiles() {
        return wrapChildren(mReal.listFiles());
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        return wrapChildren(mReal.listFiles(filter));
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        return wrapChildren(mReal.listFiles(filter));
    }

    private File[] wrapChildren(File[] children) {
        if (children == null) {
            return null;
        }
        File[] out = new File[children.length];
        String base = getAbsolutePath();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            out[i] = new StealthVisibleFile(child, base + "/" + child.getName());
        }
        return out;
    }

    @Override
    public URI toURI() {
        return mReal.toURI();
    }

    @Override
    public boolean equals(Object obj) {
        return mReal.equals(obj instanceof StealthVisibleFile ? ((StealthVisibleFile) obj).mReal : obj);
    }

    @Override
    public int hashCode() {
        return mReal.hashCode();
    }
}
