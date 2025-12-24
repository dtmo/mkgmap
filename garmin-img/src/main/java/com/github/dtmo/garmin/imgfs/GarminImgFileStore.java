package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class GarminImgFileStore extends FileStore {
    private final GarminImgFileSystem garminImgFileSystem;

    public GarminImgFileStore(final GarminImgFileSystem garminImgFileSystem) {
        this.garminImgFileSystem = garminImgFileSystem;
    }

    @Override
    public String name() {
        return garminImgFileSystem + "/";
    }

    @Override
    public String type() {
        return "imgfs";
    }

    @Override
    public boolean isReadOnly() {
        return garminImgFileSystem.isReadOnly();
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
