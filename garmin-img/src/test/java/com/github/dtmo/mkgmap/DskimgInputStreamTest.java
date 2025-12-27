package com.github.dtmo.mkgmap;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class DskimgInputStreamTest {
    static final Path BASE_MAP_PATH = Path.of("/home/dtmor/garmin/edge_800/gmapbmap.img");
    static final Path OSM_MAP_PATH = Path.of("/home/dtmor/garmin/edge_800/gmapsupp.img");
    static final Path CN_EUROPE_MAP_PATH = Path.of("/home/dtmor/garmin/nuvi/gmapprom1.img");
    static final Path CN_NORTH_AMERICA_MAP_PATH = Path.of("/home/dtmor/garmin/nuvi/gmapprom.img");

    @Test
    void testdskimgFilesystem() throws Exception {
        try (FileSystem filesystem = FileSystems.newFileSystem(BASE_MAP_PATH)) {
            final Path path = filesystem.getPath("006_F006.TRE");
            try (InputStream inputStream = Files.newInputStream(path)) {
                System.out.println("File size: " + Files.size(path));
                for (int i = 0; i < 128; i++) {
                    final byte[] bytes = inputStream.readNBytes(16);
                    System.out.println(
                            IntStream.range(0, bytes.length)
                                    .mapToObj(idx -> Integer.toHexString(Byte.toUnsignedInt(bytes[idx])))
                                    .collect(Collectors.joining(", ")));
                }
            }
        }
    }

    @Test
    void testPathOperations() throws Exception {
        System.out.println(Path.of("").getFileName());
    }
}
