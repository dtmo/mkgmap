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
/* Create date: 09-Aug-2009 */
package uk.me.parabola.mkgmap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OptionsTest {
	private static final String[] STD_SINGLE_OPTS = {
		"pool", "ocean"
	};

	static final String PATH_SEP = System.getProperty("file.separator");

	private final List<Option> found = new ArrayList<>();
	private final List<String> options = new ArrayList<>();
	private final List<String> values = new ArrayList<>();

	/**
	 * You can have options with values separated by either a ':' or an
	 * equals sign.
	 */
	@Test
	public void testOptionsWithValues() {
		String s = "three=3\nfour:4\n";
		readOptionsFromString(s);

		assertEquals(2, found.size(), "correct number");

		assertArrayEquals(new String[] {"three", "four"}, options.toArray(), "options");

		assertArrayEquals(new String[] {"3", "4"}, values.toArray(), "values");
	}

	/**
	 * Options do not need to have a value
	 */
	@Test
	public void testOptionsWithoutValues() {
		String s = "pool\nocean\n";
		readOptionsFromString(s);

		assertEquals(2, found.size(), "number of options found");
		assertArrayEquals(STD_SINGLE_OPTS, options.toArray(), "options");
		checkEmptyValues();
	}

	/**
	 * Comments can appear as the first significant character of a line
	 * and cause the rest of the line to be skipped.
	 */
	@Test
	public void testComments() {
		String s = "pool\n" +
				"    # first comment\n" +
				"# a whole line of comment  \n" +
				"ocean\n";

		readOptionsFromString(s);

		assertEquals(2, found.size(), "number of options found");
		assertArrayEquals(STD_SINGLE_OPTS, options.toArray(), "options");
		checkEmptyValues();
	}

	/**
	 * An option can have a long value that is surrounded by braces. All
	 * leading and trailing white space is trimmed.
	 */
	@Test
	public void testLongValues() {
		final String OPT1 = "This is a much longer value\n" +
				"that spans several\n" +
				"lines\n";
		final String OPT2 = "  and here is another, note that there was no new" +
				"line before the option name.";

		String s = "pool {" + OPT1 + "}" +
				"ocean {\n" + OPT2 + "}";
		readOptionsFromString(s);

		System.out.println(options);
		System.out.println(values);
		assertEquals(2, found.size(), "number of options found");
		assertArrayEquals(STD_SINGLE_OPTS, options.toArray(), "options");
		assertEquals(OPT1.trim(), values.get(0), "first value");
		assertEquals(OPT2.trim(), values.get(1), "second value");
	}

	/**
	 * Relative input filenames are relative to the directory of the args
	 * file.
	 * Note: does test work on windows?
	 */
	@Test
	public void testRelativeFilenamesInFile() {
		String s = "input-file: foo\n";

		Options opts = new Options(myOptionProcessor);
		Reader r = new StringReader(s);

		opts.readOptionFile(r, "/bar/string.args");
		String filename = values.get(0);
		File file = new File(filename);
		assertEquals(PATH_SEP + "bar", file.getParent(), "directory part");
		assertEquals("foo", file.getName(), "file part");
	}

	/**
	 * Absolute input filenames are unaffected by the directory that the
	 * args file is in.
	 * Note: does test work on windows?
	 */
	@Test
	public void testAbsoluteFilenamesInFile() {
		String s, exp_dir;
		if ("\\".equals(PATH_SEP)) {
			s = "input-file: c:\\home\\foo\n";
			exp_dir = "c:\\home";
		}
		else {
			s = "input-file: /home/foo\n";
			exp_dir = "/home";
		}

		Options opts = new Options(myOptionProcessor);
		Reader r = new StringReader(s);

		opts.readOptionFile(r, "/bar/string.args");
		System.out.println(Arrays.toString(values.toArray()));

		String filename = values.get(0);
		File file = new File(filename);
		assertEquals(exp_dir, file.getParent(), "directory part");
		assertEquals("foo", file.getName(), "file part");
	}

	private void checkEmptyValues() {
		for (String s : values) {
			assertEquals("", s, "value");
		}
	}

	private void readOptionsFromString(String s) {
		Options opts = new Options(myOptionProcessor);
		Reader r = new StringReader(s);

		opts.readOptionFile(r, "from-string");
	}

	private OptionProcessor myOptionProcessor = opt -> {
		found.add(opt);
		options.add(opt.getOption());
		values.add(opt.getValue());
	};
}
