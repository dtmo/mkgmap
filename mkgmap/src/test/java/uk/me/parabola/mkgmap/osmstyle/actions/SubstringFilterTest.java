/*
 * Copyright (C) 2014.
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

package uk.me.parabola.mkgmap.osmstyle.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.scan.SyntaxException;

public class SubstringFilterTest {

	@Test
	public void testOneArg() {
		SubstringFilter filter = new SubstringFilter("2");
		String s = filter.doFilter("abcd", null);
		assertEquals("cd", s);
	}

	@Test
	public void testTwoArgs() {
		SubstringFilter filter = new SubstringFilter("2:4");
		String s = filter.doFilter("abcdefg", null);
		assertEquals("cd", s);
	}

	@Test
	public void testBadArgs() {
		assertThrows(SyntaxException.class, () -> {
			SubstringFilter filter = new SubstringFilter("6:4");
			filter.doFilter("abc", null);
		});
	}

	@Test
	public void testEmptyArgs() {
		assertThrows(SyntaxException.class, () -> {
			SubstringFilter filter = new SubstringFilter("");
			String s = filter.doFilter("abcde", null);
			assertEquals("abcde", s);
		});
	}

	@Test
	public void testTooManyArgs() {
		assertThrows(SyntaxException.class, () -> {
			SubstringFilter filter = new SubstringFilter("1:2:3");
			filter.doFilter("abc", null);
		});
	}

	@Test
	public void testRangeLargerThanInput() {
		SubstringFilter filter = new SubstringFilter("2:30");
		String s = filter.doFilter("abcdef", null);
		assertEquals("cdef", s);
	}
	@Test
	public void testStartLargerThanInput() {
		SubstringFilter filter = new SubstringFilter("10");
		String s = filter.doFilter("abcdef", null);
		assertEquals(null, s);
	}
}
