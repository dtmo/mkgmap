package com.github.dtmo.garmin.imgfs;

import static com.github.dtmo.garmin.imgfs.GarminImgFileSystem.SEPARATOR_STRING;

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

public class GarminImgPath implements Path {
    private final GarminImgFileSystem garminImgFileSystem;
    private final String filename;

    public GarminImgPath(final GarminImgFileSystem garminImgFileSystem, final String filename) {
        this.garminImgFileSystem = garminImgFileSystem;
        this.filename = filename;
    }

    @Override
    public GarminImgFileSystem getFileSystem() {
        return garminImgFileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return filename.startsWith(SEPARATOR_STRING);
    }

    public boolean isRoot() {
        return filename.isEmpty() || filename.equals(SEPARATOR_STRING);
    }

    @Override
    public GarminImgPath getRoot() {
        return garminImgFileSystem.getRootDirectory();
    }

    @Override
    public GarminImgPath getFileName() {
        // The Garmin IMG filesystem does not support nested directory structures, so we
        // only need to strip off an optional leading separator character for absolute
        // paths.
        if (!isAbsolute()) {
            return this;
        } else {
            return new GarminImgPath(getFileSystem(), filename.substring(1));
        }
    }

    @Override
    public GarminImgPath getParent() {
        // The Garmin IMG filesystem does not support nested directory structures, so
        // the parent is always root for non-root paths, or null for root paths.
        if (isRoot()) {
            return null;
        } else {
            return getRoot();
        }
    }

    @Override
    public int getNameCount() {
        // The Garmin IMG filesystem does not support nested directory structures, so
        // the path is always eaither the root or has name name element.
        return isRoot() ? 0 : 1;
    }

    @Override
    public GarminImgPath getName(final int index) {
        if (index != 0) {
            throw new IllegalArgumentException("The Garmin IMG filesystem does not support nested directories");
        }

        if (isRoot()) {
            throw new IllegalArgumentException("The root path does not have a name to return");
        }

        return getFileName();
    }

    @Override
    public GarminImgPath subpath(final int beginIndex, final int endIndex) {
        throw new UnsupportedOperationException("Unimplemented method 'subpath'");
    }

    @Override
    public boolean startsWith(final Path other) {
        if (other instanceof GarminImgPath garminImgPath) {
            return filename.equalsIgnoreCase(garminImgPath.filename);
        } else {
            return false;
        }
    }

    @Override
    public boolean endsWith(final Path other) {
        if (other instanceof GarminImgPath garminImgPath) {
            return filename.equalsIgnoreCase(garminImgPath.filename);
        } else {
            return false;
        }
    }

    @Override
    public GarminImgPath normalize() {
        throw new UnsupportedOperationException("Unimplemented method 'normalize'");
    }

    @Override
    public GarminImgPath resolve(final Path other) {
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

    @Override
    public GarminImgPath relativize(final Path other) {
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
    public GarminImgPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return new GarminImgPath(garminImgFileSystem, SEPARATOR_STRING + filename);
        }
    }

    @Override
    public GarminImgPath toRealPath(final LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
            throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }

    @Override
    public int compareTo(final Path other) {
        if (other instanceof GarminImgPath garminImgPath) {
            return filename.compareTo(garminImgPath.filename);
        } else {
            throw new ProviderMismatchException(
                    "Cannot compare an instance of GarminImgPath with an instance of " + other.getClass());
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
        } else if (other instanceof GarminImgPath garminImgPath) {
            return this.garminImgFileSystem == garminImgPath.garminImgFileSystem
                    && Objects.equals(filename, garminImgPath.filename);
        } else {
            return false;
        }
    }
}
