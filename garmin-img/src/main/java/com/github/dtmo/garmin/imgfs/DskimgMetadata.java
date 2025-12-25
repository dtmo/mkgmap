package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;

public record DskimgMetadata(int version, YearMonth updatedAt, LocalDateTime createdAt, int fatStartBlock,
                HardDiskGeometry hardDiskGeometry, String description, long dataBlockSize) {
        public static final int FAT_BLOCK_SIZE = 512;
        public static final int OFFSET_XOR_BYTE = 0x00;
        public static final int OFFSET_VERSION = 0x08;
        public static final int OFFSET_UPDATE_MONTH = 0x0A;
        public static final int OFFSET_UPDATE_YEAR = 0x0B;
        public static final int OFFSET_CHECKSUM = 0x0F;
        public static final int OFFSET_DSKIMG = 0x10;
        public static final int OFFSET_HD_GEOMTERY = 0x18;
        public static final int OFFSET_CREATION_YEAR = 0x39;
        public static final int OFFSET_CREATION_MONTH = 0x3B;
        public static final int OFFSET_CREATION_DAY = 0x3C;
        public static final int OFFSET_CREATION_HOUR = 0x3D;
        public static final int OFFSET_CREATION_MINUTE = 0x3E;
        public static final int OFFSET_CREATION_SECOND = 0x3F;
        public static final int OFFSET_FAT_START_BLOCK = 0x40;
        public static final int OFFSET_GARMIN = 0x41;
        public static final int OFFSET_DESCRIPTION_START = 0x49;
        public static final int OFFSET_POSSIBLE_HEADS = 0x5D;
        public static final int OFFSET_POSSIBLE_SECTORS = 0x5F;
        public static final int OFFSET_BLOCK_SIZE_EXPONENT_BASE = 0x61;
        public static final int OFFSET_BLOCK_SIZE_EXPONENT_INCREMENT = 0x62;
        public static final int OFFSET_MYSTERIOUS_DISK_GEOMETRY_RELATED_VALUE = 0x63;
        public static final int OFFSET_DESCRIPTION_CONT = 0x65;

        public static final byte[] DSKIMG_SIGNATURE = new byte[] { 0x44, 0x53, 0x4B, 0x49, 0x4D, 0x47, 0x00 };
        public static final byte[] GARMIN_SIGNATURE = new byte[] { 0x47, 0x41, 0x52, 0x4D, 0x49, 0x4E, 0x00 };

        public long fileAllocationTableOffset() {
                return FAT_BLOCK_SIZE * fatStartBlock;
        }

        public static DskimgMetadata read(final ByteBuffer byteBuffer) throws IOException {
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                final int version = Short.toUnsignedInt(byteBuffer.getShort(OFFSET_VERSION));

                final Month updateMonth = Month.of(Byte.toUnsignedInt(byteBuffer.get(OFFSET_UPDATE_MONTH)) + 1);
                final int updateYearByte = Byte.toUnsignedInt(byteBuffer.get(OFFSET_UPDATE_YEAR));
                final int updateYear = updateYearByte >= 0x63 ? updateYearByte + 1900 : updateYearByte + 2000;
                final YearMonth updatedAt = YearMonth.of(updateYear, updateMonth);

                // Check for the "DKSIMG\0" signature
                final byte[] dskimgBytes = new byte[7];
                byteBuffer.get(OFFSET_DSKIMG, dskimgBytes);
                if (!Arrays.equals(DSKIMG_SIGNATURE, dskimgBytes)) {
                        throw new UnsupportedOperationException(
                    "Expected dskimg signature bytes " + Arrays.toString(DSKIMG_SIGNATURE) + ", but got: "
                            + Arrays.toString(dskimgBytes));
                }

                byteBuffer.position(OFFSET_HD_GEOMTERY);
                final HardDiskGeometry hardDiskGeometry = HardDiskGeometry.read(byteBuffer);

                final int creationYear = Short.toUnsignedInt(byteBuffer.getShort(OFFSET_CREATION_YEAR));
                final int creationMonthByte = Byte.toUnsignedInt(byteBuffer.get(OFFSET_CREATION_MONTH));
                final Month creationMonth = Month.of(creationMonthByte + 1);
                final int creationDay = Byte.toUnsignedInt(byteBuffer.get(OFFSET_CREATION_DAY));
                final int creationHour = Byte.toUnsignedInt(byteBuffer.get(OFFSET_CREATION_HOUR));
                final int creationMinute = Byte.toUnsignedInt(byteBuffer.get(OFFSET_CREATION_MINUTE));
                final int creationSecond = Byte.toUnsignedInt(byteBuffer.get(OFFSET_CREATION_SECOND));

                final LocalDateTime createdAt = LocalDateTime.of(creationYear, creationMonth, creationDay, creationHour,
                                creationMinute, creationSecond);

                // The 512 byte block in which File Allocation Table data starts.
                final int fatStartBlock = Byte.toUnsignedInt(byteBuffer.get(OFFSET_FAT_START_BLOCK));

                // Check for for "GARMIN\0" signature
                final byte[] garminBytes = new byte[7];
                byteBuffer.get(OFFSET_GARMIN, garminBytes);
                if (!Arrays.equals(GARMIN_SIGNATURE, garminBytes)) {
                        throw new UnsupportedOperationException(
                    "Expected GARMIN signature bytes " + Arrays.toString(GARMIN_SIGNATURE) + ", but got: "
                            + Arrays.toString(garminBytes));
                }

                // Start reading the map dscription.
                final byte[] descriptionBytes = new byte[50];
                byteBuffer.get(OFFSET_DESCRIPTION_START, descriptionBytes, 0, 20);
                byteBuffer.get(OFFSET_DESCRIPTION_CONT, descriptionBytes, 20, 30);
                final String description = new String(descriptionBytes, StandardCharsets.US_ASCII).trim();

                // Heads?
                System.out.println("heads?: " + Integer.toUnsignedString(byteBuffer.getShort(OFFSET_POSSIBLE_HEADS)));

                // Sectors?
                System.out.println(
                                "sectors?: " + Integer.toUnsignedString(byteBuffer.getShort(OFFSET_POSSIBLE_SECTORS)));

                final int dataBlockSizeExponentE1 = Byte.toUnsignedInt(byteBuffer.get(OFFSET_BLOCK_SIZE_EXPONENT_BASE));
                final int dataBlockSizeExponentE2 = Byte
                                .toUnsignedInt(byteBuffer.get(OFFSET_BLOCK_SIZE_EXPONENT_INCREMENT));
                final int dataBlockSize = (int) Math.pow(2, dataBlockSizeExponentE1 + dataBlockSizeExponentE2);

                // ?? == sectors * heads * cylinders / 2 ^ dataBlockSizeExponentE2
                System.out.println("Mysterious geometry?:"
                                + Integer.toUnsignedString(
                                                byteBuffer.getShort(OFFSET_MYSTERIOUS_DISK_GEOMETRY_RELATED_VALUE)));

                return new DskimgMetadata(version, updatedAt, createdAt, fatStartBlock, hardDiskGeometry,
                                description,
                                dataBlockSize);
        }
}