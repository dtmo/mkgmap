package com.github.dtmo.garmin.imgfs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DskimgInputStream implements AutoCloseable {
    private XorInputStream xorInputStream;

    protected DskimgInputStream(final XorInputStream xorInputStream) {
        this.xorInputStream = xorInputStream;
    }

    public MasterBootRecord readMasterBootRecord() throws IOException {
        final byte[] masterBootRecordBytes = xorInputStream.readNBytes(512);
        final MasterBootRecord masterBootRecord = MasterBootRecord.read(ByteBuffer.wrap(masterBootRecordBytes));
        return masterBootRecord;
    }

    public static DskimgInputStream create(final InputStream inputStream) throws IOException {
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        bufferedInputStream.mark(16);
        final byte xorByte = (byte) bufferedInputStream.read();
        bufferedInputStream.reset();
        return new DskimgInputStream(new XorInputStream(bufferedInputStream, xorByte));
    }

    @Override
    public void close() throws Exception {
        xorInputStream.close();
    }
}