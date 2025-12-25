package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class DskimgFileStore extends FileStore {
    private final DskimgFileSystem dskimgFileSystem;

    public DskimgFileStore(final DskimgFileSystem dskimgFileSystem) {
        this.dskimgFileSystem = dskimgFileSystem;
    }

    @Override
    public String name() {
        return dskimgFileSystem + "/";
    }

    @Override
    public String type() {
        return "imgfs";
    }

    @Override
    public boolean isReadOnly() {
        return dskimgFileSystem.isReadOnly();
    }

    @Override
    public long getTotalSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalSpace'");
    }

    @Override
    public long getUsableSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUsableSpace'");
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUnallocatedSpace'");
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportsFileAttributeView'");
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportsFileAttributeView'");
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStoreAttributeView'");
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttribute'");
    }
    
}
