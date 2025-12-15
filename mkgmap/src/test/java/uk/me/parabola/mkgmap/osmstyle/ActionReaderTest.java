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
package uk.me.parabola.mkgmap.osmstyle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.osmstyle.actions.Action;
import uk.me.parabola.mkgmap.osmstyle.actions.ActionReader;
import uk.me.parabola.mkgmap.reader.osm.Element;
import uk.me.parabola.mkgmap.reader.osm.GeneralRelation;
import uk.me.parabola.mkgmap.reader.osm.Relation;
import uk.me.parabola.mkgmap.reader.osm.Rule;
import uk.me.parabola.mkgmap.reader.osm.TypeResult;
import uk.me.parabola.mkgmap.reader.osm.Way;
import uk.me.parabola.mkgmap.scan.SyntaxException;
import uk.me.parabola.mkgmap.scan.TokenScanner;

/**
 * Test the possible actions that can appear in an action block.
 * These are run before any rule is finally matched.
 */
public class ActionReaderTest {

	@Test
	public void testSimpleSet() {
		List<Action> actions = readActionsFromString("{set park=yes}");
		assertEquals(1, actions.size(), "one action");

		Element el = stdElementRun(actions);

		assertEquals("yes", el.getTag("park"), "park overwritten");
	}

	@Test
	public void testSimpleAdd() {
		List<Action> actions = readActionsFromString("{add park=yes}");
		assertEquals(1, actions.size(), "one action");

		// add does not overwrite existing tags.
		Element el = stdElementRun(actions);
		assertEquals("no", el.getTag("park"), "park not overwritten");
	}

	@Test
	public void testRename() {
		List<Action> actions = readActionsFromString("{rename park landarea}");
		assertEquals(1, actions.size(), "one action");

		Element el = stdElementRun(actions);
		assertNull(el.getTag("park"), "park should be gone");
		assertEquals("no", el.getTag("landarea"), "park renamed");
	}

	/**
	 * Test with embedded comment, newlines, semicolon used as separator.
	 */
	@Test
	public void testFreeForm() {
		List<Action> actions = readActionsFromString(" { set web='world wide';" +
				"set \nribbon = 'yellow' \n# a comment } ");

		assertEquals(2, actions.size(), "number of actions");
		Element el = stdElementRun(actions);
		assertEquals("no", el.getTag("park"), "park not overwritten");
		assertEquals("world wide", el.getTag("web"), "word with spaces");
		assertEquals("yellow", el.getTag("ribbon"), "yellow ribbon");
	}

	/**
	 * Test several commands in the block.  They should all be executed.
	 */
	@Test
	public void testMultipleCommands() {
		List<Action> actions = readActionsFromString(
				"{set park=yes; add fred=other;" +
						"set pooh=bear}");

		assertEquals(3, actions.size(), "number of actions");

		Element el = stdElementRun(actions);

		assertEquals("yes", el.getTag("park"), "park set to yes");
		assertEquals("other", el.getTag("fred"), "fred set");
		assertEquals("bear", el.getTag("pooh"), "pooh set");
	}

	@Test
	public void testInvalidCommand() {
		assertThrows(SyntaxException.class, () -> readActionsFromString("{bad }"));
	}

	/**
	 * The name action set the element-name (not the 'name' tag).
	 * The first value to set it counts, later matches are ignored.
	 */
	@Test
	public void testName() {
		List<Action> actions = readActionsFromString("{name '${name} (${ref})' |" +
				"  '${ref}' | '${name}' ; }");
		Element el = makeElement();
		el.addTag("name", "Main St");
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("Main St", el.getName(), "just name");
	}

	/**
	 * Test with two name actions.  This works just the same as having several
	 * name options on the same name command, in that it is still the
	 * first one to match that counts.
	 */
	@Test
	public void testDoubleName() {
		List<Action> actions = readActionsFromString("{name '${name} (${ref})' |" +
				"  '${ref}' | '${name}' ; " +
				" name 'fred';}");

		// Something that matches nothing in the first name command.
		Element el = makeElement();
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("fred", el.getName(), "no tags, second action matches");

		el = makeElement();
		el.addTag("ref", "A1");
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("A1", el.getName(), "just a ref tag");

		el = makeElement();
		el.addTag("ref", "A1");
		el.addTag("name", "Main St");
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("Main St (A1)", el.getName(), "ref and name");
	}

	/**
	 * The apply action works on the members of relations.
	 */
	@Test
	public void testApplyAction() {
		List<Action> actions = readActionsFromString("{apply {" +
				"add route=bike;" +
				"set foo=bar; }" +
				"}\n");

		Relation rel = makeRelation();
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(rel, TypeResult.NULL_RESULT);

		assertNull(rel.getTag("route"), "Tag not set on relation");

		// Will be set on all members as there is no role filter.
		List<Map.Entry<String,Element>> elements = rel.getElements();
		Element el1 = elements.get(0).getValue();
		assertEquals("bike", el1.getTag("route"), "route tag added to first");
		assertEquals("bar", el1.getTag("foo"), "foo tag set to first");

		Element el2 = elements.get(1).getValue();
		assertEquals("bike", el2.getTag("route"), "route tag added to second");
		assertEquals("bar", el2.getTag("foo"), "foo tag set to second");
	}

	/**
	 * You can have a role filter, so that the actions are only applied
	 * to members with the given role.
	 */
	@Test
	public void testApplyWithRole() {
		List<Action> actions = readActionsFromString("{apply role=bar {" +
				"add route=bike;" +
				"set foo=bar; }}");

		Relation rel = makeRelation();
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(rel, TypeResult.NULL_RESULT);

		List<Map.Entry<String,Element>> elements = rel.getElements();
		Element el1 = elements.get(0).getValue();
		assertEquals("bike", el1.getTag("route"), "route tag added to first");
		assertEquals("bar", el1.getTag("foo"), "foo tag set to first");

		// Wrong role, so not applied.
		Element el2 = elements.get(1).getValue();
		assertNull(el2.getTag("route"), "route tag not added to second element (role=foo)");
		assertNull(el2.getTag("foo"), "foo tag not set in second element (role=foo)");
	}

	/**
	 * When an apply statement runs, then substitutions on the value use
	 * the tags of the relation and not of the sub element.
	 */
	@Test
	public void testApplyWithSubst() {
		List<Action> actions = readActionsFromString("{apply {" +
				"add route='${route_no}';" +
				"}}");

		Relation rel = makeRelation();
		rel.addTag("route_no", "66");
		Element el1 = rel.getElements().get(0).getValue();
		el1.addTag("route_no", "42");

		Rule rule = new ActionRule(null, actions);
		rule.resolveType(rel, TypeResult.NULL_RESULT);
		assertEquals("66", el1.getTag("route"), "route_no taken from relation tags");
	}

	@Test
	public void testEmptyActionList() {
		List<Action> actions = readActionsFromString("{}");
		assertEquals(0, actions.size(), "no actions found");
	}

	@Test
	public void testAlternatives() {
		List<Action> actions = readActionsFromString(
				"{set fred = '${park}' | 'default value'}");

		Element el = makeElement();
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("no", el.getTag("fred"), "first alternative");
	}

	@Test
	public void testSecondAlternative() {
		List<Action> actions = readActionsFromString(
				"{set fred = '${notset}' | 'default value'}");

		Element el = makeElement();
		el.addTag("fred", "origvalue");
		Rule rule = new ActionRule(null, actions);
		rule.resolveType(el, TypeResult.NULL_RESULT);
		assertEquals("default value", el.getTag("fred"), "second alternative");
	}

	@Test
	public void testMultipleNoSeparators() {
		List<Action> actions = readActionsFromString("{" +
				"set park='${notset}' | yes " +
				"add fred=other " +
				"set pooh=bear}");

		assertEquals(3, actions.size(), "number of actions");

		Element el = stdElementRun(actions);

		assertEquals("yes", el.getTag("park"), "park set to yes");
		assertEquals("other", el.getTag("fred"), "fred set");
		assertEquals("bear", el.getTag("pooh"), "pooh set");
	}

	@Test
	public void testErrorShortSet() {
		assertThrows(SyntaxException.class, () -> readActionsFromString("{set park= }"));
	}

	@Test
	public void testMangledSet() {
		assertThrows(SyntaxException.class, () -> readActionsFromString("{set park=yes some other junk }"));
	}

	@Test
	public void testErrorMangledList() {
		assertThrows(SyntaxException.class, () -> readActionsFromString("{set park='${notset}' | }"));
	}

	@Test
	public void testErrorExtraQuotedWord() {
		SyntaxException e = assertThrows(SyntaxException.class, () -> readActionsFromString("{set park=yes 'some' other junk }"));
		assertThat(e.getMessage()).contains("quoted word found where command expected");
	}

	private Element stdElementRun(List<Action> actions) {
		Rule rule = new ActionRule(null, actions);
		Element el = makeElement();
		rule.resolveType(el, TypeResult.NULL_RESULT);
		return el;
	}

	/**
	 * Make a standard element for the tests.
	 */
	private Element makeElement() {
		Element el = new Way(0);
		el.addTag("park", "no");
		el.addTag("test", "1");
		return el;
	}

	private Relation makeRelation() {
		Relation rel = new GeneralRelation(23);
		rel.addElement("bar", makeElement());
		rel.addElement("foo", makeElement());
		return rel;
	}
	/**
	 * Read a action list from a string.
	 */
	private List<Action> readActionsFromString(String in) {
		Reader sr = new StringReader(in);
		TokenScanner ts = new TokenScanner("string", sr);
		ActionReader ar = new ActionReader(ts);
		return ar.readActions().getList();
	}
}
