/*
 * Copyright (C) 2012.
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
package uk.me.parabola.imgfmt.app.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import uk.me.parabola.imgfmt.app.BitWriter;

/**
 * @author Steve Ratcliffe
 */
public class VarBitWriterTest {

	private final BitWriter bw = new BitWriter();

	@Test
	public void testPositive() {
		VarBitWriter vbw = new VarBitWriter(bw, 3);

		// should be able to write numbers up to 7
		vbw.write(7);

		byte b = bw.getBytes()[0];
		assertEquals(b, 7);
	}

	@Test
	public void testPositiveWithWidth() {
		VarBitWriter vbw = new VarBitWriter(bw, 3);
		vbw.bitWidth = 1;

		// should be able to write numbers up to 15
		vbw.write(15);

		byte b = bw.getBytes()[0];
		assertEquals(b, 15);
	}

	@Test
	public void testPositiveWithWidthFail() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);
			vbw.bitWidth = 1;

			// should be able to write numbers up to 15
			vbw.write(16);
		});
	}

	@Test
	public void testPositiveFail() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);

			// should be able to write numbers up to 7
			vbw.write(8);
		});
	}

	@Test
	public void testNegative() {
		VarBitWriter vbw = new VarBitWriter(bw, 3);
		vbw.negative = true;

		// write up to -7
		vbw.write(-7);
		byte b = bw.getBytes()[0];
		assertEquals(b, 7);
	}

	@Test
	public void testNegativeWithPositive() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);
			vbw.negative = true;

			// positive numbers are invalid
			vbw.write(7);
		});
	}

	@Test
	public void testNegativeTooBig() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);
			vbw.negative = true;

			// number too large
			vbw.write(8);
		});
	}

	@Test
	public void testSignedPositive() {
		VarBitWriter vbw = new VarBitWriter(bw, 3);
		vbw.signed = true;

		// up to 7
		vbw.write(7);
		byte b = bw.getBytes()[0];
		assertEquals(b, 7);
	}

	@Test
	public void testSignedNegative() {
		VarBitWriter vbw = new VarBitWriter(bw, 3);
		vbw.signed = true;

		// up to -8
		vbw.write(-8);
		byte b = bw.getBytes()[0];
		assertEquals(b, 0x8);
	}

	@Test
	public void testSignedPositiveTooBig() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);
			vbw.signed = true;

			// up to 7, 8 too big
			vbw.write(8);
		});
	}

	@Test
	public void testSignedNegativeTooBig() {
		assertThrows(Abandon.class, () -> {
			VarBitWriter vbw = new VarBitWriter(bw, 3);
			vbw.signed = true;

			// up to -8, -9 is too big
			vbw.write(-9);
		});
	}
}
