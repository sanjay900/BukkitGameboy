package com.sanjay900.jgbe.emu.tables;
/*
 *  IMPORTANT: THIS FILE IS AUTOGENERATED
 *
 *  Any modifications to this file will be lost when regenerating it.
 *  Modify the corresponding .jpp file instead, and regenerate this file.
 */

import com.sanjay900.jgbe.emu.CPU;

public class ShTablesRL {

	public final static short val[][] = new short[2][];
	public final static short flag[][] = new short[2][];

	static {
		val[0] = ShTablesSLA.val[0];
		flag[0] = ShTablesSLA.flag[0];
		val[1] = new short[256];
		flag[1] = new short[256];
		for (short i = 0; i < 256; ++i) {
			val[1][i] = (short)(((i << 1)&0xff) + 0x01);
			flag[1][i] = (short)((val[1][i] == 0 ? CPU.ZF_Mask : 0) | ((i&0x80) != 0? CPU.CF_Mask : 0));
		}
	}

};
