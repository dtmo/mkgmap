package com.github.dtmo.garmin.imgfs;

import static com.github.dtmo.garmin.imgfs.DskimgFileSystem.SEPARATOR_STRING;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

public class DskimgPath implements Path {
    private final DskimgFileSystem dskimgFileSystem;
    private final String filename;

    public DskimgPath(final DskimgFileSystem dskimgFileSystem, final String filename) {
        this.dskimgFileSystem = dskimgFileSystem;
        this.filename = filename;
    }

    @Override
    public DskimgFileSystem getFileSystem() {
        return dskimgFileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return filename.startsWith(SEPARATOR_STRING);
    }

    public boolean isRoot() {
        return filename.isEmpty() || filename.equals(SEPARATOR_STRING);
    }

    @Override
    public DskimgPath getRoot() {
        return dskimgFileSystem.getRootDirectory();
    }

    @Override
    public DskimgPath getFileName() {
        // The Garmin DSKIMG filesystem does not support nested directory structures, so we
        // only need to strip off an optional leading separator character for absolute
        // paths.
        if (!isAbsolute()) {
            return this;
        } else {
            return new DskimgPath(getFileSystem(), filename.substring(1));
        }
    }

    @Override
    public DskimgPath getParent() {
        // The Garmin DSKIMG filesystem does not support nested directory structures, so
        // the parent is always root for non-root paths, or null for root paths.
        if (isRoot()) {
            return null;
        } else {
            return getRoot();
        }
    }

    @Override
    public int getNameCount() {
        // The Garmin DSKIMG filesystem does not support nested directory structures, so
        // the path is always eaither the root or has name name element.
        return isRoot() ? 0 : 1;
    }

    @Override
    public DskimgPath getName(final int index) {
        if (index != 0) {
            throw new IllegalArgumentException("The Garmin DSKIMG filesystem does not support nested directories");
        }

        if (isRoot()) {
            throw new IllegalArgumentException("The root path does not have a name to return");
        }

        return getFileName();
    }

    @Override
    public DskimgPath subpath(final int beginIndex, final int endIndex) {
        throw new UnsupportedOperationException("Unimplemented method 'subpath'");
    }

    @Override
    public boolean startsWith(final Path other) {
        if (other instanceof DskimgPath dskimgPath) {
            return filename.equalsIgnoreCase(dskimgPath.filename);
        } else {
            return false;
        }
    }

    @Override
    public boolean endsWith(final Path other) {
        if (other instanceof DskimgPath dskimgPath) {
            return filename.equalsIgnoreCase(dskimgPath.filename);
        } else {
            return false;
        }
    }

    @Override
    public DskimgPath normalize() {
        throw new UnsupportedOperationException("Unimplemented method 'normalize'");
    }

    @Override
    public DskimgPath resolve(final Path other) {
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

    @Override
    public DskimgPath relativize(final Path other) {
        throw new UnsupportedOperationException("Unimplemented method 'relativize'");
    }

    @Override
    public URI toUri() {
        try {
            return new URI(getFileSystem().provider().getScheme(), this.toString(), null);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Could not ncode path to URI: " + toAbsolutePath().toString());
        }
    }

    @Override
    public DskimgPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return new DskimgPath(dskimgFileSystem, SEPARATOR_STRING + filename);
        }
    }

    @Override
    public DskimgPath toRealPath(final LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
            throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }

    @Override
    public int compareTo(final Path other) {
        if (other instanceof DskimgPath dskimgPath) {
            return filename.compareTo(dskimgPath.filename);
        } else {
            throw new ProviderMismatchException(
                    "Cannot compare an instance of dskimgPath with an instance of " + other.getClass());
        }
    }

    @Override
    public String toString() {
        return filename;
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof DskimgPath dskimgPath) {
            return this.dskimgFileSystem == dskimgPath.dskimgFileSystem
                    && Objects.equals(filename, dskimgPath.filename);
        } else {
            return false;
        }
    }
}
