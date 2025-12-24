package com.github.dtmo.garmin.imgfs;

public enum PartitionStatus {
    /** Inactive */
    INACTIVE(0x00),

    /** Active or bootable */
    ACTIVE(0x80);

    private final int value;

    private PartitionStatus(final int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static PartitionStatus of(final int value) {
        final PartitionStatus partitionStatus;

        switch (value) {
            case 0x80:
                partitionStatus = ACTIVE;
                break;

            case 0x00:
                partitionStatus = INACTIVE;
                break;

            default:
                throw new IllegalArgumentException("Unrecognised partition status value: " + value);
        }

        return partitionStatus;
    }
}
