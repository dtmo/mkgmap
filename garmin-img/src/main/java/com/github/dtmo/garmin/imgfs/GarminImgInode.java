package com.github.dtmo.garmin.imgfs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record GarminImgInode(long fileSize, List<Integer> logicalBlockAddresses) {
    public static Builder builder(final long dataBlockSize, final long fileSize) {
        return new Builder((int) Math.ceilDiv(fileSize, dataBlockSize))
                .withFileSize(fileSize);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long fileSize;
        private ArrayList<Integer> logicalBlockAddresses;

        public Builder(final int expectedLogicalBlockAddressCount) {
            logicalBlockAddresses = new ArrayList<>(expectedLogicalBlockAddressCount);
        }

        public Builder() {
            logicalBlockAddresses = new ArrayList<>();
        }

        public Builder withFileSize(final long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder withLogicalBlockAdresses(final Collection<Integer> logicalBlockAddresses) {
            this.logicalBlockAddresses.clear();
            return appendLogicalBlockAddresses(logicalBlockAddresses);
        }

        public Builder appendLogicalBlockAddresses(final Collection<Integer> logicalBlockAddresses) {
            this.logicalBlockAddresses.addAll(logicalBlockAddresses);
            return this;
        }

        public GarminImgInode build() {
            return new GarminImgInode(fileSize, List.copyOf(logicalBlockAddresses));
        }
    }
}
