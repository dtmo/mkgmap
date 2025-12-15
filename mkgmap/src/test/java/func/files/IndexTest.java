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
package func.files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import func.Base;
import func.lib.Args;
import func.lib.Outputs;
import func.lib.TestUtils;
import uk.me.parabola.imgfmt.fs.DirectoryEntry;
import uk.me.parabola.imgfmt.fs.FileSystem;


public class IndexTest extends Base {
	private static final String OVERVIEW_NAME = "testname";
	private static final String MDR_IMG = OVERVIEW_NAME + "_mdr.img";

	@Test
	public void testCreateIndex() throws IOException {
		File f = new File(MDR_IMG);
		f.delete();
		assertFalse(f.exists(), "does not pre-exist");

		Outputs outputs = TestUtils.runAsProcess(
				Args.TEST_STYLE_ARG,
				"--index",
				"--latin1",
				"--family-id=1002",
				"--overview-mapname=" + OVERVIEW_NAME,
				Args.TEST_RESOURCE_IMG + "63240001.img",
				Args.TEST_RESOURCE_IMG + "63240002.img"
		);
		outputs.checkError("Number of ExitExceptions: 0");

		TestUtils.registerFile(MDR_IMG);
		TestUtils.registerFile(OVERVIEW_NAME+".tdb");
		TestUtils.registerFile(OVERVIEW_NAME+".mdx");
		TestUtils.registerFile(OVERVIEW_NAME+".img");

		assertTrue(f.exists(), MDR_IMG + " is created");

		FileSystem fs = openFs(MDR_IMG);
		DirectoryEntry entry = fs.lookup(OVERVIEW_NAME.toUpperCase() + ".MDR");
		assertNotNull(entry, "Contains the MDR file");

		entry = fs.lookup(OVERVIEW_NAME.toUpperCase() + ".SRT");
		assertNotNull(entry, "contains the SRT file");
		fs.close();
	}
}
