/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 30-Nov-2008
 */
package uk.me.parabola.mkgmap.osmstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.reader.osm.Style;
import uk.me.parabola.mkgmap.reader.osm.StyleInfo;

/**
 * Tests for reading in a complete style.
 */
public class StyleImplTest {
	private static final String STYLE_LOC = "classpath:teststyles";

	@Test
	public void testGetInfo() throws FileNotFoundException {
		StyleImpl style = new StyleImpl(STYLE_LOC, "simple");

		printStyle(style);
		StyleInfo info = style.getInfo();
		assertEquals("2.2", info.getVersion(), "version");
		assertEquals("A simple test style with just one example of most things", info.getSummary(), "version");
		assertEquals("This style is used for testing.", info.getLongDescription(), "version");
	}

	@Test
	public void testGetOption() throws FileNotFoundException {
		StyleImpl style = new StyleImpl(STYLE_LOC, "simple");

		String val = style.getOption("levels");

		assertEquals("0:24\n1:20", val, "option levels");
	}

	@Test
	public void testEmptyFiles() throws FileNotFoundException {
		StyleImpl style = new StyleImpl(STYLE_LOC, "empty");
		assertNotNull(style, "read style ok");
	}

	/**
	 * The case when a style name does not exist.  This has always worked in
	 * the way you would expect - there is an error if it does not exist.
	 */
	@Test
	public void testBadStyleName() {
		assertThrows(FileNotFoundException.class, () -> {
			Style style = new StyleImpl(STYLE_LOC, "no-such-style");
			if (style != null) style= null; // pseudo use the value to calm down FindBugs
		});
	}

	/**
	 * This tests the case when a style-file is given but does not exist.
	 * This has always worked as expected, ie given an error.
	 */
	@Test
	public void testBadStyleFileOnClasspath() {
		assertThrows(FileNotFoundException.class, () -> {
			Style style = new StyleImpl("classpath:no-such-place", "default");
			if (style != null) style= null; // pseudo use the value to calm down FindBugs
		});
	}

	/**
	 * Test the case where a style file location is given that does not exist.
	 * Previously it used to default to classpath:styles if it did not exist
	 * which was confusing.
	 */
	@Test
	public void testBadStyleFileOnFilesystem() {
		assertThrows(FileNotFoundException.class, () -> {
			Style style = new StyleImpl("/no-such-place/hopefully", "default");
			if (style != null) style= null; // pseudo use the value to calm down FindBugs
		});
	}

	private void printStyle(StyleImpl in) {
		in.dumpToFile(new OutputStreamWriter(System.out));
	}
}
