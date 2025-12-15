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
 * Create date: 29-Nov-2008
 */
package uk.me.parabola.mkgmap.general;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.Coord;

public class LineClipperTest {

	/**
	 * This is the example as given on the referenced web page.
	 * We now use integers instead of floats so the 101.425 from the
	 * example is just 101 here.
	 */
	@Test
	public void testExampleClip() {
		Area a = new Area(60, 70, 150, 230);
		Coord[] co = {
				new Coord(20, 30),
				new Coord(160, 280),
		};

		List<List<Coord>> listList = LineClipper.clip(a, Arrays.asList(co));
		assertTrue(!listList.isEmpty(), "list should be empty");

		Coord[] result = {
				new Coord(60, 101),
				new Coord(132, 230)
		};
		assertArrayEquals(result, listList.get(0).toArray(), "example result");
	}

	/**
	 * Test an original line that enters the area, leaves it and then goes back
	 * into the area.  This should give two lines in the result set.
	 */
	@Test
	public void testListClip() {
		// Add your code here
		Area a = new Area(100, 100, 200, 200);
		List<Coord> l = Arrays.asList(new Coord(20, 30),
				new Coord(40, 60),
				new Coord(102, 110),
				new Coord(150, 150),
				new Coord(210, 220),
				new Coord(190, 135)
				);
		List<List<Coord>> list = LineClipper.clip(a, l);

		// There should be exactly two lines
		assertEquals(2, list.size(), "should be two lines");

		// No empty lists
		for (List<Coord> lco : list)
			assertTrue(!lco.isEmpty(), "empty list");

		// Check values
		Coord[] firstExpectedLine = {
				new Coord(100, 108),
				new Coord(102, 110),
				new Coord(150, 150),
				new Coord(193, 200)
		};
		assertArrayEquals(firstExpectedLine, list.get(0).toArray());
		Coord[] secondExpectedLine = {
				new Coord(200, 178),
				new Coord(190, 135)
		};
		assertArrayEquals(secondExpectedLine, list.get(1).toArray());
	}

	/**
	 * If all the lines are inside, then it should just return null to indicate that.
	 */
	@Test
	public void testAllInside() {
		Area a = new Area(100, 100, 200, 200);
		List<Coord> l = Arrays.asList(
				new Coord(102, 110),
				new Coord(150, 150),
				new Coord(190, 195)
				);
		List<List<Coord>> list = LineClipper.clip(a, l);
		assertNull(list, "all lines inside");
	}
}
