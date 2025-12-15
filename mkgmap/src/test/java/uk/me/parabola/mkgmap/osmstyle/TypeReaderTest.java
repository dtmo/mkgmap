/*
 * Copyright (C) 2011.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 or
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.mkgmap.osmstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.general.LevelInfo;
import uk.me.parabola.mkgmap.reader.osm.FeatureKind;
import uk.me.parabola.mkgmap.reader.osm.GType;
import uk.me.parabola.mkgmap.scan.TokenScanner;

public class TypeReaderTest {

	@Test
	public void testLevel() {
		GType gType = makeType("[0x1 level 2]");

		assertEquals(0, gType.getMinLevel(), "min level 0");
		assertEquals(2, gType.getMaxLevel(), "max level");

		assertEquals(18, gType.getMinResolution(), "min res");
		assertEquals(24, gType.getMaxResolution(), "max res");
	}

	@Test
	public void testLevelRange() {
		GType gType = makeType("[0x1 level 1-3]");

		assertEquals(1, gType.getMinLevel(), "min level");
		assertEquals(3, gType.getMaxLevel(), "max level");

		assertEquals(16, gType.getMinResolution(), "min res");
		assertEquals(20, gType.getMaxResolution(), "min res");
	}

	@Test
	public void testResolution() {
		GType gType = makeType("[0x1 resolution 18]");

		assertEquals(0, gType.getMinLevel(), "min level 0");
		assertEquals(2, gType.getMaxLevel(), "max level");

		assertEquals(18, gType.getMinResolution(), "min res");
		assertEquals(24, gType.getMaxResolution(), "max res");
	}

	@Test
	public void testResolutionRange() {
		GType gType = makeType("[0x1 resolution 16-20]");

		assertEquals(16, gType.getMinResolution(), "min res");
		assertEquals(20, gType.getMaxResolution(), "max res");
		
		assertEquals(1, gType.getMinLevel(), "min level");
		assertEquals(3, gType.getMaxLevel(), "max level");

	}

	private GType makeType(String in) {
		LevelInfo[] levels = LevelInfo.createFromString("0:24 1:20 2:18 3:16 4:14");

		TypeReader tr = new TypeReader(FeatureKind.POLYLINE, levels);
		TokenScanner ts = new TokenScanner("string", new StringReader(in));
		ts.setExtraWordChars("-:");

		return tr.readType(ts);
	}
}
