package com.github.dtmo.garmin.imgfs;

import java.nio.ByteBuffer;
import java.util.Optional;

// See: https://en.wikipedia.org/wiki/Master_boot_record#PTE
public record PartitionTableEntry(PartitionStatus partitionStatus, CylinderHeadSector firstSector,
        PartitionType partitionType, CylinderHeadSector lastSector, long firstSectorLogicalBlockAddress,
        long sectorCount) {
    public static final Optional<PartitionTableEntry> read(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 16) {
            throw new IllegalArgumentException("Expected 16 bytes of partition table entry data, but got: " + byteBuffer.remaining());
        }

        final PartitionStatus partitionStatus = PartitionStatus.of(Byte.toUnsignedInt(byteBuffer.get()));

        final CylinderHeadSector firstSector = CylinderHeadSector.read(byteBuffer);

        final PartitionType partitionType = PartitionType.of(Byte.toUnsignedInt(byteBuffer.get()));

        final CylinderHeadSector lastSector = CylinderHeadSector.read(byteBuffer);

        final long firstSectorLogicalBlockAddress = Integer.toUnsignedLong(byteBuffer.getInt());
        
        final long sectorCount = Integer.toUnsignedLong(byteBuffer.getInt());

        if (!firstSector.equals(lastSector)) {
            return Optional.of(new PartitionTableEntry(partitionStatus, firstSector, partitionType, lastSector, firstSectorLogicalBlockAddress, sectorCount));
        } else {
            return Optional.empty();
        }
    }
}
