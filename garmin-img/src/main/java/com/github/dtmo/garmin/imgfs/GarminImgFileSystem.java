package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GarminImgFileSystem extends FileSystem {
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR_CHAR);

    private final GarminImgFileSystemProvider garminImgFileSystemProvider;
    private final Path garminImgFilePath;
    private final byte xorByte;
    private final MasterBootRecord masterBootRecord;
    private final GarminImgMetadata metadata;
    private final Map<GarminImgPath, GarminImgInode> inodes;
    private final GarminImgPath rootDirectory;
    private boolean isOpen = true;

    public GarminImgFileSystem(final GarminImgFileSystemProvider garminImgFileSystemProvider,
            final Path garminImgFilePath) throws IOException {
        this.garminImgFileSystemProvider = garminImgFileSystemProvider;
        this.garminImgFilePath = garminImgFilePath;

        try (final SeekableByteChannel channel = Files.newByteChannel(garminImgFilePath)) {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(MasterBootRecord.SIZE_BYTES);
            channel.read(byteBuffer);

            xorByte = byteBuffer.get(0);

            // Xor the bytes to ensure that the header can be read.
            for (int i = 0; i < 512; i++) {
                byteBuffer.put(i, (byte) (byteBuffer.get(i) ^ xorByte));
            }

            byteBuffer.flip();

            this.masterBootRecord = MasterBootRecord.read(byteBuffer);

            // The master boot record bootstrap area contains the Garmin IMG header.
            this.metadata = GarminImgMetadata.read(ByteBuffer.wrap(masterBootRecord.bootstrapBytes()));

            this.inodes = readFileInodes(channel);
        }

        rootDirectory = new GarminImgPath(this, SEPARATOR_STRING);
    }

    public Map<GarminImgPath, GarminImgInode> readFileInodes(final SeekableByteChannel channel) throws IOException {
        final HashMap<GarminImgPath, GarminImgInode> inodes = new LinkedHashMap<>();

        channel.position(metadata.fileAllocationTableOffset());

        final ByteBuffer byteBuffer = ByteBuffer.allocate(FileAllocationTableEntry.SIZE_BYTES);
        try (final XorInputStream xorInputStream = new XorInputStream(Channels.newInputStream(channel), xorByte);
                final ReadableByteChannel xorChannel = Channels.newChannel(xorInputStream)) {
            xorChannel.read(byteBuffer);
            byteBuffer.flip();

            // The first FAT entry describes the file allocation table itself.
            // The file size and blocks include both the MBR and file allocation table
            // entries.
            // The FAT blocks are sized based on the IMG metadata block size.
            FileAllocationTableEntry fileAllocationTableEntry = FileAllocationTableEntry
                    .read(byteBuffer);
            final long dataBlocksOffset = fileAllocationTableEntry.fileSize();

            // Read the first "real" entry
            byteBuffer.rewind();
            xorChannel.read(byteBuffer);
            byteBuffer.flip();

            fileAllocationTableEntry = FileAllocationTableEntry.read(byteBuffer);
            GarminImgInode.Builder inodeBuilder = GarminImgInode.builder(this.metadata.dataBlockSize(),
                    fileAllocationTableEntry.fileSize())
                    .withLogicalBlockAdresses(fileAllocationTableEntry.logicalBlockAddresses());
            FileAllocationTableEntry previousFileAllocationTableEntry;
            while (channel.position() < dataBlocksOffset) {
                previousFileAllocationTableEntry = fileAllocationTableEntry;

                byteBuffer.rewind();
                xorChannel.read(byteBuffer);
                byteBuffer.flip();

                fileAllocationTableEntry = FileAllocationTableEntry.read(byteBuffer);

                if (fileAllocationTableEntry.entryType() == FileAllocationTableEntry.EntryType.REGULAR) {
                    if (Objects.equals(previousFileAllocationTableEntry.filename(),
                            fileAllocationTableEntry.filename())) {
                        // We have found another entry relating to the same file. Update the current
                        // inode builder.
                        inodeBuilder.appendLogicalBlockAddresses(fileAllocationTableEntry.logicalBlockAddresses());
                    } else {
                        // We have found an entry relating to a new file, so we need to register the
                        // current inode and start building a new one.
                        final GarminImgPath path = new GarminImgPath(this, previousFileAllocationTableEntry.filename());

                        if (!inodes.containsKey(path)) {
                            inodes.put(path, inodeBuilder.build());
                        } else {
                            throw new IllegalStateException("File inodes map already contains an inode for file: "
                                    + previousFileAllocationTableEntry.filename());
                        }

                        inodeBuilder = GarminImgInode.builder(this.metadata.dataBlockSize(),
                                fileAllocationTableEntry.fileSize())
                                .withLogicalBlockAdresses(fileAllocationTableEntry.logicalBlockAddresses());
                    }
                } else {
                    // Skip padding entries
                }
            }

            if (fileAllocationTableEntry.entryType() == FileAllocationTableEntry.EntryType.REGULAR) {
                // We have finished iterating over the file allocation table entries, so we need
                // to register the final inode.
                final GarminImgPath path = new GarminImgPath(this, fileAllocationTableEntry.filename());

                if (!inodes.containsKey(path)) {
                    inodes.put(path, inodeBuilder.build());
                } else {
                    throw new IllegalStateException("File inodes map already contains an inode for file: "
                            + fileAllocationTableEntry.filename());
                }
            } else {
                // skip padding entries
            }

            return inodes;
        }
    }

    public Path getGarminImgFilePath() {
        return garminImgFilePath;
    }

    public GarminImgPath getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public GarminImgFileSystemProvider provider() {
        return garminImgFileSystemProvider;
    }

    @Override
    public void close() throws IOException {
        this.isOpen = false;

        garminImgFileSystemProvider.removeFileSystem(this);
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isReadOnly() {
        // Writing is not implemented yet, so readonly is always true.
        return true;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR_STRING;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(rootDirectory);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStores'");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportedFileAttributeViews'");
    }

    @Override
    public GarminImgPath getPath(final String first, final String... more) {
        if (more.length == 0) {
            return new GarminImgPath(this, first);
        } else {
            throw new UnsupportedOperationException("The Garmin IMG file system does not support nested directories");
        }
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPathMatcher'");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserPrincipalLookupService'");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newWatchService'");
    }

    public Iterator<GarminImgPath> pathIterator() {
        return inodes.keySet().iterator();
    }

    public static byte peekXorByte(final SeekableByteChannel channel) throws IOException {
        final long position = channel.position();
        final ByteBuffer xorByteBuffer = ByteBuffer.allocate(1);
        channel.read(xorByteBuffer);
        channel.position(position);
        xorByteBuffer.flip();
        return xorByteBuffer.get();
    }
}
