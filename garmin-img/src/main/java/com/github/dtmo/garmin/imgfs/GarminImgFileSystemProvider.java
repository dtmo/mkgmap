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

public class GarminImgFileSystemProvider extends FileSystemProvider {
    private final Map<Path, GarminImgFileSystem> filesystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "garminimg";
    }

    @Override
    public GarminImgFileSystem newFileSystem(final Path path, final Map<String, ?> env) throws IOException {
        synchronized (filesystems) {
            if (filesystems.containsKey(path)) {
                throw new FileSystemAlreadyExistsException("The requested filesystem already exists: " + path);
            }

            final GarminImgFileSystem garminImgFileSystem = new GarminImgFileSystem(this, path);
            filesystems.put(path, garminImgFileSystem);

            return garminImgFileSystem;
        }    
    }

    @Override
    public GarminImgFileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        // We are expecting a URI along the lines of: garminimg:/some/path/to/file.img
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Incorrect URI scheme: " + getScheme());
        }

        final String schemeSpecificPart = uri.getSchemeSpecificPart();

        final Path garminImgFilePath = Path.of(schemeSpecificPart).toRealPath();

        synchronized (filesystems) {
            if (filesystems.containsKey(garminImgFilePath)) {
                throw new FileSystemAlreadyExistsException("The requested filesystem already exists: " + garminImgFilePath);
            }

            final GarminImgFileSystem garminImgFileSystem = new GarminImgFileSystem(this, garminImgFilePath);
            filesystems.put(garminImgFilePath, garminImgFileSystem);

            return garminImgFileSystem;
        }
    }

    @Override
    public GarminImgFileSystem getFileSystem(final URI uri) {
        // We are expecting a URI along the lines of: garminimg:/some/path/to/file.img
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Incorrect URI scheme: " + getScheme());
        }

        final String schemeSpecificPart = uri.getSchemeSpecificPart();

        try {
            final Path imgFilePath = Path.of(schemeSpecificPart).toRealPath();

            synchronized (filesystems) {
                final GarminImgFileSystem garminImgFileSystem = filesystems.get(imgFilePath);

                if (garminImgFileSystem != null) {
                    return garminImgFileSystem;
                } else {
                    throw new FileSystemNotFoundException("Count not find existing filesystem for: " + imgFilePath);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not get real path for Garmin IMG file: " + schemeSpecificPart, e);
        }
    }

    @Override
    public GarminImgPath getPath(final URI uri) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newByteChannel'");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
            throws IOException {
        Objects.requireNonNull(dir);

        if (dir instanceof GarminImgPath garminImgPath) {
            return new GarminImgDirectoryStream(garminImgPath.getFileSystem());
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
    public GarminImgFileStore getFileStore(final Path path) throws IOException {
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
            final LinkOption... options)
            throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readAttributes'");
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

    public void removeFileSystem(final GarminImgFileSystem garminImgFileSystem) {
        synchronized (filesystems) {
            filesystems.remove(garminImgFileSystem.getGarminImgFilePath());
        }
    }
}
