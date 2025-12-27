package com.github.dtmo.garmin.imgfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public record DskimgFileAttributes(boolean isRegularFile, long size) implements BasicFileAttributes {
    @Override
    public FileTime lastModifiedTime() {
        throw new UnsupportedOperationException("Unimplemented method 'lastModifiedTime'");
    }

    @Override
    public FileTime lastAccessTime() {
        throw new UnsupportedOperationException("Unimplemented method 'lastAccessTime'");
    }

    @Override
    public FileTime creationTime() {
        throw new UnsupportedOperationException("Unimplemented method 'creationTime'");
    }

    @Override
    public boolean isDirectory() {
        return !isRegularFile();
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public Object fileKey() {
        throw new UnsupportedOperationException("Unimplemented method 'fileKey'");
    }
}
