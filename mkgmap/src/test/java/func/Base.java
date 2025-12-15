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
 * Create date: 11-Jan-2009
 */
package func;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import func.lib.Args;
import func.lib.TestUtils;
import uk.me.parabola.imgfmt.fs.FileSystem;
import uk.me.parabola.imgfmt.sys.ImgFS;

/**
 * Base class for tests with some useful routines.  It ensures that created
 * files are deleted before the test starts.
 *
 * @author Steve Ratcliffe
 */
public class Base {
	@BeforeEach
	public void baseSetup() {
		TestUtils.deleteOutputFiles();
	}

	@AfterEach
	public void baseTeardown() {
		TestUtils.closeFiles();
	}

	protected void checkStdFile() {
		assertTrue(new File(Args.DEF_MAP_FILENAME).exists(), "std output file exists");
	}

	protected void checkNoStdFile() {
		assertFalse(new File(Args.DEF_MAP_FILENAME).exists(), "std output file exists");
	}

	protected FileSystem openFs(String filename) throws FileNotFoundException {
		FileSystem fs = ImgFS.openFs(filename);
		TestUtils.registerFile(fs);
		return fs;
	}
}
