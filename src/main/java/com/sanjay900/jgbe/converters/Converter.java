package com.sanjay900.jgbe.converters;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bukkit.Bukkit;

import com.google.common.base.CharMatcher;
import com.sanjay900.jgbe.bukkit.GameboyPlugin;
import com.sanjay900.jgbe.emu.CPU;

public abstract class Converter {
	GameboyPlugin plugin = GameboyPlugin.getInstance();
	public Converter() {
	}
	public void writtenMemory(int address) {
		if (plugin.isEnabled())
		Bukkit.getScheduler().runTaskLater(plugin, () ->writeMemory(address),5l);
	}
	abstract void writeMemory(int address);
	public static String BCDtoString(int bcd) {
		StringBuffer sb = new StringBuffer();
		
		byte high = (byte) (bcd & 0xf0);
		high >>>= (byte) 4;	
		high = (byte) (high & 0x0f);
		byte low = (byte) (bcd & 0x0f);
		
		sb.append(high);
		sb.append(low);
		
		return sb.toString();
	}
	
	public static String BCDtoString(int[] bcd) {
 
	StringBuffer sb = new StringBuffer();
 
	for (int i = 0; i < bcd.length; i++) {
		sb.append(BCDtoString(bcd[i]));
	}
	if (CharMatcher.is('0').trimLeadingFrom(sb.toString()).isEmpty()) {
		return "0";
	}
	return CharMatcher.is('0').trimLeadingFrom(sb.toString());
	}
	//Get the amount of bits set in a series of bytes
	public int getBitsSet(int...bs) {
		int result = 0;
		for (int b : bs) {
			for (int i=0; i<Byte.SIZE; i++) result += (b >> i & 0x1) != 0x0?1:0;
		}
		return result;
	}

	public short getShort(int b, int a) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte) a,(byte) b});
		return bb.getShort();
	}
	public int[] getWram(int start, int end) {
		int[] mm = plugin.cpu.wMemMap[start>>12];
		if (mm!=null) {
			return Arrays.copyOfRange(mm, start&0x0FFF, 1+end&0x0FFF);
		}
		return Arrays.copyOfRange(plugin.cpu.WRAM[plugin.cpu.CurrentWRAMBank], start-0xd000, 1+end-0xd000);
	}
	
}
