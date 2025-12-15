package uk.me.parabola.mkgmap.osmstyle.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import uk.me.parabola.mkgmap.reader.osm.Element;
import uk.me.parabola.mkgmap.reader.osm.Way;
import uk.me.parabola.mkgmap.scan.SyntaxException;

/**
 * @author Maxim Duester
 *
 */
public class NotContainedFilterTest {

	@Test
	public void testNoArg() {
		assertThrows(SyntaxException.class, () -> {
			NotContainedFilter filter = new NotContainedFilter("");
			filter.doFilter("x", null);
		});
	}

	@Test
	public void testOneArg() {
		assertThrows(SyntaxException.class, () -> {
			NotContainedFilter filter = new NotContainedFilter(";");
			filter.doFilter("x", null);
		});
	}

	@Test
	public void test2ndArgMissing() {
		assertThrows(SyntaxException.class, () -> {
			NotContainedFilter filter = new NotContainedFilter(":");
			filter.doFilter("x", null);
		});
	}

	@Test
	public void test2ndArgNotContained() {
		NotContainedFilter filter = new NotContainedFilter(";:ref");
		Element el = stdElement();
		String s = filter.doFilter("aa", el);
		assertEquals(s, "aa");
	}

	@Test
	public void test2ndArgContained() {
		NotContainedFilter filter = new NotContainedFilter(":ref");
		Element el = stdElement();
		String s = filter.doFilter("x", el);
		assertNull(s);
	}

	@Test
	public void testNonDefaultDelimiterNotContained() {
		NotContainedFilter filter = new NotContainedFilter("#:ref");
		Element el = stdElement();
		String s = filter.doFilter("x", el);
		assertEquals(s, "x");
	}
	
	@Test
	public void testNonDefaultDelimiterContained() {
		NotContainedFilter filter = new NotContainedFilter("#:test");
		Element el = stdElement();
		el.addTag("test", "Aa#Bb#Cc#Dd");
		String s = filter.doFilter("Cc", el);
		assertNull(s);
	}
	
	@Test
	public void testMissingTag(){
		NotContainedFilter filter=new NotContainedFilter(":sometag");
		Element el = stdElement();
		String s=filter.doFilter("x", el);
		assertEquals(s, "x");
	}

	private Element stdElement() {
		Element el1 = new Way(1);
		el1.addTag("ref", "x;y;z");
		return el1;
	}

}
