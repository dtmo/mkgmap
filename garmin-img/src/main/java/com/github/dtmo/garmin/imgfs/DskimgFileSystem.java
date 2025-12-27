package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

public class DskimgFileSystem extends FileSystem {
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR_CHAR);

    private final DskimgFileSystemProvider dskimgFileSystemProvider;
    private final Path dskimgFilePath;
    private final byte xorByte;
    private final MasterBootRecord masterBootRecord;
    private final DskimgMetadata metadata;
    private final Map<DskimgPath, DskimgInode> inodes;
    private final DskimgPath rootDirectory;
    private boolean isOpen = true;

    public DskimgFileSystem(final DskimgFileSystemProvider dskimgFileSystemProvider,
            final Path dskimgFilePath) throws IOException {
        this.dskimgFileSystemProvider = dskimgFileSystemProvider;
        this.dskimgFilePath = dskimgFilePath;

        try (final SeekableByteChannel channel = Files.newByteChannel(dskimgFilePath)) {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(MasterBootRecord.SIZE_BYTES);
            channel.read(byteBuffer);

            xorByte = byteBuffer.get(0);

            // Xor the bytes to ensure that the header can be read.
            for (int i = 0; i < 512; i++) {
                byteBuffer.put(i, (byte) (byteBuffer.get(i) ^ xorByte));
            }

            byteBuffer.flip();

            this.masterBootRecord = MasterBootRecord.read(byteBuffer);

            // The master boot record bootstrap area contains the Garmin DSKIMG header.
            this.metadata = DskimgMetadata.read(ByteBuffer.wrap(masterBootRecord.bootstrapBytes()));

            this.inodes = readFileInodes(channel);
        }

        rootDirectory = new DskimgPath(this, SEPARATOR_STRING);
    }

    public Map<DskimgPath, DskimgInode> readFileInodes(final SeekableByteChannel channel) throws IOException {
        final HashMap<DskimgPath, DskimgInode> inodes = new LinkedHashMap<>();

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
            DskimgInode.Builder inodeBuilder = DskimgInode.builder(this.metadata.dataBlockSize(),
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
                        final DskimgPath path = new DskimgPath(this, previousFileAllocationTableEntry.filename());

                        if (!inodes.containsKey(path)) {
                            inodes.put(path, inodeBuilder.build());
                        } else {
                            throw new IllegalStateException("File inodes map already contains an inode for file: "
                                    + previousFileAllocationTableEntry.filename());
                        }

                        inodeBuilder = DskimgInode.builder(this.metadata.dataBlockSize(),
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
                final DskimgPath path = new DskimgPath(this, fileAllocationTableEntry.filename());

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

    public Path getDskimgFilePath() {
        return dskimgFilePath;
    }

    public MasterBootRecord getMasterBootRecord() {
        return masterBootRecord;
    }

    public DskimgMetadata getMetadata() {
        return metadata;
    }

    public DskimgInode getPathInode(final DskimgPath path) {
        return inodes.get(path);
    }

    public DskimgPath getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public DskimgFileSystemProvider provider() {
        return dskimgFileSystemProvider;
    }

    @Override
    public void close() throws IOException {
        this.isOpen = false;

        dskimgFileSystemProvider.removeFileSystem(this);
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
    public DskimgPath getPath(final String first, final String... more) {
        if (more.length == 0) {
            return new DskimgPath(this, first);
        } else {
            throw new UnsupportedOperationException("The Garmin DSKIMG file system does not support nested directories");
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

    public Iterator<DskimgPath> pathIterator() {
        return inodes.keySet().iterator();
    }

    public DskimgFileAttributes getFileAttributes(final DskimgPath dskimgPath) throws NoSuchFileException {
        if (dskimgPath.isRoot()) {
            // I have no idea if this really makes sense
            return new DskimgFileAttributes(false, metadata.dataBlockSize());
        } else {
            final DskimgInode dskimgInode = inodes.get(dskimgPath.getFileName());
            if (dskimgInode != null) {
                return new DskimgFileAttributes(true, dskimgInode.fileSize());
            } else {
                throw new NoSuchFileException(dskimgPath.toString());
            }
        }
    }
}
