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
package uk.me.parabola.mkgmap.osmstyle.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for values that may or may not have an associated unit (eg 70mph).
 */
public class ValueWithUnitTest {

	@Test
	public void testBareInteger() {
		ValueWithUnit val = new ValueWithUnit("23");
		assertTrue(val.isValid(), "valid value");
	}

	@Test
	public void testInvalidNumber() {
		ValueWithUnit val = new ValueWithUnit("abc");
		assertFalse(val.isValid(), "invalid value");
	}

	@Test
	public void testIntegerCompare() {
		ValueWithUnit val1 = new ValueWithUnit("23");
		ValueWithUnit val2 = new ValueWithUnit("24");

		assertEquals(-1, val1.compareTo(val2));
		assertEquals(1, val2.compareTo(val1));
	}

	@Test
	public void testDecimalNumber() {
		ValueWithUnit val = new ValueWithUnit("23.3");
		assertTrue(val.isValid(), "valid value");
	}

	/**
	 * Test that 23.5 is not the same as 23 alone. (Checks that the decimal part
	 * is not being stripped off as the unit.)
	 */
	@Test
	public void testDecimalNotEqualToInteger() {
		ValueWithUnit val1 = new ValueWithUnit("23.5");
		ValueWithUnit val2 = new ValueWithUnit("23");

		assertEquals(1, val1.compareTo(val2));
	}

	@Test
	public void testDecimalCompare() {
		ValueWithUnit val1 = new ValueWithUnit("23.45");
		ValueWithUnit val2 = new ValueWithUnit("23.46");

		assertEquals(-1, val1.compareTo(val2));
		assertEquals(1, val2.compareTo(val1));
	}

	/**
	 * Make sure that 2.0 is equal to 2 at this level.
	 */
	@Test
	public void testCompareWithDifferentScales() {
		ValueWithUnit val1 = new ValueWithUnit("23");
		ValueWithUnit val2 = new ValueWithUnit("23.0");

		assertEquals(0, val1.compareTo(val2));
		assertEquals(0, val2.compareTo(val1));
	}

	/**
	 * Test something that looks like a number but has two decimal points.
	 * Used to cause an exception.
	 */
	@Test
	public void testTwoDPs() {
		ValueWithUnit val = new ValueWithUnit("de.08315102.reistenhofweg");
		ValueWithUnit zero = new ValueWithUnit("0");
		assertEquals(0, val.compareTo(zero));
		assertFalse(val.isValid());
	}
}
