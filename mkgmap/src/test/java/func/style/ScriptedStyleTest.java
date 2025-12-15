/*
 * Copyright (C) 2010.
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
package func.style;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import func.lib.TestUtils;
import uk.me.parabola.mkgmap.main.StyleTester;

/**
 * A set of tests written as files, using the StyleTester.
 */
public class ScriptedStyleTest {
	private OutputStream output;

	/**
	 * This is to check that the tests are working.  Run a test that fails
	 * on purpose to check that we can detect this.
	 */
	@Test
	public void failureTest() {
		TestUtils.registerFile("styletester.style");
		StyleTester.runSimpleTest(ScriptedStyleTest.class.getResourceAsStream("/rules/fails-on-purpose.fail"));
		String result = output.toString();
		assertTrue(result.contains("ERROR"), "failure check");
	}

	/**
	 * This is really a whole bunch of tests as we find test files in a
	 * directory and run each one in turn.
	 */
	@Test
	public void testAllRuleFiles() throws IOException {
		Path rulesDirPath = Path.of(ScriptedStyleTest.class.getResource("/rules").getPath());
		assertTrue(Files.isDirectory(rulesDirPath));

		// Only run files ending in .test
		FilenameFilter filter = (dir, name) -> {
			if (name.endsWith(".test"))
				return true;
			return false;
		};

		int count = 0;
		
		File[] files = rulesDirPath.toFile().listFiles(filter);
		assert files != null;
		for (File file : files) {
			setup();
			String name = file.getCanonicalPath();
			try (InputStream inputStream = Files.newInputStream(file.toPath())){
				StyleTester.runSimpleTest(inputStream);
			} catch (Exception e) {
				assertFalse(true, name);
			}
			String result = output.toString();

			// Make sure that the result does not contain an error
			if (result.contains("ERROR")) {
				System.out.println(result);
				assertFalse(true, name);
			}

			// make sure that the output was reasonable (and not 'cannot open
			// file', for example).
			assertTrue(result.contains("WAY 1:"), name);

			count++;
		}

		// Check that some tests were run (ie. it will fail if you just delete
		// them all).
		assertTrue(count >= 3, "tests run");
	}

	@BeforeEach
	public void setup() {
		output = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(output);
		StyleTester.setOut(ps);

		// Make sure that there is a given result set
		StyleTester.forceUseOfGiven();
	}
}
