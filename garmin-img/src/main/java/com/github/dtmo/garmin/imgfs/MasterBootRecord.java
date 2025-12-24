package com.github.dtmo.garmin.imgfs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

// See: https://en.wikipedia.org/wiki/Master_boot_record#Sector_layout
public record MasterBootRecord(byte[] bootstrapBytes, Optional<PartitionTableEntry> partition1,
        Optional<PartitionTableEntry> partition2, Optional<PartitionTableEntry> partition3,
        Optional<PartitionTableEntry> partition4) {
    public static final int SIZE_BYTES = 512;
    public static final byte[] BOOT_SIGNATURE = new byte[] { (byte) 0x55, (byte) 0xAA };

    public static MasterBootRecord read(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Expected " + SIZE_BYTES + " bytes of master boot record data, but got: " + byteBuffer.remaining());
        }

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        final byte[] bootstrapBytes = new byte[446];
        byteBuffer.get(bootstrapBytes);

        final Optional<PartitionTableEntry> partition1 = PartitionTableEntry.read(byteBuffer);
        final Optional<PartitionTableEntry> partition2 = PartitionTableEntry.read(byteBuffer);
        final Optional<PartitionTableEntry> partition3 = PartitionTableEntry.read(byteBuffer);
        final Optional<PartitionTableEntry> partition4 = PartitionTableEntry.read(byteBuffer);

        // Check for boot signature
        final byte[] bootSignatureBytes = new byte[2];
        byteBuffer.get(bootSignatureBytes);
        if (!Arrays.equals(BOOT_SIGNATURE, bootSignatureBytes)) {
            throw new UnsupportedOperationException(
                    "Expected boot signature bytes " + Arrays.toString(BOOT_SIGNATURE) + ", but got: "
                            + Arrays.toString(bootSignatureBytes));
        }

        return new MasterBootRecord(bootstrapBytes, partition1, partition2, partition3, partition4);
    }
}
