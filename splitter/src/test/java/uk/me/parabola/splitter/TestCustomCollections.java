/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package uk.me.parabola.splitter;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 
 */
public class TestCustomCollections {

	//@Test(expectedExceptions = IllegalArgumentException.class)
	//public void testInit() {
	//	new IntObjMap<String>(123, 0.5f);
	//}

	@Test
	public void testLongShortMap() {
		testMap(new SparseLong2ShortMapInline(), 0L);
		testMap(new SparseLong2ShortMapInline(), -10000L);
		testMap(new SparseLong2ShortMapInline(), 1L << 35);
		testMap(new SparseLong2ShortMapInline(), -1L << 35);
		testMap(new SparseLong2ShortMapHuge(), 0L);
		testMap(new SparseLong2ShortMapHuge(), -10000L);
		testMap(new SparseLong2ShortMapHuge(), 1L << 35);
		testMap(new SparseLong2ShortMapHuge(), -1L << 35);
	}

	private void testMap(SparseLong2ShortMapFunction map, long idOffset) {
		map.defaultReturnValue((short) Short.MIN_VALUE);

		for (short i = 1; i < 1000; i++) {
			int j = map.put(idOffset + i, i);
			Assertions.assertEquals(j, Short.MIN_VALUE);
			Assertions.assertEquals(map.size(), i);
		}

		for (short i = 1; i < 1000; i++) {
			boolean b = map.containsKey(idOffset + i);
			Assertions.assertEquals(b, true);
		}


		for (short i = 1; i < 1000; i++) {
			Assertions.assertEquals(map.get(idOffset + i), i);
		}

		// random read access 
		for (short i = 1; i < 1000; i++) {
			short key = (short) Math.max(1, (Math.random() * 1000));
			Assertions.assertEquals(map.get(idOffset + key), key);
		}

		for (short i = 1000; i < 2000; i++) {
			Assertions.assertEquals(map.get(idOffset + i), Short.MIN_VALUE);
		}
		for (short i = 1000; i < 2000; i++) {
			boolean b = map.containsKey(idOffset + i);
			Assertions.assertEquals(b, false);
		}
		for (short i = 1000; i < 1200; i++) {
			short j = map.put(idOffset + i, (short) 333);
			Assertions.assertEquals(j, Short.MIN_VALUE);
			Assertions.assertEquals(map.size(), i);
		}
		// random read access 2 
		for (int i = 1; i < 1000; i++) {
			int key = 1000 + (short) (Math.random() * 200);
			Assertions.assertEquals(map.get(idOffset + key), 333);
		}


		for (short i = -2000; i < -1000; i++) {
			Assertions.assertEquals(map.get(idOffset + i), Short.MIN_VALUE);
		}
		for (short i = -2000; i < -1000; i++) {
			boolean b = map.containsKey(idOffset + i);
			Assertions.assertEquals(b, false);
		}
		long mapSize = map.size();
		// seq. update existing records 
		for (int i = 1; i < 1000; i++) {
			short j = map.put(idOffset + i, (short) (i+333));
			Assertions.assertEquals(j, i);
			Assertions.assertEquals(map.size(), mapSize);
		}
		// random read access 3, update existing entries 
		for (int i = 1; i < 1000; i++) {
			short j = map.put(idOffset + i, (short) (i+555));
			Assertions.assertEquals(true, j == i+333 | j == i+555);
			Assertions.assertEquals(map.size(), mapSize);
		}
				
		Assertions.assertEquals(map.get(idOffset + 123456), Short.MIN_VALUE);
		map.put(idOffset + 123456, (short) 999);
		Assertions.assertEquals(map.get(idOffset + 123456), 999);
		map.put(idOffset + 123456, (short) 888);
		Assertions.assertEquals(map.get(idOffset + 123456), 888);

		Assertions.assertEquals(map.get(idOffset - 123456), Short.MIN_VALUE);
		map.put(idOffset - 123456, (short) 999);
		Assertions.assertEquals(map.get(idOffset - 123456), 999);
		map.put(idOffset - 123456, (short) 888);
		Assertions.assertEquals(map.get(idOffset - 123456), 888);
		map.put(idOffset + 3008, (short) 888);
		map.put(idOffset + 3009, (short) 888);
		map.put(idOffset + 3010, (short) 876);
		map.put(idOffset + 3011, (short) 876);
		map.put(idOffset + 3012, (short) 678);
		map.put(idOffset + 3013, (short) 678);
		map.put(idOffset + 3014, (short) 678);
		map.put(idOffset + 3015, (short) 678);
		map.put(idOffset + 3016, (short) 678);
		map.put(idOffset + 3017, (short) 678);
		map.put(idOffset + 4000, (short) 888);
		map.put(idOffset + 4001, (short) 888);
		map.put(idOffset + 4002, (short) 876);
		map.put(idOffset + 4003, (short) 876);
		// update the first one
		map.put(idOffset + 3008, (short) 889);
		// update the 2nd one
		map.put(idOffset + 4000, (short) 889);
		// add a very different key
		map.put(idOffset + 5000, (short) 889);
		map.put(idOffset + 5001, (short) 222);
		Assertions.assertEquals(map.get(idOffset + 3008), 889);
		Assertions.assertEquals(map.get(idOffset + 3009), 888);
		Assertions.assertEquals(map.get(idOffset + 3010), 876);
		Assertions.assertEquals(map.get(idOffset + 3011), 876);
		Assertions.assertEquals(map.get(idOffset + 3012), 678);
		Assertions.assertEquals(map.get(idOffset + 3013), 678);
		Assertions.assertEquals(map.get(idOffset + 3014), 678);
		Assertions.assertEquals(map.get(idOffset + 4000), 889);
		Assertions.assertEquals(map.get(idOffset + 4001), 888);
		Assertions.assertEquals(map.get(idOffset + 4002), 876);
		Assertions.assertEquals(map.get(idOffset + 4003), 876);
		Assertions.assertEquals(map.get(idOffset + 5000), 889);
		Assertions.assertEquals(map.get(idOffset + 5001), 222);
	}

	@Test
	public void testLong2IntMap() {
		testMap(new Long2IntClosedMap("test", 10000, -1));
	}

	private void testMap(Long2IntClosedMapFunction map) {
		int val;
		for (int i = 1; i < 1000; i++) {
			int j = map.add((long)i*10, i);
			Assertions.assertEquals(j, i-1);
			Assertions.assertEquals(map.size(), i);
		}

		for (int i = 1; i < 1000; i++) {
			int pos = map.getKeyPos(i*10);
			Assertions.assertEquals(i, pos+1);
		}

		for (int i = 1; i < 1000; i++) {
			val = map.getRandom(i*10);
			Assertions.assertEquals(i, val);
		}

		try {
			map.switchToSeqAccess(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try{
			val = map.getRandom(5);
		} catch (IllegalArgumentException e){
			Assertions.assertEquals(e.getMessage(), "random access on sequential-only map requested");
		}
		val = map.getSeq(5);
		Assertions.assertEquals(val,-1);
		val = map.getSeq(10);
		Assertions.assertEquals(val,1);
		val = map.getSeq(19);
		Assertions.assertEquals(val,-1);
		val = map.getSeq(30);
		Assertions.assertEquals(val,3);

		map.finish();
	}


}
