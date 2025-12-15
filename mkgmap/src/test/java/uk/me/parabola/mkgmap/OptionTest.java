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
/* Create date: 08-Aug-2009 */
package uk.me.parabola.mkgmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OptionTest {
	/** If an option does not have a value, then the value is the empty
	 * string.
	 */
	@Test
	public void testOptionWithoutValue() {
		Option o = new Option("hello");
		assertEquals("hello", o.getOption(), "name");
		assertEquals("", o.getValue(), "value");
	}


	@Test
	public void testOption() {
		Option o = new Option("hello", "world");
		assertEquals("hello", o.getOption(), "name");
		assertEquals("world", o.getValue(), "value");
		assertFalse(o.isExperimental(), "not experimental");
	}

	/**
	 * Regular option, parsed in constructor.
	 */
	@Test
	public void testParseOption() {
		Option o = new Option("hello=world");
		assertEquals("hello", o.getOption(), "name");
		assertEquals("world", o.getValue(), "value");
		assertFalse(o.isExperimental(), "not experimental");
	}

	/**
	 * Test for an experimental option.  These begin with 'x-' but are otherwise
	 * treated as if the 'x-' was not there.
	 */
	@Test
	public void testIsExperimental() {
		Option o = new Option("x-hello=world");
		assertEquals("hello", o.getOption(), "name");
		assertEquals("world", o.getValue(), "value");
		assertTrue(o.isExperimental(), "experimental");
	}

	/**
	 * Test for an negative option eg: no-route. These begin with the prefix 'no-'.
	 * The option name is without the prefix and a flag is set to show that the option
	 * is being reset.
	 */
	@Test
	public void testOptionReset() {
		Option o = new Option("no-hello");
		assertEquals("hello", o.getOption(), "name");
		assertEquals(null, o.getValue(), "value");
		assertTrue(o.isReset(), "reset");
	}
}
