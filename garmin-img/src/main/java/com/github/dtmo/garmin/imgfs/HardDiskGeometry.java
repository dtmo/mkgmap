package com.github.dtmo.garmin.imgfs;

import java.nio.ByteBuffer;

// See: https://en.wikipedia.org/wiki/Logical_block_addressing#CHS_conversion
public record HardDiskGeometry(int cylinders, int heads, int sectors) {
    public long toLogicalBlockAddress(final int cylinder, final int head, final int sector) {
        return (cylinder * heads + head) * sectors + (sector - 1);
    }

    public CylinderHeadSector toCylinderHeadSector(final int logicalBlockAddress) {
        final int cylinder = logicalBlockAddress / (heads * sectors);
        final int head = (logicalBlockAddress / sectors) % heads;
        final int sector = (logicalBlockAddress % sectors) + 1;

        return new CylinderHeadSector(cylinder, head, sector);
    }

    public static HardDiskGeometry read(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 6) {
            throw new IllegalArgumentException("Expected 6 bytes of hard disk geometry data, but got: " + byteBuffer.remaining());
        }

        final int sectors = Short.toUnsignedInt(byteBuffer.getShort());
        final int heads = Short.toUnsignedInt(byteBuffer.getShort());
        final int cylinders = Short.toUnsignedInt(byteBuffer.getShort());

        return new HardDiskGeometry(cylinders, heads, sectors);
    }
}
