package com.github.dtmo.garmin.imgfs;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XorInputStream extends FilterInputStream {
    private final byte xorByte;

    protected XorInputStream(final InputStream inputStream, final byte xorByte) {
        super(inputStream);
        this.xorByte = xorByte;
    }

    @Override
    public int read() throws IOException {
        return super.read() ^ xorByte;
    }

    @Override
    public int read(final byte[] bytes) throws IOException {
        return this.read(bytes, 0, bytes.length);
    }

    @Override
    public int read(final byte[] bytes, final int offset, final int length) throws IOException {
        final int bytesRead = super.read(bytes, offset, length);

        for (int i = offset; i < bytesRead; i++) {
            bytes[i] = (byte) (bytes[i] ^ xorByte);
        }

        return bytesRead;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return this.readNBytes(Integer.MAX_VALUE);
    }

    @Override
    public byte[] readNBytes(final int length) throws IOException {
        final byte[] bytes = super.readNBytes(length);

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ xorByte);
        }

        return bytes;
    }

    @Override
    public int readNBytes(final byte[] bytes, final int offset, final int length) throws IOException {
        final int bytesRead = super.readNBytes(bytes, offset, length);

        for (int i = offset; i < bytesRead; i++) {
            bytes[i] = (byte) (bytes[i] ^ xorByte);
        }

        return bytesRead;
    }
}
