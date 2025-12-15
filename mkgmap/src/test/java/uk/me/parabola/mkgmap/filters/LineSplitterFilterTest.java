/*
 * Copyright (C) 2017.
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

package uk.me.parabola.mkgmap.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.me.parabola.imgfmt.app.Coord;
import uk.me.parabola.mkgmap.build.LayerFilterChain;
import uk.me.parabola.mkgmap.general.MapElement;
import uk.me.parabola.mkgmap.general.MapLine;

/**
 * Test for the {@link LineSplitterFilter}.
 * @author Gerd Petermann
 *
 */
public class LineSplitterFilterTest {
	
	@Test
	public void testSizes(){
		List<Coord> points = new ArrayList<>();
		points.add(new Coord(1,1));
		for (int n = 2; n < 10 * LineSplitterFilter.MAX_POINTS_IN_LINE; n++) {
			points.add(new Coord(n,n));
			test(points);
		}
	}	
	
	private void test(List<Coord> points) {
		MapLine l = new MapLine();
		l.setPoints(points);
		FilterConfig config = new FilterConfig() {{
			setResolution(24);
			setLevel(0);
		}};
		LayerFilterChain chain = new LayerFilterChain(config);
		LineSplitterFilter filter = new LineSplitterFilter();
		filter.init(config);
		chain.addFilter(filter);
		TestFilter testFilter = new TestFilter(l);
		chain.addFilter(testFilter);
		chain.doFilter(l);
		testFilter.check();
	}
	
	private class TestFilter extends BaseFilter implements MapFilter {
		final MapLine origLine; 
		final int origSize;
		int count;
		int newSize;
		Coord lastPoint;
		
		TestFilter(MapLine orig) {
			origLine = orig;
			origSize = orig.getPoints().size();
		}

		public void check() {
			assertEquals(origSize, newSize, "final test " + origSize);
			int neededParts = 1;
			int rem = origSize - LineSplitterFilter.MAX_POINTS_IN_LINE;
			if (rem > 0) { 
				neededParts += rem / (LineSplitterFilter.MAX_POINTS_IN_LINE - 1);
				if (rem % (LineSplitterFilter.MAX_POINTS_IN_LINE - 1) != 0)
					++neededParts; 
			}
			assertEquals(neededParts, count, "too many parts " + origSize);
		}

		public void doFilter(MapElement element, MapFilterChain next) {
			MapLine line = (MapLine) element;
			int n = line.getPoints().size();
			count++;
			assertTrue(n <= LineSplitterFilter.MAX_POINTS_IN_LINE,"too many points " + origSize);
			if (origLine.getPoints().size() >= LineSplitterFilter.MAX_POINTS_IN_LINE / 2)
				assertTrue(n >= LineSplitterFilter.MAX_POINTS_IN_LINE / 2, "too few points in part" + origSize + " " + n);
			if (newSize == 0) {
				newSize = n;
			} else {
				assertTrue(lastPoint == line.getPoints().get(0), "new part doesn't start with same point " + origSize);
				newSize += n - 1;
				ArrayList<Coord> points = new ArrayList<>();
				assertTrue(points.size() <= origSize, "parts are wrong " + origSize);				
			}
			lastPoint = line.getPoints().get(n - 1);
		}
	}
	
}
