package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class DskimgByteChannel implements SeekableByteChannel {
    private final DskimgFileSystem dskimgFileSystem;
    private final DskimgInode inode;

    /** The channel to the underlying DSKIMG file. */
    private final SeekableByteChannel dskimgChannel;

    /** The bytebuffer to hold the current datablock. */
    private final ByteBuffer dataBlockBuffer;

    private boolean isOpen = true;
    private long position = 0;
    private int currentBlockIndex = -1;

    public DskimgByteChannel(final DskimgFileSystem dskimgFileSystem, final DskimgInode inode) throws IOException {
        this.dskimgFileSystem = dskimgFileSystem;
        this.inode = inode;
        this.dskimgChannel = Files.newByteChannel(dskimgFileSystem.getDskimgFilePath(), StandardOpenOption.READ);
        this.dataBlockBuffer = ByteBuffer.allocate(dskimgFileSystem.getMetadata().dataBlockSize());
    }

    @Override
    public boolean isOpen() {
        return this.isOpen;
    }

    @Override
    public void close() throws IOException {
        this.isOpen = false;
    }

    @Override
    public long size() throws IOException {
        return inode.fileSize();
    }

    public int positionDataBlockIndex() {
        final long dataBlockSize = dskimgFileSystem.getMetadata().dataBlockSize();
        return (int) (this.position / dataBlockSize);
    }

    public long positionDataBlockOffset() {
        return dskimgFileSystem.getMetadata().dataBlockSize() % position;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
        this.position = newPosition;
        return this;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        int bytesRead = 0;
        while (inode.fileSize() - position > 0 && dst.hasRemaining()) {
            // There are still bytes to read, and space left in the destination buffer.
            final int destinationRemaining = dst.remaining();

            // Work out which DSKIMG block we should be reading bytes from
            final int positionDataBlockIndex = positionDataBlockIndex();
            if (currentBlockIndex != positionDataBlockIndex) {
                // Work out the position for the start of the data block that we need to read.
                final int positionDataBlockAddress = inode.logicalBlockAddresses().get(positionDataBlockIndex);
                final long dskimgDataBlockOffset = positionDataBlockAddress
                        * dskimgFileSystem.getMetadata().dataBlockSize();
                dskimgChannel.position(dskimgDataBlockOffset);

                // Update the current data block index to one the DSKIMG channel is now pointing
                // at
                currentBlockIndex = positionDataBlockIndex;
            }

            final int dataBlockRemaining = (int) (dskimgFileSystem.getMetadata().dataBlockSize()
                    - (position % dskimgFileSystem.getMetadata().dataBlockSize()));

            // Work out the smaller of either the available destination space, or the size
            // of a data block.
            // We need to make sure that we don't overflow the destination buffer.
            final int limit = Math.min(destinationRemaining, dataBlockRemaining);
            dataBlockBuffer.limit(limit);

            // The current buffered data block is not the one we need, so load the relevant
            // data block bytes into the buffer.
            dataBlockBuffer.rewind();

            // Put as much data into our buffer as we can.
            while (dataBlockBuffer.hasRemaining()) {
                dskimgChannel.read(dataBlockBuffer);
            }

            // Prepare the buffer for reading.
            dataBlockBuffer.flip();

            // Write the data block buffer data to the destination.
            dst.put(dataBlockBuffer);

            // Advance the position by the amount of data we just transferred.
            position += dataBlockBuffer.limit();
            bytesRead += dataBlockBuffer.limit();
        }

        return bytesRead;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'write'");
    }

    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'truncate'");
    }
}
