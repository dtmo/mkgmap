package com.github.dtmo.garmin.imgfs;

// See: https://en.wikipedia.org/wiki/Partition_type
public enum PartitionType {
    /** Empty partition entry */
    EMPTY(0x00);

    private final int value;

    private PartitionType(final int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static PartitionType of(final int value) {
        final PartitionType partitionType;

        switch (value) {
            case 0x00:
                partitionType = EMPTY;
                break;

            default:
                throw new IllegalArgumentException("Unrecognised partition type value: " + value);
        }

        return partitionType;
    }
}
