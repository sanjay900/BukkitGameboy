package com.sanjay900.jgbe.emu;

import java.io.*;

public class Bios {

	public Bios(String fname, int[] location) {
		try {
			loadFromFile(fname, location);
		}
		catch (Throwable ioe) {
			System.out.println("Using BIOS stub");
			location[0]=(0xc3);
			location[1]=(0x00);
			location[2]=(0x01);
		}
	}

	public void loadFromFile(String fname, int[] location) throws Throwable {
		long fsize = 256;
		DataInputStream distream = FHandler.getDataInputStream(fname);
		for (int i = 0; i < fsize; ++i) {
			location[i] = (distream.readUnsignedByte());
		}
	}
}
