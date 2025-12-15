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
 * Create date: 10-Jan-2009
 */
package func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.jupiter.api.Test;

import func.lib.Args;
import func.lib.TestUtils;
import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.map.MapReader;
import uk.me.parabola.imgfmt.app.trergn.Point;
import uk.me.parabola.imgfmt.app.trergn.Polyline;
import uk.me.parabola.imgfmt.fs.DirectoryEntry;
import uk.me.parabola.imgfmt.fs.FileSystem;
import uk.me.parabola.mkgmap.main.Main;

/**
 * Very simple checks.  May go away as more detailed checks are developed.
 * 
 * @author Steve Ratcliffe
 */
public class SimpleTest extends Base {

	/**
	 * A very basic check that the size of all the sections has not changed.
	 * This can be used to make sure that a change that is not expected to
	 * change the output does not do so.
	 *
	 * The sizes will have to be always changed when the output does change
	 * though.
	 */
	@Test
	public void testBasic() throws FileNotFoundException {

		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--preserve-element-order",
				Args.TEST_RESOURCE_OSM + "uk-test-1.osm.gz");

		MapReader mr = new MapReader(Args.DEF_MAP_ID + ".img");
		TestUtils.registerFile(mr);
		//FileSystem fs = ImgFS.openFs(Args.DEF_MAP_ID + ".img");
		assertNotNull(mr, "file exists");

		Area bounds = mr.getTreBounds();
		Area expBox = new Area(2402404, -11185, 2407064, -6524);
		assertEquals(expBox, bounds, "bounds of map");

		List<Point> list = mr.pointsForLevel(0, MapReader.WITH_EXT_TYPE_DATA);
		assertEquals(204, list.size(), "number of points at level 0");

		List<Polyline> list1 = mr.linesForLevel(0);
		assertEquals(3289, list1.size(), "number of lines at level 0");
	}

	@Test
	public void testNoSuchFile() {
		Main.mainNoSystemExit("no-such-file-xyz.osm");
		assertFalse(new File(Args.DEF_MAP_FILENAME).exists(), "no file generated");
	}

	@Test
	public void testPolish() throws FileNotFoundException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, Args.TEST_RESOURCE_MP + "test1.mp");

		FileSystem fs = openFs(Args.DEF_MAP_FILENAME);
		assertNotNull(fs, "file exists");

		List<DirectoryEntry> entries = fs.list();
		int count = 0;
		for (DirectoryEntry ent : entries) {
			String ext = ent.getExt();

			int size = ent.getSize();
			switch (ext) {
			case "RGN":
				count++;
				System.out.println("RGN size " + size);
				assertThat(size).isCloseTo(2630, byLessThan(3));
				break;
			case "TRE":
				count++;
				System.out.println("TRE size " + size);
				// Size varies depending on svn modified status
				assertThat(size).isCloseTo(770, byLessThan(2));
				break;
			case "LBL":
				count++;
				assertEquals(999, size, "LBL size");
				break;
			}
		}
		assertTrue(count >= 3, "enough checks run");

	}
}
