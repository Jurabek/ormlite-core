package com.j256.ormlite.stmt.query;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.j256.ormlite.stmt.SelectArg;

public class InTest {

	@Test(expected = IllegalArgumentException.class)
	public void testAppendValueNull() {
		List<Object> objList = new ArrayList<Object>();
		objList.add(null);
		In in = new In("foo", true, objList);
		in.appendValue(null, new StringBuilder(), null);
	}

	@Test
	public void testAppendValue() {
		List<Object> objList = new ArrayList<Object>();
		Random random = new Random();
		int numArgs = 100;
		for (int i = 0; i < numArgs; i++) {
			objList.add((Integer) random.nextInt());
		}
		In in = new In("foo", true, objList);
		StringBuilder sb = new StringBuilder();
		assertEquals(sb, in.appendValue(null, sb, new ArrayList<SelectArg>()));
		String[] args = sb.toString().split(",");
		assertEquals("(" + objList.get(0) + " ", args[0]);
		for (int i = 1; i < numArgs - 1; i++) {
			assertEquals(objList.get(i) + " ", args[i]);
		}
		assertEquals(objList.get(numArgs - 1) + " ) ", args[numArgs - 1]);
	}
}
