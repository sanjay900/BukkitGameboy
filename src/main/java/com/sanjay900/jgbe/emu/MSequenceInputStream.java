package com.sanjay900.jgbe.emu;


import java.io.*;

class MSequenceInputStream extends InputStream {
	private InputStream is1;
	private InputStream is2;

	public MSequenceInputStream(InputStream is1_, InputStream is2_) throws IOException
	{
		is1 = is1_;
		is2 = is2_;
	}

	public int read() throws IOException
	{
		do {
			if (is1 == null) return -1;
			int ret = is1.read();
			if (ret != -1) return ret;
			is1 = is2;
			is2 = null;
		} while (true);
	}

	public void close() throws IOException
	{
		is1 = null;
		is2 = null;
	}

	protected void finalize() throws Throwable
	{
		close();
	}
};
