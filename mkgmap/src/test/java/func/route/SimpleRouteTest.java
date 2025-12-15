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
/* Create date: 16-Feb-2009 */
package func.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.jupiter.api.Test;

import func.Base;
import func.lib.Args;
import uk.me.parabola.imgfmt.fs.DirectoryEntry;
import uk.me.parabola.imgfmt.fs.FileSystem;
import uk.me.parabola.mkgmap.main.Main;

public class SimpleRouteTest extends Base {

	/**
	 * Simple test to ensure that nothing has changed.  Of course
	 * if the output should have changed, then this will have to be altered
	 * to match.
	 */
	@Test
	public void testSize() throws FileNotFoundException {
		Main.mainNoSystemExit(Args.TEST_STYLE_ARG, "--preserve-element-order",
				"--route", Args.TEST_RESOURCE_OSM + "uk-test-1.osm.gz", Args.TEST_RESOURCE_MP + "test1.mp");

		FileSystem fs = openFs(Args.DEF_MAP_ID + ".img");
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
				assertThat(size).isCloseTo(126035, withinPercentage(5));
				break;
			case "TRE":
				count++;
				System.out.println("TRE size " + size);
				// Size varies depending on svn modified status
				assertThat(size).isCloseTo(1413, byLessThan(2));
				break;
			case "LBL":
				count++;
				assertEquals(28742, size, "LBL size");
				break;
			case "NET":
				count++;
				assertEquals(66591, size, "NET size");
				break;
			case "NOD":
				count++;
				System.out.println("NOD size " + size);
				assertEquals(169689, size, "NOD size");
				break;
			}
		}
		assertTrue(count == 5, "enough checks run");

		fs = openFs(Args.DEF_MAP_FILENAME2);
		assertNotNull(fs, "file exists");

		entries = fs.list();
		count = 0;
		for (DirectoryEntry ent : entries) {
			String ext = ent.getExt();

			int size = ent.getSize();
			switch (ext) {
			case "RGN":
				count++;
				System.out.println("RGN size " + size);
				assertThat(size).isCloseTo(2743, byLessThan(3));
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
			case "NET":
				count++;
				assertEquals(1301, size, "NET size");
				break;
			case "NOD":
				count++;
				System.out.println("NOD size " + size);
				assertEquals(3584, size, "NOD size");
				break;
			}
		}
		assertTrue(count == 5, "enough checks run");
	}
}
