/*
 * Copyright (C) 2014
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
/* Create date: 28-Nov-2014 */
package uk.me.parabola.mkgmap.osmstyle.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 
 * @author GerdP
 *
 */
public class CountryISOFilterTest {
	/**
	 * Test different inputs for the country-ISO filter
	 */
	@Test
	public void testDoFilter() {
		CountryISOFilter filter = new CountryISOFilter();
		String s;
		s = filter.doFilter("Germany", null);
		assertEquals("DEU", s, "Germany");
		s = filter.doFilter("Deutschland", null);
		assertEquals("DEU", s, "Deutschland");
		s = filter.doFilter("United Kingdom", null);
		assertEquals("GBR", s, "United Kingdom");
		s = filter.doFilter("UNITED KINGDOM", null);
		assertEquals("GBR", s, "UNITED KINGDOM");
		s = filter.doFilter("united kingdom", null);
		assertEquals("GBR", s, "united kingdom");
		s = filter.doFilter("UK", null);
		assertEquals("GBR", s, "UK");
		s = filter.doFilter("xyz", null);
		assertEquals("xyz", s, "xyz");
		s = filter.doFilter("Ελλάδα", null);
		assertEquals("GRC", s, "Ελλάδα");
		s = filter.doFilter("  germany ", null);
		assertEquals("DEU", s, "  germany ");
	}
}
