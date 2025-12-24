package com.github.dtmo.garmin.imgfs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

public record FileAllocationTableEntry(EntryType entryType, String fileBasename, String fileExtension,
        long fileSize, int filePart, List<Integer> logicalBlockAddresses) {
    public static final int SIZE_BYTES = 512;
    public static final int OFFSET_ENTRY_TYPE = 0x00;
    public static final int OFFSET_FILE_NAME = 0x01;
    public static final int OFFSET_FILE_EXTENSION = 0x09;
    public static final int OFFSET_FILE_SIZE = 0x0C;
    public static final int OFFSET_FILE_PART = 0x10;
    public static final int OFFSET_BLOCK_SEQ_START = 0x20;

    public enum EntryType {
        PADDING(0x00),
        REGULAR(0x01);

        private final int value;

        private EntryType(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static EntryType of(final int value) {
            final EntryType fileBlockType;

            switch (value) {
                case 0x00:
                    fileBlockType = PADDING;
                    break;

                case 0x01:
                    fileBlockType = REGULAR;
                    break;

                default:
                    throw new IllegalArgumentException("Unrecognised file block type value: " + value);
            }

            return fileBlockType;
        }
    }

    public String filename() {
        return fileBasename() + "." + fileExtension();
    }

    public static FileAllocationTableEntry read(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("Expected " + SIZE_BYTES
                    + " bytes of file allocation table entry data, but got: " + byteBuffer.remaining());
        }

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        final EntryType entryType = EntryType.of(Byte.toUnsignedInt(byteBuffer.get(OFFSET_ENTRY_TYPE)));

        final byte[] fileBasenameBytes = new byte[8];
        byteBuffer.get(OFFSET_FILE_NAME, fileBasenameBytes);
        final String fileBasename = new String(fileBasenameBytes, StandardCharsets.US_ASCII).trim();

        final byte[] fileExtensionBytes = new byte[3];
        byteBuffer.get(OFFSET_FILE_EXTENSION, fileExtensionBytes);
        final String fileExtension = new String(fileExtensionBytes, StandardCharsets.US_ASCII).trim();

        final long fileSize = Integer.toUnsignedLong(byteBuffer.getInt(OFFSET_FILE_SIZE));

        final int filePart = Short.toUnsignedInt(byteBuffer.getShort(OFFSET_FILE_PART));

        byteBuffer.position(OFFSET_BLOCK_SEQ_START);

        // Read up to 240 logical block addresses or until we find 0xFFFF
        final List<Integer> logicalBlockAddresses = Stream.generate(byteBuffer::getShort)
                .map(Short::toUnsignedInt)
                .takeWhile(block -> block != 0xFFFF)
                .limit(240)
                .toList();

        return new FileAllocationTableEntry(entryType, fileBasename, fileExtension, fileSize, filePart,
                logicalBlockAddresses);
    }
}
