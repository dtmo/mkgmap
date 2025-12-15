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
 * Create date: 29-Nov-2008
 */
package uk.me.parabola.mkgmap.osmstyle.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.reader.osm.Element;
import uk.me.parabola.mkgmap.reader.osm.Way;

public class AddTagActionTest {
	private static final String REFVAL = "A11";
	private static final String PLACENAME = "Trefriw";

	/**
	 * If there are no substitutions, then the exact same string is
	 * used.
	 */
	@Test
	public void testNoSub() {
		String value = "fred";
		Action act = new AddTagAction("a", value, false);
		Element el = stdElement();
		act.perform(el);
		assertSame(value, el.getTag("a"), "a not changed");
	}

	/**
	 * Simple test, substituting the whole string.
	 */
	@Test
	public void testBareSubst() {
		Action act = new AddTagAction("a", "${ref}", false);

		Element el = stdElement();
		act.perform(el);

		assertEquals(REFVAL, el.getTag("a"), "subst ref");
	}

	/**
	 * Complex string with more than one substitution.
	 */
	@Test
	public void testManySubs() {
		Action act = new AddTagAction("a", "Road ${ref}, name ${name:cy}", false);
		Element el = stdElement();
		act.perform(el);

		assertEquals("Road " + REFVAL + ", name " + PLACENAME, el.getTag("a"), "many substitutions");
	}

	/**
	 * If a substitution tag has no value then the value of the tag is not
	 * changed by the action.
	 */
	@Test
	public void testNoValue() {
		Action act = new AddTagAction("a", "Road ${noexist}, name ${name:cy}", true);
		Element el = stdElement();
		String val = "before";
		el.addTag("a", val);
		act.perform(el);
		assertSame(val, el.getTag("a"), "no substitution");
	}

	/**
	 * Test substitutions that get a conversion factor applied to them.
	 */
	@Test
	public void testNumberWithUnit() {
		Action act = new AddTagAction("result", "${ele|conv:m=>ft}", false);

		Element el = stdElement();
		el.addTag("ele", "100");
		act.perform(el);

		assertEquals("328", el.getTag("result"), "subst ref");
	}

	@Test
	public void testSubstWithDefault() {
		Action act = new AddTagAction("result", "${ref|def:default-ref}", true);

		Element el = stdElement();
		act.perform(el);

		assertEquals(REFVAL, el.getTag("result"), "ref not defaulted");

		act = new AddTagAction("result", "${ref|def:default-ref}", true);
		el = stdElement();
		el.deleteTag("ref");
		act.perform(el);
		assertEquals("default-ref", el.getTag("result"), "ref was defaulted");
	}

	/**
	 * Test for the highway symbol substitution.
	 */
	@Test
	public void testHighwaySymbol() {
		Action act = new AddTagAction("a", "${ref|highway-symbol:hbox}", false);

		Element el = stdElement();
		act.perform(el);

		// There should be one of the magic garmin values at the beginning
		// of the string.
		assertEquals("\u0004" + REFVAL, el.getTag("a"), "subst ref");
	}


	/**
	 * The add/set commands now support alternatives just like the name command
	 * has always done.
	 * Several alternatives, but none match.
	 */
	@Test
	public void testNoMatchingAlternatives() {
		AddTagAction act = new AddTagAction("a", "${notset}", false);
		act.add("${hello}");
		act.add("${world}");

		Element el = stdElement();
		act.perform(el);

		assertNull(el.getTag("a"), "a not set");
	}

	/**
	 * Several alternatives and the first one matches.
	 */
	@Test
	public void testFirstAlternativeMatches() {
		AddTagAction act = new AddTagAction("a", "${val}", false);
		act.add("${hello}");
		act.add("${world}");

		Element el = stdElement();
		el.addTag("val", "has value");
		el.addTag("hello", "hello");
		act.perform(el);

		assertEquals("has value", el.getTag("a"), "a is set");
	}

	/**
	 * Several alternatives and the second one matches.
	 */
	@Test
	public void testSecondAlternativeMatches() {
		AddTagAction act = new AddTagAction("a", "${val}", false);
		act.add("${hello}");
		act.add("${world}");

		Element el = stdElement();
		el.addTag("hello", "hello");
		el.addTag("world", "world");
		act.perform(el);

		assertEquals("hello", el.getTag("a"), "a is set");
	}

	private Element stdElement() {
		Element el1 = new Way(1);
		el1.addTag("ref", REFVAL);
		el1.addTag("name:cy", PLACENAME);
		return el1;
	}
}
