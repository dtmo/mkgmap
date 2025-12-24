package com.github.dtmo.garmin.imgfs;

import java.nio.ByteBuffer;

public record CylinderHeadSector(int cylinder, int head, int sector) {
    public static CylinderHeadSector read(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 3) {
            throw new IllegalArgumentException("Expected 3 bytes of cylinder sector head data, but got: " + byteBuffer.remaining());
        }

        final int head = Byte.toUnsignedInt(byteBuffer.get());

        final byte sectorCylinderByte = byteBuffer.get();
        final byte cylinderByte = byteBuffer.get();
        
        final int sector = (byte) (sectorCylinderByte & 0x3F);
        final int cylinder = Short.toUnsignedInt((short) ((sectorCylinderByte & 0xC0) << 2 | cylinderByte & 0xFF));

        return new CylinderHeadSector(cylinder, head, sector);
    }
}
