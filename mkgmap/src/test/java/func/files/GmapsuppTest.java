/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 or
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
/* Create date: 17-Feb-2009 */
package func.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.Test;

import func.Base;
import func.lib.Args;
import func.lib.Outputs;
import func.lib.TestUtils;
import uk.me.parabola.imgfmt.Utils;
import uk.me.parabola.imgfmt.fs.DirectoryEntry;
import uk.me.parabola.imgfmt.fs.FileSystem;
import uk.me.parabola.imgfmt.fs.ImgChannel;
import uk.me.parabola.imgfmt.mps.MapBlock;
import uk.me.parabola.imgfmt.mps.MpsFileReader;
import uk.me.parabola.imgfmt.mps.ProductBlock;
import uk.me.parabola.imgfmt.sys.ImgFS;
import uk.me.parabola.mkgmap.main.Main;

public class GmapsuppTest extends Base {
	private static final String GMAPSUPP_IMG = "gmapsupp.img";

	@Test
	public void testBasic() throws IOException {
		File f = new File(GMAPSUPP_IMG);
		assertFalse(f.exists(), "does not pre-exist");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		assertTrue(f.exists(), "gmapsupp.img is created");

		FileSystem fs = openFs(GMAPSUPP_IMG);
		DirectoryEntry entry = fs.lookup("63240001.TRE");
		assertNotNull(entry, "first file TRE");
		assertEquals(getFileSize(Args.TEST_RESOURCE_IMG + "63240001.img", "63240001.TRE"), entry.getSize(), "first file TRE size");

		entry = fs.lookup("63240002.TRE");
		assertNotNull(entry, "second file TRE");
		assertEquals(getFileSize(Args.TEST_RESOURCE_IMG + "63240002.img", "63240002.TRE"), entry.getSize(), "second file TRE size");
	}

	/**
	 * Check the values inside the MPS file, when the family id etc is
	 * common to all files.
	 */
	@Test
	public void testMpsFile() throws IOException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=150",
				"--product-id=24",
				"--series-name=tst series",
				"--family-name=tst family",
				"--area-name=tst area",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		MpsFileReader reader = getMpsFile();
		List<MapBlock> list = reader.getMaps();
		reader.close();
		assertEquals(2, list.size(), "number of map blocks");

		// All maps will have the same parameters apart from map name here
		int count = 0;
		for (MapBlock map : list) {
			assertEquals(63240001 + count++, map.getMapNumber(), "map number");
			assertEquals(150, map.getFamilyId(), "family id");
			assertEquals(24, map.getProductId(), "product id");
			assertEquals("tst series", map.getSeriesName(), "series name");
			assertEquals("tst area", map.getAreaName(), "area name");
			assertEquals("uk test " + count, map.getMapDescription(), "map description");
		}
	}

	/**
	 * Test combining gmapsupp files.  The family id etc should be taken from
	 * the MPS file in the gmapsupp.
	 */
	@Test
	public void testCombiningSupps() throws IOException {
		TestUtils.registerFile("g1.img", "g2.img");
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=150",
				"--product-id=24",
				"--series-name=tst series",
				"--family-name=tst family",
				"--area-name=tst area",
				Args.TEST_RESOURCE_IMG + "63240001.img");

		File f = new File("gmapsupp.img");
		f.renameTo(new File("g1.img"));

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=152",
				"--product-id=26",
				"--series-name=tst series 2",
				"--family-name=tst family 2",
				"--area-name=tst area 2",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		f.renameTo(new File("g2.img"));

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--gmapsupp", "g1.img", "g2.img");


		MpsFileReader reader = getMpsFile();
		List<MapBlock> list = reader.getMaps();
		assertEquals(2, list.size(), "number of map blocks");

		for (MapBlock map : list) {
			if (map.getMapNumber() == 63240001) {
				assertEquals(150, map.getFamilyId(), "family id");
				assertEquals(24, map.getProductId(), "product id");
				assertEquals("tst series", map.getSeriesName(), "series name");
				assertEquals("tst area", map.getAreaName(), "area name");
				assertEquals(63240001, map.getHexNumber(), "hex name");
				assertEquals("uk test 1", map.getMapDescription(), "map description");
			} else if (map.getMapNumber() == 63240002) {
				assertEquals(152, map.getFamilyId(), "family id");
				assertEquals(26, map.getProductId(), "product id");
				assertEquals("tst series 2", map.getSeriesName(), "series name");
				assertEquals("tst area 2", map.getAreaName(), "area name");
				assertEquals(63240002, map.getHexNumber(), "hex name");
				assertEquals("uk test 2", map.getMapDescription(), "map description");
			} else {
				assertTrue(false, "Unexpected map found");
			}
		}
	}

	/**
	 * Test the case where we are combining img files with different family
	 * and product ids.
	 */
	@Test
	public void testDifferentFamilies() throws IOException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=101",
				"--product-id=1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				"--family-id=102",
				"--product-id=2",
				"--series-name=tst series2",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		MpsFileReader reader = getMpsFile();
		List<MapBlock> list = reader.getMaps();
		reader.close();
		assertEquals(2, list.size(), "number of map blocks");

		// Directly check the family id's
		assertEquals(101, list.get(0).getFamilyId(), "family in map1");
		assertEquals(102, list.get(1).getFamilyId(), "family in map2");

		// Check more things
		int count = 0;
		for (MapBlock map : list) {
			count++;
			assertEquals(100 + count, map.getFamilyId(), "family in map" + count);
			assertEquals(count, map.getProductId(), "product in map" + count);
			assertEquals("tst series" + count, map.getSeriesName(), "series name in map" + count);
		}
	}

	/**
	 * The mps file has a block for each family/product in the map set.
	 */
	@Test
	public void testProductBlocks() throws IOException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=101",
				"--product-id=1",
				"--family-name=tst family1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				"--family-id=102",
				"--product-id=2",
				"--family-name=tst family2",
				"--series-name=tst series2",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		MpsFileReader reader = getMpsFile();

		List<ProductBlock> products = reader.getProducts();
		products.sort((o1, o2) -> {
			if (o1.getFamilyId() == o2.getFamilyId())
				return 0;
			else if (o1.getFamilyId() > o2.getFamilyId())
				return 1;
			else return -1;
		});

		ProductBlock block = products.get(0);
		assertEquals(101, block.getFamilyId(), "product block first family");
		assertEquals(1, block.getProductId(), "product block first product id");
		assertEquals("tst family1", block.getDescription(), "product block first family name");
		
		block = products.get(1);
		assertEquals(102, block.getFamilyId(), "product block second family");
		assertEquals(2, block.getProductId(), "product block first product id");
		assertEquals("tst family2", block.getDescription(), "product block first family name");
	}

	/**
	 * Make sure that if we have multiple maps in the same family, which after
	 * all is the common case, that we only get one product block.
	 */
	@Test
	public void testProductWithSeveralMaps() throws IOException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--family-id=101",
				"--product-id=1",
				"--family-name=tst family1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		MpsFileReader reader = getMpsFile();
		assertEquals(2, reader.getMaps().size(), "number of map blocks");
		assertEquals(1, reader.getProducts().size(), "number of product blocks");
	}

	@Test
	public void testWithIndex() throws IOException {
		new File("osmmap_mdr.img").delete();
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--index",
				"--latin1",
				"--family-id=101",
				"--product-id=1",
				"--family-name=tst family1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		assertFalse(new File("osmmap_mdr.img").exists());

		// All we are doing here is checking that the file was created and that it is
		// not completely empty.
		FileSystem fs = openFs(GMAPSUPP_IMG);
		ImgChannel r = fs.open("00000101.MDR", "r");
		r.position(2);
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		int read = r.read(buf);
		assertEquals(1024, read);

		buf.flip();
		byte[] b = new byte[3];
		buf.get(b, 0, 3);
		assertEquals('G', b[0]);
	}

	@Test
	public void testWithTwoIndexes() throws IOException {
		TestUtils.registerFile("osmmap_mdr.img", "osmmap.img", "osmmap.tbd", "osmmap.mdx");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--index",
				"--tdbfile",
				"--latin1",
				"--family-id=101",
				"--product-id=1",
				"--family-name=tst family1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img");

		assertTrue(new File("osmmap_mdr.img").exists());

		// All we are doing here is checking that the file was created and that it is
		// not completely empty.
		FileSystem fs = openFs(GMAPSUPP_IMG);
		ImgChannel r = fs.open("00000101.MDR", "r");
		r.position(2);
		ByteBuffer buf = ByteBuffer.allocate(1024);

		int read = r.read(buf);
		assertEquals(1024, read);

		buf.flip();
		byte[] b = new byte[3];
		buf.get(b, 0, 3);
		assertEquals('G', b[0]);
	}

	/**
	 * If there are files in two (or more) families then there should be a MDR and SRT for each.
	 */
	@Test
	public void testTwoFamilyIndex() throws IOException {
		TestUtils.registerFile("osmmap_mdr.img", "osmmap.img", "osmmap.tbd", "osmmap.mdx");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--index",
				"--latin1",
				"--family-id=101",
				"--product-id=1",
				"--family-name=tst family1",
				"--series-name=tst series1",
				Args.TEST_RESOURCE_OSM + "uk-test-1.osm.gz",
				"--family-id=202",
				"--family-name=tst family2",
				"--series-name=tst series2",
				Args.TEST_RESOURCE_OSM + "uk-test-2.osm.gz");

		assertFalse(new File("osmmap_mdr.img").exists());

		// All we are doing here is checking that the file was created and that it is
		// not completely empty.
		FileSystem fs = openFs(GMAPSUPP_IMG);
		ImgChannel r = fs.open("00000101.MDR", "r");
		r.position(2);
		ByteBuffer buf = ByteBuffer.allocate(1024);
		int read = r.read(buf);
		assertEquals(1024, read);

		fs = openFs(GMAPSUPP_IMG);
		r = fs.open("00000202.MDR", "r");
		r.position(2);
		buf.clear();
		read = r.read(buf);
		assertEquals(1024, read);

		r = fs.open("00000202.SRT", "r");
		buf = ByteBuffer.allocate(512);
		read = r.read(buf);
		assertEquals(512, read);
		r = fs.open("00000101.SRT", "r");
		buf.clear();
		read = r.read(buf);
		assertEquals(512, read);
	}

	/**
	 * If no code page is given for the index, it is taken from the input files.
	 */
	@Test
	public void testImplicitCodePageIndex() throws IOException {
		TestUtils.registerFile("osmmap_mdr.img", "osmmap.img", "osmmap.tbd", "osmmap.mdx");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--code-page=1256",
				Args.TEST_RESOURCE_OSM + "uk-test-1.osm.gz");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--gmapsupp", "--index", "63240001.img");

		assertFalse(new File("osmmap_mdr.img").exists());

		FileSystem fs = openFs(GMAPSUPP_IMG);
		ImgChannel r = fs.open("00006324.MDR", "r");

		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		r.read(buf);
		assertEquals(1256, buf.getChar(0x15));
		assertEquals(0x020009, buf.getInt(0x17));
	}

	/**
	 * If there are mis-matching code-pages in the input files there should be a warning.
	 */
	@Test
	public void testWarningOnMismatchedCodePages() {
		TestUtils.registerFile("osmmap.img");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--route", "--code-page=1256",
				Args.TEST_RESOURCE_OSM + "uk-test-1.osm.gz",
				"--latin1",
				Args.TEST_RESOURCE_OSM + "uk-test-2.osm.gz");

		Outputs outputs = TestUtils.runAsProcess(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				"--index",

				"63240001.img",
				"63240002.img"
		);

		outputs.checkError("different code page");
	}

	@Test
	public void testWithTypFile() throws IOException {
		File f = new File(GMAPSUPP_IMG);
		assertFalse(f.exists(), "does not pre-exist");

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG,
				"--gmapsupp",
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_TYP + "test.txt");

		assertTrue(f.exists(), "gmapsupp.img is created");

		FileSystem fs = openFs(GMAPSUPP_IMG);
		DirectoryEntry entry = fs.lookup("63240001.TRE");
		assertNotNull(entry, "first file TRE");
		assertEquals(getFileSize(Args.TEST_RESOURCE_IMG + "63240001.img", "63240001.TRE"), entry.getSize(), "first file TRE size");

		entry = fs.lookup("0000TEST.TYP");
		assertNotNull(entry, "contains typ file");
	}

	private MpsFileReader getMpsFile() throws IOException {
		FileSystem fs = openFs(GMAPSUPP_IMG);
		MpsFileReader reader = new MpsFileReader(fs.open("MAKEGMAP.MPS", "r"), 0);
		TestUtils.registerFile(reader);
		return reader;
	}

	private int getFileSize(String imgName, String fileName) throws IOException {
		FileSystem fs = ImgFS.openFs(imgName);
		try {
			return fs.lookup(fileName).getSize();
		} finally {
			Utils.closeFile(fs);
		}
	}
}
