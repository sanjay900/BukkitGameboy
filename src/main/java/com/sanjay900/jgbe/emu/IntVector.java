package com.sanjay900.jgbe.emu;

import java.io.*;


public class IntVector {
	private int length;
	private int[] data;

	private void resize(int newsize) {
		int[] old = data;
		data = new int[newsize];
		System.arraycopy(old, 0, data, 0, length);
	}

	public IntVector() {
		clear();
	}

	public void ensureCapacity(int minCapacity) {
		if (data.length < minCapacity)
			resize(minCapacity);
	}
	
	public void add(int index, int element) {
		ensureCapacity();
		System.arraycopy(data, index, data, index+1, length-index);
		data[index] = element;
		++length;
	}

	private void ensureCapacity() {
		if (data.length == length) resize(length*2);
	}
	
	public boolean add(int element) {
		ensureCapacity();
		data[length++] = element;
		return true;
	}

	public void clear() {
		length = 0;
		data = new int[1024];
	}
	
	public int get(int index) {
		if (!(index < length)) throw new Error("Assertion failed: " + "index < length");
		return data[index];
	}

	public boolean isEmpty() {
		return length == 0;
	}
	
	int remove(int index) {
		int res = data[index];
		for (int i = index+1; i < length; ++i)
			data[i-1] = data[i];
		--length;
		return res;
	}
	int set(int index, int element) {
		int res = data[index];
		data[index] = element;
		return res;
	}


	public int size() {
		return length;
	};
	void trimToSize() {
		resize(length);
	}

	protected void stateSaveLoad(boolean save, int version, DataOutputStream dostream, DataInputStream distream) throws IOException {
		{ if ((save)) dostream.writeInt((int)length); else length = distream.readInt(); };
		if ((!save)) data = new int[Math.max(1, length*2)];
		{ for (int sl_i = 0; sl_i < (length); ++sl_i) { if ((save)) dostream.writeInt((int)data[sl_i]); else data[sl_i] = distream.readInt(); }; };
	}

	void setSize(int len) {
		length = len;
	}
}
