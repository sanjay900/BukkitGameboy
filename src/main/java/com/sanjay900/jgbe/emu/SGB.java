package com.sanjay900.jgbe.emu;

import java.util.BitSet;
import java.util.concurrent.Callable;

import com.sanjay900.jgbe.bukkit.GameboyPlugin;


public class SGB {
	GameboyPlugin plugin = GameboyPlugin.getInstance();
	BitSet sgbbits = new BitSet(129);
	int[] sgbPacket = new int[16];
	int sgbPacketLength = 0; // Number of packets to be transferred this command
	int sgbPacketsTransferred=0; // Number of packets which have been transferred so far
	int sgbPacketBit = -1; // Next bit # to be sent in the packet. -1 if no packet is being transferred.
	int sgbCommand;
	int sgbNumControllers=1;
	int sgbSelectedController=0; // Which controller is being observed
	int sgbButtonsChecked=0;
	public void handleP1(int val) {
		if ((val&0x30) == 0) {
			System.out.println("TEST");
			// Start packet transfer
			sgbPacketBit = 0;
			plugin.cpu.IOP[0x00] = 0xcf;
			return;
		}
		if (sgbPacketBit != -1) {
			int oldVal = plugin.cpu.IOP[0x00];
			plugin.cpu.IOP[0x00] = val;

			int shift = sgbPacketBit%8;
			int bytet = (sgbPacketBit/8)%16;
			if (shift == 0)
				sgbPacket[bytet] = 0;

			int bit;
			if ((oldVal & 0x30) == 0 && (val & 0x30) != 0x30) { // A bit of speculation here. Fixes castlevania.
				sgbPacketBit = -1;
				return;
			}
			if ((val & 0x10)==0)
				bit = 0;
			else if ((val & 0x20)==0)
				bit = 1;
			else
				return;

			sgbPacket[bytet] |= bit<<shift;
			sgbPacketBit++;
			if (sgbPacketBit == 128) {
				if (sgbPacketsTransferred == 0) {
					sgbCommand = sgbPacket[0]/8;
					sgbPacketLength = sgbPacket[0]&7;
					//printLog("CMD %x\n", sgbCommand);
				}
				if (sgbCommands[sgbCommand] != null)
					try {
						System.out.println(sgbCommands[sgbCommand].call());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				sgbPacketBit = -1;
				sgbPacketsTransferred++;
				if (sgbPacketsTransferred == sgbPacketLength) {
					sgbPacketLength = 0;
					sgbPacketsTransferred = 0;
				}
			}
		}
		else {
			if ((val&0x30) == 0x30) {
				if (sgbButtonsChecked == 3) {
					sgbSelectedController++;
					if (sgbSelectedController >= sgbNumControllers)
						sgbSelectedController = 0;
					sgbButtonsChecked = 0;
				}
				plugin.cpu.IOP[0x00] = 0xff - sgbSelectedController;
			}
			else {
				plugin.cpu.IOP[0x00] = val|0xcf;
				if ((val&0x30) == 0x10)
					sgbButtonsChecked |= 1;
				else if ((val&0x30) == 0x20)
					sgbButtonsChecked |= 2;
			}
		}
	}
	@SuppressWarnings("unchecked")
	Callable<String>[] sgbCommands = new Callable[]{this::sgbPalXX,this::sgbPalXX,this::sgbPalXX,this::sgbPalXX,this::sgbAttrBlock,this::sgbAttrLin,this::sgbAttrDiv,this::sgbAttrChr,
    null,null,this::sgbPalSet,this::sgbPalTrn,null,null,null,this::sgbDataSnd,
    null,this::sgbMltReq,null,this::sgbChrTrn,this::sgbPctTrn,this::sgbAttrTrn,this::sgbAttrSet,this::sgbMask,
    null,null,null,null,null,null,null,null};
	private String sgbDoVramTransfer() {
		return "Doing Vram Transfer!";
	}
	private String setBackdrop() {
		return "Setting Backdrop!";
	}
	private String sgbPalXX() {
		return "Command sgbPalXX recieved!";
	}
	private String sgbAttrBlock() {
		return "Command sgbAttrBlock recieved!";
	}
	private String sgbAttrLin() {
		return "Command sgbAttrLin recieved!";
	}
	private String sgbAttrDiv() {
		return "Command sgbAttrDiv recieved!";
	}
	private String sgbAttrChr() {
		return "Command sgbAttrChr recieved!";
	}
	private String sgbPalSet() {
		return "Command sgbPalSet recieved!";
	}
	private String sgbPalTrn() {
		return "Command sgbPalTrn recieved!";
	}
	private String sgbDataSnd() {
		return "Command sgbDataSnd recieved!";
	}
	private String sgbMltReq() {
		return "Command sgbMltReq recieved!";
	}
	private String sgbChrTrn() {
		return "Command sgbChrTrn recieved!";
	}
	private String sgbPctTrn() {
		return "Command sgbPctTrn recieved!";
	}
	private String sgbAttrTrn() {
		return "Command sgbAttrTrn recieved!";
	}
	private String sgbAttrSet() {
		return "Command sgbAttrSet recieved!";
	}
	private String sgbMask() {
		return "Command sgbMask recieved!";
	}
	
}
