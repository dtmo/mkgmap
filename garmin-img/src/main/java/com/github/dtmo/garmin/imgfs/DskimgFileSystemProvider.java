package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DskimgFileSystemProvider extends FileSystemProvider {
    private final Map<Path, DskimgFileSystem> filesystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "dskimg";
    }

    @Override
    public DskimgFileSystem newFileSystem(final Path path, final Map<String, ?> env) throws IOException {
        synchronized (filesystems) {
            if (filesystems.containsKey(path)) {
                throw new FileSystemAlreadyExistsException("The requested filesystem already exists: " + path);
            }

            final DskimgFileSystem dskimgFileSystem = new DskimgFileSystem(this, path);
            filesystems.put(path, dskimgFileSystem);

            return dskimgFileSystem;
        }
    }

    @Override
    public DskimgFileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        // We are expecting a URI along the lines of: dskimg:/some/path/to/file.img
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Incorrect URI scheme: " + getScheme());
        }

        final String schemeSpecificPart = uri.getSchemeSpecificPart();

        final Path dskimgFilePath = Path.of(schemeSpecificPart).toRealPath();

        synchronized (filesystems) {
            if (filesystems.containsKey(dskimgFilePath)) {
                throw new FileSystemAlreadyExistsException(
                        "The requested filesystem already exists: " + dskimgFilePath);
            }

            final DskimgFileSystem dskimgFileSystem = new DskimgFileSystem(this, dskimgFilePath);
            filesystems.put(dskimgFilePath, dskimgFileSystem);

            return dskimgFileSystem;
        }
    }

    @Override
    public DskimgFileSystem getFileSystem(final URI uri) {
        // We are expecting a URI along the lines of: dskimg:/some/path/to/file.img
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Incorrect URI scheme: " + getScheme());
        }

        final String schemeSpecificPart = uri.getSchemeSpecificPart();

        try {
            final Path imgFilePath = Path.of(schemeSpecificPart).toRealPath();

            synchronized (filesystems) {
                final DskimgFileSystem dskimgFileSystem = filesystems.get(imgFilePath);

                if (dskimgFileSystem != null) {
                    return dskimgFileSystem;
                } else {
                    throw new FileSystemNotFoundException("Count not find existing filesystem for: " + imgFilePath);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not get real path for Garmin DSKIMG file: " + schemeSpecificPart, e);
        }
    }

    @Override
    public DskimgPath getPath(final URI uri) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        Objects.requireNonNull(path);

        if (path instanceof DskimgPath dskimgPath) {
            final DskimgFileSystem fileSystem = dskimgPath.getFileSystem();
            return new DskimgByteChannel(fileSystem, fileSystem.getPathInode(dskimgPath));
        } else {
            throw new ProviderMismatchException();
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
            throws IOException {
        Objects.requireNonNull(dir);

        if (dir instanceof DskimgPath dskimgPath) {
            return new DskimgDirectoryStream(dskimgPath.getFileSystem());
        } else {
            throw new ProviderMismatchException();
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDirectory'");
    }

    @Override
    public void delete(final Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSameFile'");
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isHidden'");
    }

    @Override
    public DskimgFileStore getFileStore(final Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStore'");
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkAccess'");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
            final LinkOption... options) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileAttributeView'");
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
            final LinkOption... options) throws IOException {
        Objects.requireNonNull(path);

        if (path instanceof DskimgPath dskimgPath) {
            return dskimgPath.readAttributes(type);
        } else {
            throw new ProviderMismatchException();
        }
    }

    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
            throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readAttributes'");
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
            throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    public void removeFileSystem(final DskimgFileSystem dskimgFileSystem) {
        synchronized (filesystems) {
            filesystems.remove(dskimgFileSystem.getDskimgFilePath());
        }
    }
}
