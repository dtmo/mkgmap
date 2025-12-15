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
package uk.me.parabola.imgfmt.app.srt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.Collator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.srt.SrtTextReader;


public class SrtCollatorTest {

	private Collator collator;

	@BeforeEach
	public void setUp() {
		Sort sort = SrtTextReader.sortForCodepage(1252);
		collator = sort.getCollator();
	}

	/**
	 * Test primary strength comparisons.
	 */
	@Test
	public void testPrimary() {
		collator.setStrength(Collator.PRIMARY);
		assertEquals(0, collator.compare("AabBb", "aabbb"), "prim: different case");
		assertEquals(0, collator.compare("aabBb", "aabbb"), "prim: different case");
		assertEquals(-1, collator.compare("AabB", "aabbb"), "prim: different length");
		assertEquals(-1, collator.compare("aaac", "aaad"), "prim: different letter");
		assertEquals(1, collator.compare("aaae", "aaad"), "prim: different letter");
		assertEquals(0, collator.compare("aaaa", "aaaa"));
		assertEquals(0, collator.compare("aáÄâ", "aaaa"));
	}

	@Test
	public void testSecondary() {
		collator.setStrength(Collator.SECONDARY);
		assertEquals(0, collator.compare("AabBb", "aabbb"));
		assertEquals(0, collator.compare("aabBb", "aabBb"));
		assertEquals(0, collator.compare("aabbB", "aabBb"));
		assertEquals(1, collator.compare("aáÄâ", "aaaa"));
		assertEquals(-1, collator.compare("aáÄâ", "aaaaa"), "prim len diff");
		assertEquals(-1, collator.compare("aáÄâa", "aaaab"));
	}

	@Test
	public void testTertiary() {
		collator.setStrength(Collator.TERTIARY);
		assertEquals(1, collator.compare("AabBb", "aabbb"), "prim: different case");
		assertEquals(1, collator.compare("AabBb", "aabbb"));
		assertEquals(0, collator.compare("aabBb", "aabBb"));
		assertEquals(-1, collator.compare("aabbB", "aabBb"));
		assertEquals(-1, collator.compare("aAbb", "aabbb"));
		assertEquals(1, collator.compare("t", "a"));
		assertEquals(1, collator.compare("ß", "a"));
		assertEquals(-1, collator.compare("ESA", "Eß"));
		assertEquals(-1, collator.compare(":.e", "\u007fæ"));
		assertEquals(-1, collator.compare(";œ", ";Œ"));
		assertEquals(-1, collator.compare("œ;", "Œ;"));
	}

	/**
	 * Test that ignorable characters do not affect the result in otherwise identical strings.
	 */
	@Test
	public void testIgnoreable() throws Exception {
		assertEquals(0, collator.compare("\u0008fred", "fred"), "ignorable at beginning");
		assertEquals(0, collator.compare("fred\u0008", "fred"), "ignorable at end");
		assertEquals(0, collator.compare("fr\u0008ed", "fred"), "ignorable in middle");
		assertEquals(1, collator.compare("\u0001A", "A\u0008"));

		collator.setStrength(Collator.PRIMARY);
		assertEquals(0, collator.compare("AabBb\u0008", "aabbb"), "prim: different case");
	}

	@Test
	public void testSecondaryIgnorable() {
		assertEquals(-1, collator.compare("A", "A\u0001"));
	}

	@Test
	public void testLengths() {
		assertEquals(-1, collator.compare("-Û", "-ü:X"));
		assertEquals(-1, collator.compare("-Û", "-Û$"));
		assertEquals(-1, collator.compare("-ü:X", "-Û$"));
		assertEquals(-1, collator.compare("–", "–X"));
		assertEquals(1, collator.compare("–TÛ‡²", "–"));
	}

	@Test
	public void testSpaces() {
		assertEquals(1, collator.compare("øþõ Ñ", "õþO"));
	}

	/**
	 * Test using the java collator, to experiment. Note that our implementation is not
	 * meant to be identical to the java one.
	 */
	@Test
	public void testJavaRules() {
		Collator collator = Collator.getInstance();

		// Testing ignorable
		assertEquals(0, collator.compare("\u0001fred", "fred"));
		assertEquals(0, collator.compare("fre\u0001d", "fred"));

		collator.setStrength(Collator.PRIMARY);
		assertEquals(0, collator.compare("AabBb", "aabbb"), "prim: different case");
		assertEquals(0, collator.compare("aabBb", "aabbb"), "prim: different case");
		assertEquals(-1, collator.compare("AabB", "aabbb"), "prim: different length");
		assertEquals(-1, collator.compare("aaac", "aaad"), "prim: different letter");
		assertEquals(1, collator.compare("aaae", "aaad"), "prim: different letter");
		assertEquals(0, collator.compare("aaaa", "aaaa"));
		assertEquals(0, collator.compare("aáÄâ", "aaaa"));

		collator.setStrength(Collator.SECONDARY);
		assertEquals(0, collator.compare("AabBb", "aabbb"));
		assertEquals(0, collator.compare("aabBb", "aabBb"));
		assertEquals(0, collator.compare("aabbB", "aabBb"));
		assertEquals(1, collator.compare("aáÄâ", "aaaa"));
		assertEquals(-1, collator.compare("aáÄâ", "aaaaa"), "prim len diff");
		assertEquals(-1, collator.compare("aáÄâa", "aaaab"));

		collator.setStrength(Collator.TERTIARY);
		assertEquals(1, collator.compare("AabBb", "aabbb"), "prim: different case");
		assertEquals(1, collator.compare("AabBb", "aabbb"));
		assertEquals(0, collator.compare("aabBb", "aabBb"));
		assertEquals(-1, collator.compare("aabbB", "aabBb"));
		assertEquals(-1, collator.compare("aAbb", "aabbb"));
		assertEquals(1, collator.compare("t", "a"));
	}

}
