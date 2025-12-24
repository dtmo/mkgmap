package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class GarminImgDirectoryStream implements DirectoryStream<Path> {
    private final GarminImgFileSystem garminImgFileSystem;
    private boolean isClosed = false;

    public GarminImgDirectoryStream(final GarminImgFileSystem garminImgFileSystem) {
        this.garminImgFileSystem = garminImgFileSystem;
    }

    @Override
    public Iterator<Path> iterator() {
        if (isClosed) {
            throw new ClosedDirectoryStreamException();
        }

        final Iterator<GarminImgPath> pathIterator = garminImgFileSystem.pathIterator();

        return new Iterator<Path>() {
            @Override
            public boolean hasNext() {
                return isClosed ? false : pathIterator.hasNext();
            }

            @Override
            public Path next() {
                if (isClosed) {
                    throw new NoSuchElementException("The directory stream is closed");
                } else {
                    return pathIterator.next();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.isClosed = true;
    }
}
