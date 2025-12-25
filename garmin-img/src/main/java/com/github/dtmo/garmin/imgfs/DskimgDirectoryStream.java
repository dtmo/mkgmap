package com.github.dtmo.garmin.imgfs;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DskimgDirectoryStream implements DirectoryStream<Path> {
    private final DskimgFileSystem dskimgFileSystem;
    private boolean isClosed = false;

    public DskimgDirectoryStream(final DskimgFileSystem dskimgFileSystem) {
        this.dskimgFileSystem = dskimgFileSystem;
    }

    @Override
    public Iterator<Path> iterator() {
        if (isClosed) {
            throw new ClosedDirectoryStreamException();
        }

        final Iterator<DskimgPath> pathIterator = dskimgFileSystem.pathIterator();

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
