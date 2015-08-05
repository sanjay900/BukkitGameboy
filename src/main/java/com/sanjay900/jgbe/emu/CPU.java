package com.sanjay900.jgbe.emu;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public final class CPU {
	public static CPU singleton = null;


	public static final int ZF_Shift = 7;
	public static final int NF_Shift = ZF_Shift - 1;
	public static final int HC_Shift = NF_Shift - 1;
	public static final int CF_Shift = HC_Shift - 1;
	public static final int ZF_Mask = 1 << ZF_Shift;
	public static final int NF_Mask = 1 << NF_Shift;
	public static final int HC_Mask = 1 << HC_Shift;
	public static final int CF_Mask = 1 << CF_Shift;

	public static final long MAX_CYCLE_COUNT = 0x7FFFFFFFFFFFFFFFL;

	public static long TotalInstrCount = 0;
	public static long TotalCycleCount = 0;

	public static long NextEventCycleCount = 0;
	public static long VCRenderEventCycleCount = 0;
	public static long lastVCRenderCycleCount = 0;
	public static long KeyBounceEventCycleCount = 0;
	public static long TIMAEventCycleCount = 0;
	public static int cyclesPerTIMA = 0;
	public static int keyBounce = 0;

	public static boolean KeyBounceEventPending = false;
	public static boolean TIMAEventPending = false;

	public static int A = 0;
	public static int B = 1;
	public static int C = 2;
	public static int D = 3;
	public static int E = 4;
	public static int F = 5;
	public static int H = 6;
	public static int L = 7;

	public static int[] IOP = new int[0x80];
	public static int[] HRAM = new int[0x7f];
	public static int[][] WRAM = new int[0x08][0x1000];
	public static int[] FWRAM = WRAM[0];
	public static int CurrentWRAMBank=1;

	public static boolean doublespeed = false;
	public static boolean speedswitch = false;


	public static final int CYCLES_PER_DIV = 256;
	public static long divReset = 0;

	public static int globalPC = 0;
	public static int localPC = 0;

	public static int[] smallcruise = new int[2];
	public static int[] decoderMemory = null;
	public static int decoderMaxCruise = 0;

	public static int getPC() {
		return globalPC + localPC;
	}

	public static void setPC(int newPC) {
		newPC &= 0xffff;
		if (rMemMap[newPC >> 12] != null && (newPC&0x0fff)<=(0x0fff-2)) {

			decoderMemory = rMemMap[newPC >> 12];
			globalPC = newPC&0xf000;
			localPC = newPC&0x0fff;
			decoderMaxCruise = 0x0ffe;
		} else if (0xFF80 <= newPC && newPC <= 0xFFFE-2) {

			localPC = newPC - 0xFF80;
			globalPC = newPC - localPC;
			decoderMemory = HRAM;
			decoderMaxCruise = 0x7d;
		} else {

			smallcruise[0] = (read(newPC));

			boolean b = BIOS_enabled;
			smallcruise[1] = (read((newPC+1)&0xffff));
			if(b) {
				rMemMap[0x0] = null;
				BIOS_enabled = b;
			}
			decoderMemory = smallcruise;
			decoderMaxCruise = 0;
			globalPC = newPC;
			localPC = 0;
		}
	}

	public static void pushPC() {
		int pc = globalPC + localPC;
		if (SP >= 0xC002 && SP <= 0xD000) {
			FWRAM[--SP - 0xC000] = (pc>>8);
			FWRAM[--SP - 0xC000] = (pc&0xff);
		} else slowPushPC();
	}

	public static void slowPushPC() {

		int pc = globalPC + localPC;
		--SP; SP &= 0xffff; write(SP, pc >> 8);
		--SP; SP &= 0xffff; write(SP, pc&0xff);
	}

	public static void popPC() {
		if (SP >= 0xC000 && SP <= 0xCFFE ) {
			setPC(
					(FWRAM[SP++ - 0xC000])
					+ ((FWRAM[SP++ - 0xC000])<<8)
					);
		} else slowPopPC();
	}

	public static void slowPopPC() {

		int pc = read(SP++); SP &= 0xffff;
		pc |= (read(SP++) << 8); SP &= 0xffff;
		setPC(pc);
	}


	public static int SP=0;
	public static int IE=0;
	public static boolean IME=true;
	public static boolean halted=false;
	public static boolean delayed_halt=false;
	public static boolean halt_fail_inc_pc=false;


	public static int KeyStatus=0;
	public static int GUIKeyStatus=0;
	public static int RemoteKeyStatus=0;
	public static boolean useRemoteKeys = false;

	public static boolean keyHistoryEnabled;
	public static IntVector keyHistory;
	public static int lastKeyChange;
	public static int playbackHistoryIndex = -1;

	static boolean BIOS_enabled = false;


	public static Cartridge cartridge;

	public static VideoController VC;
	public static AudioController AC;
	static int hblank_dma_state;

	public static int last_memory_access=-1;
	public static int last_memory_access_internal=-1;

	public CPU() {
		;
		if (singleton != null) {
			System.out.println("WARNING: trying to instantiate second instance of CPU singleton class");


		}
		singleton = this;

		;
		VC = new VideoController(this, 160, 144);
		;
		AC = new AudioController(this);
		;
		keyHistory = new IntVector();
		;
		;

	}

	public static void loadCartridge(Cartridge acartridge) {
		cartridge = acartridge;

		reset(false);



	}

	public static boolean canRun() {
		return (cartridge != null);
	}

	public static String first_save_string = new String();
	public static String last_save_string = new String();

	public static void calcCyclesPerTIMA() {


		if ((IOP[0x07]&4) != 0) {
			cyclesPerTIMA = 4*(4 << (2 * ((IOP[0x07]-1)&3)));
		} else {
			cyclesPerTIMA = 0;
		}
	}

	public static void calcTIMAEventCycleCount() {
		if ((IOP[0x07]&4) != 0) {
			TIMAEventCycleCount = TotalCycleCount + cyclesPerTIMA * (0x100 - IOP[0x05]);
			addEventCycleCount(TIMAEventCycleCount);
			TIMAEventPending = true;
		} else {
			TIMAEventCycleCount = MAX_CYCLE_COUNT;
			TIMAEventPending = false;
		}
	}

	static protected void stateSaveLoad(boolean save, int version, DataOutputStream dostream, DataInputStream distream) throws IOException {
		if ( ((0 == -1) || (0 <= version)) && ((9 == -1) || (version <= 9)) ) first_save_string = "unknown";
		if ( ((0 == -1) || (0 <= version)) && ((9 == -1) || (version <= 9)) ) last_save_string = "unknown";
		if ((save)) last_save_string = Version.str;
		if ( ((10 == -1) || (10 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeUTF(first_save_string); else first_save_string = distream.readUTF(); };
		if ( ((10 == -1) || (10 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeUTF(last_save_string); else last_save_string = distream.readUTF(); };

		if ( ((1 == -1) || (1 <= version)) && ((-1 == -1) || (version <= -1)) ) {
			{ if ((save)) dostream.writeByte((B)&0xff); else B = distream.readUnsignedByte(); };
			{ if ((save)) dostream.writeByte((C)&0xff); else C = distream.readUnsignedByte(); };
			{ if ((save)) dostream.writeByte((D)&0xff); else D = distream.readUnsignedByte(); };
			{ if ((save)) dostream.writeByte((E)&0xff); else E = distream.readUnsignedByte(); };
		}
		{ if ((save)) dostream.writeByte((H)&0xff); else H = distream.readUnsignedByte(); };
		{ if ((save)) dostream.writeByte((L)&0xff); else L = distream.readUnsignedByte(); };
		{ if ((save)) dostream.writeByte((F)&0xff); else F = distream.readUnsignedByte(); };
		{ if ((save)) dostream.writeByte((A)&0xff); else A = distream.readUnsignedByte(); };
		{ for (int sl_i = 0; sl_i < (0x80); ++sl_i) { if ((save)) dostream.writeByte((IOP[sl_i])&0xff); else IOP[sl_i] = distream.readUnsignedByte(); }; };
		{ for (int sl_i = 0; sl_i < (0x7f); ++sl_i) { if ((save)) dostream.writeByte(((HRAM[sl_i])&0xff)); else HRAM[sl_i] = (distream.readUnsignedByte()); }; };
		if ( ((0 == -1) || (0 <= version)) && ((0 == -1) || (version <= 0)) ) {
			boolean doskip = false;
			int skipsize = 0x10000 - 0x1000;
			{ for (int sl_i = 0; sl_i < (0x1000); ++sl_i) { if ((save)) dostream.writeByte(((WRAM[0][sl_i])&0xff)); else WRAM[0][sl_i] = (distream.readUnsignedByte()); }; };
			if ((!save)) {
				int trysize = 0x8000;
				byte[] buffer = new byte[trysize];
				int len = distream.read(buffer, 0, trysize);
				if (len == trysize) {
					doskip = true;
					for (int i = 0; i < trysize; ++i)
						if (buffer[i] != 0)
							doskip = false;
				}
				if (!doskip)
					distream = new DataInputStream(new MSequenceInputStream(new ByteArrayInputStream(buffer), distream));
				else
				{ int tempskip = 0; for (int sl_i = 0; sl_i < (skipsize-trysize); ++sl_i) { if ((save)) dostream.writeByte(((tempskip)&0xff)); else tempskip = (distream.readUnsignedByte()); }; };
			}

			for (int i = 1; i < 8; ++i) {
				{ for (int sl_i = 0; sl_i < (0x1000); ++sl_i) { if ((save)) dostream.writeByte(((WRAM[i][sl_i])&0xff)); else WRAM[i][sl_i] = (distream.readUnsignedByte()); }; };
				if (doskip) { int tempskip = 0; for (int sl_i = 0; sl_i < (skipsize); ++sl_i) { if ((save)) dostream.writeByte(((tempskip)&0xff)); else tempskip = (distream.readUnsignedByte()); }; };
			}
		}
		if ( ((1 == -1) || (1 <= version)) && ((-1 == -1) || (version <= -1)) ) { for (int sl_i = 0; sl_i < (0x08); ++sl_i) for (int sl_j = 0; sl_j < (0x1000); ++sl_j) { if ((save)) dostream.writeByte(((WRAM[sl_i][sl_j])&0xff)); else WRAM[sl_i][sl_j] = (distream.readUnsignedByte()); }; };
		{ if ((save)) dostream.writeInt((int)CurrentWRAMBank); else CurrentWRAMBank = distream.readInt(); };
		{ if ((save)) dostream.writeBoolean(doublespeed); else doublespeed = distream.readBoolean(); };
		{ if ((save)) dostream.writeBoolean(speedswitch); else speedswitch = distream.readBoolean(); };

		int DIVcntdwnFix = -1;
		if ( ((-1 == -1) || (-1 <= version)) && ((18 == -1) || (version <= 18)) ) { if ((save)) dostream.writeInt((int)DIVcntdwnFix); else DIVcntdwnFix = distream.readInt(); };
		if ( ((19 == -1) || (19 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(divReset); else divReset = distream.readLong(); };
		int TIMAcntdwnFix = -1;
		if ( ((-1 == -1) || (-1 <= version)) && ((19 == -1) || (version <= 19)) ) { if ((save)) dostream.writeInt((int)TIMAcntdwnFix); else TIMAcntdwnFix = distream.readInt(); };
		if ( ((20 == -1) || (20 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(TIMAEventCycleCount); else TIMAEventCycleCount = distream.readLong(); };

		if ( ((-1 == -1) || (-1 <= version)) && ((14 == -1) || (version <= 14)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeInt((int)tempskip); else tempskip = distream.readInt(); }; };
		if ( ((21 == -1) || (21 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(lastVCRenderCycleCount); else lastVCRenderCycleCount = distream.readLong(); };

		int pc = getPC();
		{ if ((save)) dostream.writeShort((pc)&0xffff); else pc = distream.readUnsignedShort(); };
		setPC(pc);

		{ if ((save)) dostream.writeShort((SP)&0xffff); else SP = distream.readUnsignedShort(); };
		{ if ((save)) dostream.writeByte((IE)&0xff); else IE = distream.readUnsignedByte(); };
		{ if ((save)) dostream.writeBoolean(IME); else IME = distream.readBoolean(); };
		{ if ((save)) dostream.writeBoolean(halted); else halted = distream.readBoolean(); };
		if ( ((16 == -1) || (16 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeBoolean(delayed_halt); else delayed_halt = distream.readBoolean(); };
		if ( ((16 == -1) || (16 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeBoolean(halt_fail_inc_pc); else halt_fail_inc_pc = distream.readBoolean(); };
		if ( ((0 == -1) || (0 <= version)) && ((2 == -1) || (version <= 2)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeByte((tempskip)&0xff); else tempskip = distream.readUnsignedByte(); }; };
		if ( ((0 == -1) || (0 <= version)) && ((2 == -1) || (version <= 2)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeByte((tempskip)&0xff); else tempskip = distream.readUnsignedByte(); }; };
		if ( ((11 == -1) || (11 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeByte((KeyStatus)&0xff); else KeyStatus = distream.readUnsignedByte(); };
		if ( ((12 == -1) || (12 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeInt((int)keyBounce); else keyBounce = distream.readInt(); };
		if ( ((22 == -1) || (22 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(KeyBounceEventCycleCount); else KeyBounceEventCycleCount = distream.readLong(); };
		int keyBounceWaitNextFix = 0;
		if ( ((12 == -1) || (12 <= version)) && ((21 == -1) || (version <= 21)) ) { if ((save)) dostream.writeInt((int)keyBounceWaitNextFix); else keyBounceWaitNextFix = distream.readInt(); };
		if ( ((-1 == -1) || (-1 <= version)) && ((18 == -1) || (version <= 18)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeInt((int)tempskip); else tempskip = distream.readInt(); }; };
		{ if ((save)) dostream.writeBoolean(BIOS_enabled); else BIOS_enabled = distream.readBoolean(); };

		(cartridge).stateSaveLoad(save, version, dostream, distream);;
		(VC).stateSaveLoad(save, version, dostream, distream);;
		if ( ((2 == -1) || (2 <= version)) && ((-1 == -1) || (version <= -1)) ) (AC).stateSaveLoad(save, version, dostream, distream);;

		if ( ((3 == -1) || (3 <= version)) && ((13 == -1) || (version <= 13)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeByte((tempskip)&0xff); else tempskip = distream.readUnsignedByte(); }; };

		if ( ((3 == -1) || (3 <= version)) && ((8 == -1) || (version <= 8)) ) { if ((save)) dostream.writeInt((int)TotalInstrCount); else TotalInstrCount = distream.readInt(); };
		if ( ((3 == -1) || (3 <= version)) && ((8 == -1) || (version <= 8)) ) { if ((save)) dostream.writeInt((int)TotalCycleCount); else TotalCycleCount = distream.readInt(); };
		if ( ((9 == -1) || (9 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(TotalInstrCount); else TotalInstrCount = distream.readLong(); };
		if ( ((9 == -1) || (9 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeLong(TotalCycleCount); else TotalCycleCount = distream.readLong(); };
		if ( ((4 == -1) || (4 <= version)) && ((16 == -1) || (version <= 16)) ) { int tempskip = 0; for (int sl_i = 0; sl_i < (1); ++sl_i) { if ((save)) dostream.writeByte((tempskip)&0xff); else tempskip = distream.readUnsignedByte(); }; };

		if ( ((7 == -1) || (7 <= version)) && ((-1 == -1) || (version <= -1)) ) { if ((save)) dostream.writeByte((hblank_dma_state)&0xff); else hblank_dma_state = distream.readUnsignedByte(); };

		if ( ((13 == -1) || (13 <= version)) && ((-1 == -1) || (version <= -1)) ) {
			if ((save) && playbackHistoryIndex != -1) {
				boolean t = true;
				{ if ((save)) dostream.writeBoolean(t); else t = distream.readBoolean(); };
				{ if ((save)) dostream.writeInt((int)lastKeyChange); else lastKeyChange = distream.readInt(); };
				int olen = keyHistory.size();
				keyHistory.setSize(playbackHistoryIndex);
				(keyHistory).stateSaveLoad(save, version, dostream, distream);;
				keyHistory.setSize(olen);
			} else {
				{ if ((save)) dostream.writeBoolean(keyHistoryEnabled); else keyHistoryEnabled = distream.readBoolean(); };
				if (keyHistoryEnabled) {
					{ if ((save)) dostream.writeInt((int)lastKeyChange); else lastKeyChange = distream.readInt(); };
					(keyHistory).stateSaveLoad(save, version, dostream, distream);;
				}
			}
			if ((!save)) playbackHistoryIndex = -1;
		} else
			keyHistoryEnabled = false;

		if ((!save)) {
			refreshMemMap();
			calcCyclesPerTIMA();
			NextEventCycleCount = 0;
			VCRenderEventCycleCount = 0;
			if ( ((-1 == -1) || (-1 <= version)) && ((2 == -1) || (version <= 2)) ) {
				TotalInstrCount = 0;
				TotalCycleCount = 0;
			}
			if ( ((-1 == -1) || (-1 <= version)) && ((18 == -1) || (version <= 18)) ) {

				divReset = (TotalCycleCount + CYCLES_PER_DIV - 1) - (IOP[0x04]*CYCLES_PER_DIV);
				divReset -= CYCLES_PER_DIV;
				divReset += DIVcntdwnFix;
			}
			if ( ((-1 == -1) || (-1 <= version)) && ((19 == -1) || (version <= 19)) ) {

				calcTIMAEventCycleCount();
				TIMAEventCycleCount -= cyclesPerTIMA;
				TIMAEventCycleCount += TIMAcntdwnFix;
				addEventCycleCount(TIMAEventCycleCount);
			}
			if ( ((-1 == -1) || (-1 <= version)) && ((20 == -1) || (version <= 20)) )
				lastVCRenderCycleCount = TotalCycleCount;
			if ( ((-1 == -1) || (-1 <= version)) && ((21 == -1) || (version <= 21)) )
				KeyBounceEventCycleCount = (keyBounce > 0) ? TotalCycleCount + keyBounceWaitNextFix * ((doublespeed)?10:20) : MAX_CYCLE_COUNT;
				if ( ((22 == -1) || (22 <= version)) && ((22 == -1) || (version <= 22)) ) {
					TIMAEventCycleCount = TotalCycleCount;
					KeyBounceEventCycleCount = TotalCycleCount;
				}
				TIMAEventPending = (TIMAEventCycleCount != MAX_CYCLE_COUNT);
				KeyBounceEventPending = (KeyBounceEventCycleCount != MAX_CYCLE_COUNT);
		}
		
		
	}

	public static void saveState(DataOutputStream dostream)
			throws IOException
	{
		int saveversion = (23);
		dostream.writeInt((0x4a374a53));
		dostream.writeInt(saveversion);

		OutputStream compress = null;
		int compressionmethod = 0;

		compressionmethod = 1;


		dostream.writeInt(compressionmethod);
		switch (compressionmethod) {

		case 0: break;


		case 1: {
			compress = new GZIPOutputStream(dostream);
			dostream = new DataOutputStream(compress);
		}; break;

		case 2: {
			compress = new LZOutputStream(dostream);
			dostream = new DataOutputStream(compress);
		}; break;

		default: if (!(false)) throw new Error("Assertion failed: " + "false");
		}
		stateSaveLoad(true, saveversion, dostream, null);

		dostream.flush();

		if (compress != null) compress.close();
	}

	public static void loadState(DataInputStream distream)
			throws IOException
	{
		int loadversion;
		int magix = distream.readInt();
		if (magix != (0x4a374a53)) {


			loadversion = 0;
			B = (magix >> 24) & 0xff;
			C = (magix >> 16) & 0xff;
			D = (magix >> 8) & 0xff;
			E = (magix >> 0) & 0xff;
		} else
			loadversion = distream.readInt();
		if (loadversion < (0))
			throw new IOException("save state too old");
		if (loadversion > ((23)))
			throw new IOException("save state too new");
		if (loadversion != (23))
			System.out.println("loading state with old version:"+loadversion);

		int compressionmethod = 0;
		if (loadversion >= 5)
			compressionmethod = distream.readInt();
		switch (compressionmethod) {

		case 0: break;


		case 1: distream = new DataInputStream(new GZIPInputStream(distream)); break;

		case 2: distream = new DataInputStream(new LZInputStream(distream)); break;

		default: throw new IOException("unknown compression method:"+compressionmethod);
		}
		stateSaveLoad(false, loadversion, null, distream);
	}

	public static int[][] rMemMap = new int[0x10][];
	public static int[][] wMemMap = new int[0x10][];

	public static boolean isCGB() {

		if(cartridge==null) return false;
		return (read(0x0143) == 0x80) || (read(0x0143) == 0xC0);
	}

	public static int hblank_dma() {
		if (!((VC.STAT&2)==0)) throw new Error("Assertion failed: " + "(VC.STAT&2)==0");
		if (hblank_dma_state < (1<<7)) return 0;

		int src = ((IOP[0x51]<<8)|IOP[0x52]) & 0xfff0;
		int dst = (((IOP[0x53]<<8)|IOP[0x54]) & 0x1ff0) | 0x8000;
		int len = 16;

		for (int i = 0; i < len; ++i)
			VC.write(dst++, read(src++));

		IOP[0x51] = src >> 8;
		IOP[0x52] = src & 0xF0;
		IOP[0x53] = 0x1F & (dst >> 8);
		IOP[0x54] = dst & 0xF0;
		--IOP[0x55];
		--hblank_dma_state;

		return 8;
	}

	public static void refreshMemMap() {
		if (BIOS_enabled)
			rMemMap[0x0] = null;
		else
			rMemMap[0x0] = cartridge.MM_ROM[0];

		rMemMap[0x1] = cartridge.MM_ROM[1];
		rMemMap[0x2] = cartridge.MM_ROM[2];
		rMemMap[0x3] = cartridge.MM_ROM[3];






		rMemMap[0x4] = cartridge.MM_ROM[(cartridge.CurrentROMBank<<2)|0];
		rMemMap[0x5] = cartridge.MM_ROM[(cartridge.CurrentROMBank<<2)|1];
		rMemMap[0x6] = cartridge.MM_ROM[(cartridge.CurrentROMBank<<2)|2];
		rMemMap[0x7] = cartridge.MM_ROM[(cartridge.CurrentROMBank<<2)|3];





		if(cartridge.MBC!=2) {
			rMemMap[0xA] = wMemMap[0xA] = cartridge.MM_RAM[(cartridge.CurrentRAMBank<<1)|0];
			rMemMap[0xB] = wMemMap[0xB] = cartridge.MM_RAM[(cartridge.CurrentRAMBank<<1)|1];
		}
		else {
			rMemMap[0xA] = wMemMap[0xA] = null;
			rMemMap[0xB] = wMemMap[0xB] = null;
		}


		rMemMap[0xC] = wMemMap[0xC] = WRAM[0];
		rMemMap[0xD] = wMemMap[0xD] = WRAM[CurrentWRAMBank];


		rMemMap[0xE] = wMemMap[0xE] = rMemMap[0xC];



		setPC(getPC());
	}

	public static int read(int index) {



		int mm[]=rMemMap[index>>12];
		if (mm!=null)
			return (mm[index&0x0FFF]);
		return read_slow(index);
	}

	public static int read_slow(int index) {

		int b;



		if (!(index>=0 && index <=0xffff)) throw new Error("Assertion failed: " + "index>=0 && index <=0xffff");

		if (index < 0x4000) {
			if (index < 0x100) {

				b = cartridge.BIOS_ROM[index];

			}
			else if (index == 0x100) {

				BIOS_enabled = false;

				refreshMemMap();
				VC.isCGB = isCGB();
				A = VC.isCGB?0x11:0x01;
				b = read(index);
			}
			else {
				b = cartridge.MM_ROM[0][index];

			}
		}
		else if(index < 0x8000) {
			b=cartridge.read(index);
		}
		else if(index < 0xa000) {
			b=VC.read(index);
		}
		else if(index < 0xc000) {
			b=cartridge.read(index);
		}
		else if(index < 0xd000) {
			b=(WRAM[0][index-0xc000]);
		}
		else if(index < 0xe000) {
			b=(WRAM[CurrentWRAMBank][index-0xd000]);
		}
		else if(index < 0xfe00) {
			b=read(index-0x2000);
		}
		else if(index < 0xfea0) {
			b=VC.read(index);
		}
		else if(index < 0xff00) {
			System.out.println("WARNING: CPU.read(): Read from unusable memory (0xfea0-0xfeff)");
			b=0;
		}
		else if((index>0xff0f) && (index<0xff40)) {
			b = AC.read(index);
		}
		else if((index>0xff3f) && (index<0xff70)) {
			b = VC.read(index);
		}
		else if(index < 0xff80) {
			switch(index) {
			case 0xff00:
				b=(IOP[index-0xff00]&0x30)|0xc0;
				if((b&(1<<4))==0) {
					b|=(KeyStatus&0x0f);
				}
				if((b&(1<<5))==0) {
					b|=(KeyStatus>>4);
				}
				b^=0x0f;
				break;
			case 0xff04:

				return (int)((((TotalCycleCount + CYCLES_PER_DIV - 1 - divReset) / CYCLES_PER_DIV))&0xff);
			case 0xff01:
			case 0xff02:
				b = IOP[index-0xff00];
				break;
			case 0xff05:
				if (cyclesPerTIMA != 0) {
					b = (int)(((TotalCycleCount - TIMAEventCycleCount + (cyclesPerTIMA * 0x100)) / cyclesPerTIMA)&0xff);
				} else {
					b = IOP[0x05];
				}
				break;
			case 0xff06:
			case 0xff07:
				b = IOP[index-0xff00];
				break;
			case 0xff0f:
				b = IOP[0x0f];
				break;


			case 0xff70:
				b = CurrentWRAMBank;
				break;

			case 0xff6c:
				System.out.printf("WARNING: CPU.read(): Read from *undocumented* IO port $%04x\n",index);
				b = IOP[index-0xff00] | 0xfe;
				break;
			case 0xff72:
			case 0xff73:
			case 0xff74:
				b = IOP[index-0xff00];
				break;
			case 0xff75:
				System.out.printf("WARNING: CPU.read(): Read from *undocumented* IO port $%04x\n",index);
				b = IOP[index-0xff00] | 0x8f;
				break;
			case 0xff76:
			case 0xff77:
				System.out.printf("WARNING: CPU.read(): Read from *undocumented* IO port $%04x\n",index);
				b = 0;
				break;
			default:
				System.out.printf("TODO: CPU.read(): Read from IO port $%04x\n",index);
				b=0xff;
				break;
			}
		}
		else if(index < 0xffff) {
			b = (HRAM[index-0xff80]);
		}
		else if(index < 0x10000) {
			b=IE;
		}
		else {
			System.out.println("ERROR: CPU.read(): Out of range memory access: $"+index);
			b=0;
		}
		return b;
	}


	public static void write_fast1(int value, int index) {
		if (swinggui.getInstance().gp != null && swinggui.getInstance().gp.conv != null) 
			swinggui.getInstance().gp.conv.writtenMemory(index);

		int mm[]=wMemMap[index>>12];
		if (mm!=null) {
			mm[index&0x0FFF] = (value);
			return;
		}
		write_slow(index, value);
	}

	public static void write(int index, int value) {
		if (swinggui.getInstance().gp != null && swinggui.getInstance().gp.conv != null) 
			swinggui.getInstance().gp.conv.writtenMemory(index);
		int mm[]=wMemMap[index>>12];
		if (mm!=null) {
			mm[index&0x0FFF] = (value);
			return;
		}
		write_slow(index, value);
	}

	public static void write_slow(int index, int value) {
		if (swinggui.getInstance().gp != null && swinggui.getInstance().gp.conv != null) 
			swinggui.getInstance().gp.conv.writtenMemory(index);
		if (!(wMemMap[index>>12] == null)) throw new Error("Assertion failed: " + "wMemMap[index>>12] == null");

		if (!(index>=0 && index <=0xffff)) throw new Error("Assertion failed: " + "index>=0 && index <=0xffff");

		if(index < 0x8000) {
			cartridge.write(index, value);

			refreshMemMap();
		}
		else if(index < 0xa000) {
			VC.write(index, value);
		}
		else if(index < 0xc000) {
			cartridge.write(index, value);
		}
		else if(index < 0xd000) {
			WRAM[0][index-0xc000]=(value);
		}
		else if(index < 0xe000) {
			WRAM[CurrentWRAMBank][index-0xd000]=(value);
		}
		else if(index < 0xfe00) {
			write(index-0x2000, value);
		}
		else if(index < 0xfea0) {
			VC.write(index, value);
		}
		else if(index < 0xff00) {



		}
		else if((index>0xff0f) && (index<0xff40)) {
			AC.write(index, value);
		}
		else if((index>0xff3f) && (index<0xff70)) {
			VC.write(index, value);
		}
		else if(index < 0xff80) {
			switch(index) {
			case 0xff00:
				IOP[index&0xff]=value;
				break;
			case 0xff01:
				IOP[0x01]=value;
				break;
			case 0xff02:
				IOP[0x02]=value;
				if(LinkCableStatus==0) {
					if ((value&(1<<7))!=0) {

						if ((value&(1<<0))!=0) {

							IOP[0x01] = 0xFF;
							IOP[0x02] &= ~(1<<7);
							triggerInterrupt(3);
						}
						else {

						}
					}
				}
				break;
			case 0xff04:

				int tcycle = (int)(TotalCycleCount&0xff);
				if (tcycle == 0)
					divReset = TotalCycleCount;
				else
					divReset = TotalCycleCount-tcycle+CYCLES_PER_DIV;
				break;
			case 0xff05:
				if (cyclesPerTIMA == 0) {
					IOP[0x05] = value;
				} else {

					int prevval = read(0xff05);
					int prevcount = 0x100 - prevval;
					int newcount = 0x100 - value;
					TIMAEventCycleCount += cyclesPerTIMA * (newcount - prevcount);
					addEventCycleCount(TIMAEventCycleCount);
				}
				break;
			case 0xff06:
				IOP[0x06] = value;
				break;
			case 0xff07:
				if ((value&4) == 0 && cyclesPerTIMA != 0) {

					IOP[0x05] = read(0xff05);
				}
				IOP[0x07] = value;
				calcCyclesPerTIMA();
				calcTIMAEventCycleCount();
				break;
			case 0xff0f:
				IOP[0x0f] = value;
				preCheckInts();
				break;


			case 0xff70:
				CurrentWRAMBank=((value&0x07)<(1)?(1):(value&0x07));
				refreshMemMap();
				break;

			case 0xff72:
			case 0xff73:
			case 0xff74:
			case 0xff75:
				System.out.printf("WARNING: CPU.write(): Write to *undocumented* IO port $%04x\n",index);
				IOP[index-0xff00] = value;
				break;
			case 0xff76:
			case 0xff77:
				System.out.printf("WARNING: CPU.write(): Write to *undocumented* IO port $%04x\n",index);
				break;
			default:
				System.out.printf("TODO: CPU.write(): Write %02x to IO port $%04x\n",value, index);
				break;
			}
		}
		else if(index < 0xffff) {
			HRAM[index-0xff80] = (value);
		}
		else if(index < 0x10000) {
			IE=value;
			preCheckInts();
		}
		else {
			System.out.println("ERROR: CPU.write(): Out of range memory access: $"+index);
		}
	}


	public static void reset() {
		reset(true);
	}

	public static void reset(boolean bios) {
		if(cartridge == null) return;
		BIOS_enabled = bios;

		VC.reset();
		cartridge.CurrentRAMBank=0;
		cartridge.CurrentROMBank=1;

		setPC(BIOS_enabled?0x00:0x100);
		refreshMemMap();

		VC.isCGB = BIOS_enabled?false:isCGB();


		A=BIOS_enabled ? 0x00 : (VC.isCGB?0x11:0x01);
		F=BIOS_enabled ? 0x00 : 0xb0;

		B=0x00;
		C=BIOS_enabled ? 0x00 : 0x13;

		D=0x00;
		E=BIOS_enabled ? 0x00 : 0xd8;

		H=BIOS_enabled ? 0x00 : 0x01;
		L=BIOS_enabled ? 0x00 : 0x4d;
		TotalInstrCount=0;
		TotalCycleCount=0;


		SP=BIOS_enabled ? 0x0000 : 0xfffe;


		write(0xff05, 0x00);
		write(0xff06, 0x00);
		write(0xff07, 0x00);
		write(0xff26, 0xf1);
		write(0xff47, 0xfc);
		write(0xff48, 0xff);
		write(0xff49, 0xff);
		AC.reset();

		CurrentWRAMBank=1;
		doublespeed = false;
		speedswitch = false;
		divReset = 0;

		IE=0;
		IME=true;
		halted=false;
		KeyStatus=0;
		keyBounce=0;
		lastKeyChange = 0;
		keyHistory.clear();
		hblank_dma_state = 0;
		playbackHistoryIndex = -1;
		keyHistoryEnabled = false;
		first_save_string = Version.str;
		delayed_halt = false;
		halt_fail_inc_pc = false;

		preCheckInts();

		NextEventCycleCount = 0;
		VCRenderEventCycleCount = 0;
		lastVCRenderCycleCount = 0;
		KeyBounceEventCycleCount = MAX_CYCLE_COUNT;
		KeyBounceEventPending = false;


		for (int i = 0; i < 0x80; ++i) IOP[i] = 0;
		for (int i = 0; i < 0x7f; ++i) HRAM[i] = 0;
		for (int i = 0; i < 0x08; ++i)
			for (int j = 0; j < 0x1000; ++j)
				WRAM[i][j] = 0;
	}

	public static long cycles() {
		return TotalInstrCount;
	}

	public static void printCPUstatus() {
		String flags = "";
		flags += (( F & ZF_Mask ) == ZF_Mask )?"Z ":"z ";
		flags += (( F & NF_Mask ) == NF_Mask )?"N ":"n ";
		flags += (( F & HC_Mask ) == HC_Mask )?"H ":"h ";
		flags += (( F & CF_Mask ) == CF_Mask )?"C ":"c ";
		flags += (( F & ( 1 <<3 ) ) == ( 1 <<3 ) )?"1 ":"0 ";
		flags += (( F & ( 1 <<2 ) ) == ( 1 <<2 ) )?"1 ":"0 ";
		flags += (( F & ( 1 <<1 ) ) == ( 1 <<1 ) )?"1 ":"0 ";
		flags += (( F & ( 1 <<0 ) ) == ( 1 <<0 ) )?"1 ":"0 ";
		System.out.println("---CPU Status for cycle "+TotalCycleCount+" , instruction "+TotalInstrCount+"---");
		System.out.printf("   A=$%02x    B=$%02x    C=$%02x    D=$%02x   E=$%02x   F=$%02x   H=$%02x   L=$%02x\n", A, B, C, D, E, F, H,L);
		System.out.printf("  PC=$%04x SP=$%04x                           flags="+flags+"\n",getPC(),SP);

	}

	public static int checkInterrupts() {
		if(IME) {
			int ir = IOP[0x0f]&IE;
			if((ir&(1<<0))!=0) {
				IOP[0x0f] &= ~(1<<0);
				interrupt(0x40);
				return 1;
			}
			else if ((ir&(1<<1))!=0) {
				IOP[0x0f] &= ~(1<<1);
				interrupt(0x48);
				return 1;
			}
			else if ((ir&(1<<2))!=0) {
				IOP[0x0f] &= ~(1<<2);
				interrupt(0x50);
				return 1;
			}
			else if ((ir&(1<<3))!=0) {
				IOP[0x0f] &= ~(1<<3);
				interrupt(0x58);
				return 1;
			}
			else if ((ir&(1<<4))!=0) {
				IOP[0x0f] &= ~(1<<4);
				interrupt(0x60);
				return 1;
			}
			preCheckInts();
		}
		return 0;
	}

	public static void interrupt(int i) {
		IME = false;
		pushPC();
		setPC(i);
		IOP[0x0f] &= ~i;
		preCheckInts();
	}

	public static void triggerInterrupt(int i) {
		IOP[0x0f] |= (1<<i);

		if (halted && IME && ((1<<i) & IE) != 0) halted = false;
		preCheckInts();
	}

	public static void pressButton(int i) {
		GUIKeyStatus |= i;
	}

	public static void releaseButton(int i) {
		GUIKeyStatus &= ~i;
	}

	public static void pressRemoteButton(int i) {
		RemoteKeyStatus |= i;
	}

	public static void releaseRemoteButton(int i) {
		RemoteKeyStatus &= ~i;
	}


	private static final int registerRead(int regNum) {
		switch (regNum) {
		case 0:
			return B;
		case 1:
			return C;
		case 2:
			return D;
		case 3:
			return E;
		case 4:
			return H;
		case 5:
			return L;
		case 6:
			return read(H<<8 | L);
		case 7:
			return A;
		default:
			return -1;
		}
	}

	public static void executeALU(int b1) {
		int operand = registerRead(b1 & 0x07);
		switch ((b1 & 0x38) >> 3) {
		case 1:
			if ((F & CF_Mask) != 0) {
				operand++;
			}

		case 0:

			if ((A & 0x0F) + (operand & 0x0F) >= 0x10) {
				F = HC_Mask;
			} else {
				F = 0;
			}

			A += operand;

			if (A > 0xff) {
				F |= CF_Mask;
				A &= 0xff;
			}

			if (A == 0) {
				F |= ZF_Mask;
			}
			break;
		case 3:
			if ((F & CF_Mask) != 0) {
				operand++;
			}

		case 2:

			F = NF_Mask;

			if ((A & 0x0F) < (operand & 0x0F)) {
				F |= HC_Mask;
			}

			A -= operand;

			if (A < 0) {
				F |= CF_Mask;
				A &= 0xff;
			}
			if (A == 0) {
				F |= ZF_Mask;
			}

			break;
		case 4:
			A &= operand;
			if (A == 0) {
				F = HC_Mask + ZF_Mask;
			} else {
				F = HC_Mask;
			}
			break;
		case 5:
			A ^= operand;
			F = (A == 0) ? ZF_Mask : 0;
			break;
		case 6:
			A |= operand;
			F = (A == 0) ? ZF_Mask : 0;
			break;
		case 7:
			F = NF_Mask;
			if (A == operand) {
				F |= ZF_Mask;
			} else if (A < operand) {
				F |= CF_Mask;
			}
			if ((A & 0x0F) < (operand & 0x0F)) {
				F |= HC_Mask;
			}
			break;
		}
	}

	public static void handleTIMAEvent()
	{
		IOP[0x05] = IOP[0x06];
		triggerInterrupt(2);
		TIMAEventCycleCount += cyclesPerTIMA * (0x100 - IOP[0x05]);
	};

	public static void handleKeyBounceEvent()
	{
		if (keyBounce > 0) {
			triggerInterrupt(4);
			--keyBounce;
			KeyBounceEventCycleCount = TotalCycleCount + (doublespeed ? 20000 : 10000);
			KeyBounceEventPending = true;
		} else {
			KeyBounceEventCycleCount = MAX_CYCLE_COUNT;
			KeyBounceEventPending = false;
		}
	};

	public static void handleVCRenderEvent()
	{
		int cycles = (int)(TotalCycleCount - lastVCRenderCycleCount);

		if (doublespeed) cycles /= 2;
		int cntdown = VC.render(cycles);
		if (doublespeed) cntdown *= 2;




		lastVCRenderCycleCount = TotalCycleCount;
		VCRenderEventCycleCount = TotalCycleCount + cntdown;

	};

	public static void addEventCycleCount(long count)
	{
		if (count < NextEventCycleCount)
			NextEventCycleCount = count;
	};

	public static void handleEvents() {
		if (TotalCycleCount >= VCRenderEventCycleCount);
		handleVCRenderEvent();
		NextEventCycleCount = VCRenderEventCycleCount;

		if (TIMAEventPending) {
			if (TotalCycleCount >= TIMAEventCycleCount)
				handleTIMAEvent();
			addEventCycleCount(TIMAEventCycleCount);
		}

		if (KeyBounceEventPending) {
			if (TotalCycleCount >= KeyBounceEventCycleCount)
				handleKeyBounceEvent();
			addEventCycleCount(KeyBounceEventCycleCount);
		}
	};


	public static boolean fastCheckInts;
	public static void preCheckInts() {
		fastCheckInts = (IME && ((IOP[0x0f]&IE)!=0));
	}

	public static int execute() {
		int op;
		int cycles;
		int t_mm[]; int t_mi; int t_w16; int t_acc; int t_vol; int t_mask;;

		if (fastCheckInts) {
			checkInterrupts();
			halted = false;

			return 5;
		}

		if (delayed_halt && IME) {
			delayed_halt = false;
			halted = true;
		}

		if (halted) {

			return (int)((NextEventCycleCount - TotalCycleCount + 3)>>2);
		}

		if (halt_fail_inc_pc) {

			halt_fail_inc_pc = false;
			--localPC;
		}

		if (localPC >= 0 && localPC < decoderMaxCruise) {
			op=(decoderMemory[localPC++]);
		} else {
			int pc = getPC();
			op=read(pc++);
			setPC(pc);
		}

		cycles = Tables.cycles[op];


		switch ( op ) {
		case 0x00: break;
		case 0xf3: IME=false; preCheckInts(); break;
		case 0xfb: IME=true; preCheckInts(); break;
		case 0xea: write(((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)), A); break;
		case 0xfa: A = read(((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8))); break;
		case 0xe0: write(((decoderMemory[localPC++])) | 0xff00, A); break;
		case 0xf0: A = (read_slow(((decoderMemory[localPC++])) | 0xff00)); break;
		case 0xe2: write(C | 0xff00, A); break;
		case 0xf2: A = (read_slow(C | 0xff00)); break;
		case 0xf9: SP = ((H<<8)|L); break;
		case 0x22: write(((H<<8)|L), A); { { H = (t_w16=(((H<<8)|L) + 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case 0x2a: A = read(((H<<8)|L)); { { H = (t_w16=(((H<<8)|L) + 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case 0x32: write(((H<<8)|L), A); { { H = (t_w16=(((H<<8)|L) - 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case 0x3a: A = read(((H<<8)|L)); { { H = (t_w16=(((H<<8)|L) - 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case 0xc3: { setPC(((decoderMemory[localPC])|((decoderMemory[localPC+1])<<8))); }; break;
		case 0xc2: { if ((F&ZF_Mask)==0) { setPC(((decoderMemory[localPC])|((decoderMemory[localPC+1])<<8))); } else { --cycles; localPC+=2; }; };; break;
		case 0xca: { if ((F&ZF_Mask)!=0) { setPC(((decoderMemory[localPC])|((decoderMemory[localPC+1])<<8))); } else { --cycles; localPC+=2; }; };; break;
		case 0xd2: { if ((F&CF_Mask)==0) { setPC(((decoderMemory[localPC])|((decoderMemory[localPC+1])<<8))); } else { --cycles; localPC+=2; }; };; break;
		case 0xda: { if ((F&CF_Mask)!=0) { setPC(((decoderMemory[localPC])|((decoderMemory[localPC+1])<<8))); } else { --cycles; localPC+=2; }; };; break;
		case 0xcd: { t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); pushPC(); setPC(t_acc); }; break;
		case 0xc4: { if ((F&ZF_Mask)==0) { t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); pushPC(); setPC(t_acc); } else { cycles-=3; localPC+=2; }; };; break;
		case 0xcc: { if ((F&ZF_Mask)!=0) { t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); pushPC(); setPC(t_acc); } else { cycles-=3; localPC+=2; }; };; break;
		case 0xd4: { if ((F&CF_Mask)==0) { t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); pushPC(); setPC(t_acc); } else { cycles-=3; localPC+=2; }; };; break;
		case 0xdc: { if ((F&CF_Mask)!=0) { t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); pushPC(); setPC(t_acc); } else { cycles-=3; localPC+=2; }; };; break;
		case 0x18: { localPC += 1+(((((decoderMemory[localPC])))^0x80)-0x80); }; break;
		case 0x20: { if ((F&ZF_Mask)==0) { localPC += 1+(((((decoderMemory[localPC])))^0x80)-0x80); } else { --cycles; ++localPC; }; };; break;
		case 0x28: { if ((F&ZF_Mask)!=0) { localPC += 1+(((((decoderMemory[localPC])))^0x80)-0x80); } else { --cycles; ++localPC; }; };; break;
		case 0x30: { if ((F&CF_Mask)==0) { localPC += 1+(((((decoderMemory[localPC])))^0x80)-0x80); } else { --cycles; ++localPC; }; };; break;
		case 0x38: { if ((F&CF_Mask)!=0) { localPC += 1+(((((decoderMemory[localPC])))^0x80)-0x80); } else { --cycles; ++localPC; }; };; break;
		case 0xc9: { popPC(); }; break;
		case 0xc0: { if ((F&ZF_Mask)==0) { popPC(); } else { cycles-=3; }; };; break;
		case 0xc8: { if ((F&ZF_Mask)!=0) { popPC(); } else { cycles-=3; }; };; break;
		case 0xd0: { if ((F&CF_Mask)==0) { popPC(); } else { cycles-=3; }; };; break;
		case 0xd8: { if ((F&CF_Mask)!=0) { popPC(); } else { cycles-=3; }; };; break;
		case 0x02: write(((B<<8)|C), A); break;
		case 0x0A: A = read(((B<<8)|C)); break;
		case 0x12: write(((D<<8)|E), A); break;
		case 0x1A: A = read(((D<<8)|E)); break;
		case 0x70: write(((H<<8)|L), B); break;
		case 0x71: write(((H<<8)|L), C); break;
		case 0x72: write(((H<<8)|L), D); break;
		case 0x73: write(((H<<8)|L), E); break;
		case 0x74: write(((H<<8)|L), H); break;
		case 0x75: write(((H<<8)|L), L); break;
		case 0x77: write(((H<<8)|L), A); break;
		case 0x36: write(((H<<8)|L), ((decoderMemory[localPC++]))); break;
		case 0x76: {
			if (IME) {
				halted = true;
			} else {
				delayed_halt = true;
				if (!isCGB() && (IOP[0x0f]&IE) > 0)
					halt_fail_inc_pc = true;
			}
		}; break;
		case 0xd9: IME = true; preCheckInts(); { popPC(); }; break;
		case 0xc1: { B = (t_w16=(read(SP++)|(read(SP++)<<8))) >> 8; C = t_w16 & 0xFF; }; break;
		case 0xd1: { D = (t_w16=(read(SP++)|(read(SP++)<<8))) >> 8; E = t_w16 & 0xFF; }; break;
		case 0xe1: { H = (t_w16=(read(SP++)|(read(SP++)<<8))) >> 8; L = t_w16 & 0xFF; }; break;
		case 0xf1: { A = (t_w16=(read(SP++)|(read(SP++)<<8))) >> 8; F = t_w16 & 0xF0; }; break;
		case 0xc5: { SP=(SP-1)&0xffff; write(SP, (t_w16=((B<<8)|C))>>8); SP=(SP-1)&0xffff; write(SP, (t_w16)&0xff); }; break;
		case 0xd5: { SP=(SP-1)&0xffff; write(SP, (t_w16=((D<<8)|E))>>8); SP=(SP-1)&0xffff; write(SP, (t_w16)&0xff); }; break;
		case 0xe5: { SP=(SP-1)&0xffff; write(SP, (t_w16=((H<<8)|L))>>8); SP=(SP-1)&0xffff; write(SP, (t_w16)&0xff); }; break;
		case 0xf5: { SP=(SP-1)&0xffff; write(SP, (t_w16=((A<<8)|F))>>8); SP=(SP-1)&0xffff; write(SP, (t_w16)&0xff); }; break;
		case 0x09: { t_vol=(B<<8)|C; t_acc = ((H<<8)|L) + t_vol; F = (F & (ZF_Mask)) | (HC_Mask & ((H ^ (B) ^ (((t_acc)>>8))) << 1)) | ((t_acc>>12)&CF_Mask); H = (t_acc&0xff00)>>8; L = (t_acc&0xff); }; break;
		case 0x19: { t_vol=(D<<8)|E; t_acc = ((H<<8)|L) + t_vol; F = (F & (ZF_Mask)) | (HC_Mask & ((H ^ (D) ^ (((t_acc)>>8))) << 1)) | ((t_acc>>12)&CF_Mask); H = (t_acc&0xff00)>>8; L = (t_acc&0xff); }; break;
		case 0x29: { t_vol=(H<<8)|L; t_acc = ((H<<8)|L) + t_vol; F = (F & (ZF_Mask)) | (HC_Mask & ((H ^ (H) ^ (((t_acc)>>8))) << 1)) | ((t_acc>>12)&CF_Mask); H = (t_acc&0xff00)>>8; L = (t_acc&0xff); }; break;
		case 0x39: { t_vol=((SP>>8)<<8)|(SP&0xff); t_acc = ((H<<8)|L) + t_vol; F = (F & (ZF_Mask)) | (HC_Mask & ((H ^ ((SP>>8)) ^ (((t_acc)>>8))) << 1)) | ((t_acc>>12)&CF_Mask); H = (t_acc&0xff00)>>8; L = (t_acc&0xff); }; break;
		case 0xe9: setPC(((H<<8)|L)); break;
		case 0x2f: A ^= 0xff; F |= (NF_Mask|HC_Mask); break;
		case 0x07: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(A)]; A = ShTablesRLC .val[t_acc][(A)]; }; F &= CF_Mask; break;
		case 0x17: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(A)]; A = ShTablesRL .val[t_acc][(A)]; }; F &= CF_Mask; break;
		case 0x0f: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(A)]; A = ShTablesRRC .val[t_acc][(A)]; }; F &= CF_Mask; break;
		case 0x1f: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(A)]; A = ShTablesRR .val[t_acc][(A)]; }; F &= CF_Mask; break;
		case 0xc7: pushPC(); setPC(0x00); break;
		case 0xcf: pushPC(); setPC(0x08); break;
		case 0xd7: pushPC(); setPC(0x10); break;
		case 0xdf: pushPC(); setPC(0x18); break;
		case 0xe7: pushPC(); setPC(0x20); break;
		case 0xef: pushPC(); setPC(0x28); break;
		case 0xf7: pushPC(); setPC(0x30); break;
		case 0xff: pushPC(); setPC(0x38); break;
		case 0x37: F &= ZF_Mask; F |= CF_Mask; break;
		case 0x3f: F &= (ZF_Mask|CF_Mask); F ^= CF_Mask; break;
		case 0x08: {
			t_acc = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8));
			write(t_acc, SP&0xff);
			write((t_acc+1)&0xffff, SP>>8);
		}; break;
		case 0xf8:{
			{ H = SP >> 8; L = SP & 0xFF; };
			L += (((((decoderMemory[localPC++])))^0x80)-0x80);
			F = 0;
			if (L > 0xff) {
				L &= 0xff;
				F |= HC_Mask;
				++H;
				if (H > 0xff) {
					H &= 0xff;
					F |= CF_Mask;
				}
			}
			else if (L < 0) {
				L &= 0xff;
				F |= HC_Mask;
				--H;
				if (H < 0) {
					H &= 0xff;
					F |= CF_Mask;
				}
			}
		};break;
		case 0x27:{
			if ((F & (NF_Mask))!=NF_Mask) {
				if ((F & (HC_Mask))==HC_Mask|| (A & 0xF) > 9)
					A += 0x06;

				if ((F & (CF_Mask))==CF_Mask|| A > 0x9F)
					A += 0x60;
			} else {
				if ((F & (HC_Mask))==HC_Mask)
					A = (A - 6) & 0xFF;

				if ((F & (CF_Mask))==CF_Mask)
					A -= 0x60;
			}
			F &= ~(HC_Mask | ZF_Mask);
			if ((A & 0x100) == 0x100)
				F |= CF_Mask;

			A &= 0xFF;

			if (A == 0)
				F |= ZF_Mask;
		};break;
		case 0xe8:{
			t_acc = SP;
			SP += (((((decoderMemory[localPC++])))^0x80)-0x80);
			F = ((SP >> 8) != (t_acc >> 8)) ? HC_Mask : 0;
			if ((SP & ~0xffff) != 0) {
				SP &= 0xffff;
				F |= CF_Mask;
			}
		};break;
		case 0x10: if (speedswitch) {
			System.out.println("Speed switch!");
			doublespeed = !doublespeed;
			++localPC;
			speedswitch = false;
		}; break;
		case (0xfe) : { t_vol = (((decoderMemory[localPC++]))); { t_acc = A - (t_vol); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; }; break;
		case (0xb8) : { t_acc = A - (B); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (B) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+1: { t_acc = A - (C); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (C) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+2: { t_acc = A - (D); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (D) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+3: { t_acc = A - (E); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (E) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+4: { t_acc = A - (H); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (H) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+5: { t_acc = A - (L); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (L) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xb8)+6: { t_vol = (read(((H<<8)|L))); { t_acc = A - (t_vol); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; }; break;
		case (0xb8)+7: { t_acc = A - (A); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (A) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; break;
		case (0xe6) : { A &= (((decoderMemory[localPC++]))); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0) : { A &= (B); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+1: { A &= (C); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+2: { A &= (D); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+3: { A &= (E); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+4: { A &= (H); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+5: { A &= (L); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+6: { A &= (read(((H<<8)|L))); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa0)+7: { A &= (A); F = HC_Mask | ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xee) : { A ^= (((decoderMemory[localPC++]))); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8) : { A ^= (B); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+1: { A ^= (C); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+2: { A ^= (D); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+3: { A ^= (E); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+4: { A ^= (H); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+5: { A ^= (L); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+6: { A ^= (read(((H<<8)|L))); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xa8)+7: { A ^= (A); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xf6) : { A |= (((decoderMemory[localPC++]))); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0) : { A |= (B); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+1: { A |= (C); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+2: { A |= (D); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+3: { A |= (E); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+4: { A |= (H); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+5: { A |= (L); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+6: { A |= (read(((H<<8)|L))); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xb0)+7: { A |= (A); F = ( (A) != 0 ? 0 : ZF_Mask ); }; break;
		case (0xC6) : { t_vol = (((decoderMemory[localPC++]))); { t_acc = (A) + (t_vol); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; }; break;
		case (0x80) : { t_acc = (A) + (B); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (B) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+1: { t_acc = (A) + (C); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (C) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+2: { t_acc = (A) + (D); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (D) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+3: { t_acc = (A) + (E); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (E) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+4: { t_acc = (A) + (H); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (H) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+5: { t_acc = (A) + (L); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (L) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0x80)+6: { t_vol = (read(((H<<8)|L))); { t_acc = (A) + (t_vol); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; }; break;
		case (0x80)+7: { t_acc = (A) + (A); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & (((A) ^ (A) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << 4); A = ((t_acc)&0xff); };; break;
		case (0xCE) : { t_vol = (((decoderMemory[localPC++]))); { t_acc = A + (t_vol) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; }; break;
		case (0x88) : { t_acc = A + (B) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (B) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+1: { t_acc = A + (C) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (C) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+2: { t_acc = A + (D) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (D) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+3: { t_acc = A + (E) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (E) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+4: { t_acc = A + (H) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (H) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+5: { t_acc = A + (L) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (L) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0x88)+6: { t_vol = (read(((H<<8)|L))); { t_acc = A + (t_vol) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; }; break;
		case (0x88)+7: { t_acc = A + (A) + ((F&CF_Mask)>>CF_Shift); F = (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (A) ^ ((t_acc)&0xff)) << 1)) | ((((t_acc)>>8)) << CF_Shift); A = ((t_acc)&0xff); }; break;
		case (0xD6) : { t_vol = (((decoderMemory[localPC++]))); { { t_acc = A - ((t_vol)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((t_vol)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; }; break;
		case (0x90) : { { t_acc = A - ((B)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((B)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+1: { { t_acc = A - ((C)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((C)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+2: { { t_acc = A - ((D)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((D)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+3: { { t_acc = A - ((E)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((E)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+4: { { t_acc = A - ((H)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((H)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+5: { { t_acc = A - ((L)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((L)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0x90)+6: { t_vol = (read(((H<<8)|L))); { { t_acc = A - ((t_vol)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((t_vol)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; }; break;
		case (0x90)+7: { { t_acc = A - ((A)); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ ((A)) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); }; A = ((t_acc)&0xff); }; break;
		case (0xDE) : { t_vol = (((decoderMemory[localPC++]))); { t_acc = A - (t_vol) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; }; break;
		case (0x98) : { t_acc = A - (B) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (B) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+1: { t_acc = A - (C) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (C) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+2: { t_acc = A - (D) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (D) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+3: { t_acc = A - (E) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (E) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+4: { t_acc = A - (H) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (H) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+5: { t_acc = A - (L) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (L) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x98)+6: { t_vol = (read(((H<<8)|L))); { t_acc = A - (t_vol) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (t_vol) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; }; break;
		case (0x98)+7: { t_acc = A - (A) - ((F&CF_Mask)>>CF_Shift); F = NF_Mask | (( (((t_acc)&0xff)) != 0 ? 0 : ZF_Mask )) | (HC_Mask & ((A ^ (A) ^ ((t_acc)&0xff)) << 1)) | (CF_Mask&(t_acc>>8)); A = ((t_acc)&0xff); }; break;
		case (0x04+(0<<3)): { B = (((B)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(B)] ; }; break;
		case (0x05+(0<<3)): { B = (((B)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(B)] ; }; break;
		case (0x04+(1<<3)): { C = (((C)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(C)] ; }; break;
		case (0x05+(1<<3)): { C = (((C)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(C)] ; }; break;
		case (0x04+(2<<3)): { D = (((D)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(D)] ; }; break;
		case (0x05+(2<<3)): { D = (((D)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(D)] ; }; break;
		case (0x04+(3<<3)): { E = (((E)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(E)] ; }; break;
		case (0x05+(3<<3)): { E = (((E)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(E)] ; }; break;
		case (0x04+(4<<3)): { H = (((H)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(H)] ; }; break;
		case (0x05+(4<<3)): { H = (((H)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(H)] ; }; break;
		case (0x04+(5<<3)): { L = (((L)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(L)] ; }; break;
		case (0x05+(5<<3)): { L = (((L)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(L)] ; }; break;
		case (0x04+(7<<3)): { A = (((A)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(A)] ; }; break;
		case (0x05+(7<<3)): { A = (((A)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(A)] ; }; break;
		case 0x34: { t_acc = read(((H<<8)|L)); { t_acc = (((t_acc)+1)&0xff); F = (F & CF_Mask) | Tables.incflag[(t_acc)] ; }; write(((H<<8)|L), t_acc); }; break;
		case 0x35: { t_acc = read(((H<<8)|L)); { t_acc = (((t_acc)-1)&0xff); F = (F & CF_Mask) | Tables.decflag[(t_acc)] ; }; write(((H<<8)|L), t_acc); }; break;
		case (0x03+(0<<4)): { { B = (t_w16=(((B<<8)|C) + 1) & 0xffff) >> 8; C = t_w16 & 0xFF; }; }; break;
		case (0x0b+(0<<4)): { { B = (t_w16=(((B<<8)|C) - 1) & 0xffff) >> 8; C = t_w16 & 0xFF; }; }; break;
		case (0x03+(1<<4)): { { D = (t_w16=(((D<<8)|E) + 1) & 0xffff) >> 8; E = t_w16 & 0xFF; }; }; break;
		case (0x0b+(1<<4)): { { D = (t_w16=(((D<<8)|E) - 1) & 0xffff) >> 8; E = t_w16 & 0xFF; }; }; break;
		case (0x03+(2<<4)): { { H = (t_w16=(((H<<8)|L) + 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case (0x0b+(2<<4)): { { H = (t_w16=(((H<<8)|L) - 1) & 0xffff) >> 8; L = t_w16 & 0xFF; }; }; break;
		case (0x03+(3<<4)): { { SP = ((SP) + 1) & 0xffff; }; }; break;
		case (0x0b+(3<<4)): { { SP = ((SP) - 1) & 0xffff; }; }; break;
		case (0x40+(0<<3)+(0)): { B = B; }; break;
		case (0x40+(0<<3)+(1)): { B = C; }; break;
		case (0x40+(0<<3)+(2)): { B = D; }; break;
		case (0x40+(0<<3)+(3)): { B = E; }; break;
		case (0x40+(0<<3)+(4)): { B = H; }; break;
		case (0x40+(0<<3)+(5)): { B = L; }; break;
		case (0x40+(0<<3)+(7)): { B = A; }; break;
		case (0x06+(0<<3)): { B = ((decoderMemory[localPC++])); }; break;
		case (0x46+(0<<3)): { B = read(((H<<8)|L)); }; break;
		case (0x40+(1<<3)+(0)): { C = B; }; break;
		case (0x40+(1<<3)+(1)): { C = C; }; break;
		case (0x40+(1<<3)+(2)): { C = D; }; break;
		case (0x40+(1<<3)+(3)): { C = E; }; break;
		case (0x40+(1<<3)+(4)): { C = H; }; break;
		case (0x40+(1<<3)+(5)): { C = L; }; break;
		case (0x40+(1<<3)+(7)): { C = A; }; break;
		case (0x06+(1<<3)): { C = ((decoderMemory[localPC++])); }; break;
		case (0x46+(1<<3)): { C = read(((H<<8)|L)); }; break;
		case (0x40+(2<<3)+(0)): { D = B; }; break;
		case (0x40+(2<<3)+(1)): { D = C; }; break;
		case (0x40+(2<<3)+(2)): { D = D; }; break;
		case (0x40+(2<<3)+(3)): { D = E; }; break;
		case (0x40+(2<<3)+(4)): { D = H; }; break;
		case (0x40+(2<<3)+(5)): { D = L; }; break;
		case (0x40+(2<<3)+(7)): { D = A; }; break;
		case (0x06+(2<<3)): { D = ((decoderMemory[localPC++])); }; break;
		case (0x46+(2<<3)): { D = read(((H<<8)|L)); }; break;
		case (0x40+(3<<3)+(0)): { E = B; }; break;
		case (0x40+(3<<3)+(1)): { E = C; }; break;
		case (0x40+(3<<3)+(2)): { E = D; }; break;
		case (0x40+(3<<3)+(3)): { E = E; }; break;
		case (0x40+(3<<3)+(4)): { E = H; }; break;
		case (0x40+(3<<3)+(5)): { E = L; }; break;
		case (0x40+(3<<3)+(7)): { E = A; }; break;
		case (0x06+(3<<3)): { E = ((decoderMemory[localPC++])); }; break;
		case (0x46+(3<<3)): { E = read(((H<<8)|L)); }; break;
		case (0x40+(4<<3)+(0)): { H = B; }; break;
		case (0x40+(4<<3)+(1)): { H = C; }; break;
		case (0x40+(4<<3)+(2)): { H = D; }; break;
		case (0x40+(4<<3)+(3)): { H = E; }; break;
		case (0x40+(4<<3)+(4)): { H = H; }; break;
		case (0x40+(4<<3)+(5)): { H = L; }; break;
		case (0x40+(4<<3)+(7)): { H = A; }; break;
		case (0x06+(4<<3)): { H = ((decoderMemory[localPC++])); }; break;
		case (0x46+(4<<3)): { H = read(((H<<8)|L)); }; break;
		case (0x40+(5<<3)+(0)): { L = B; }; break;
		case (0x40+(5<<3)+(1)): { L = C; }; break;
		case (0x40+(5<<3)+(2)): { L = D; }; break;
		case (0x40+(5<<3)+(3)): { L = E; }; break;
		case (0x40+(5<<3)+(4)): { L = H; }; break;
		case (0x40+(5<<3)+(5)): { L = L; }; break;
		case (0x40+(5<<3)+(7)): { L = A; }; break;
		case (0x06+(5<<3)): { L = ((decoderMemory[localPC++])); }; break;
		case (0x46+(5<<3)): { L = read(((H<<8)|L)); }; break;
		case (0x40+(7<<3)+(0)): { A = B; }; break;
		case (0x40+(7<<3)+(1)): { A = C; }; break;
		case (0x40+(7<<3)+(2)): { A = D; }; break;
		case (0x40+(7<<3)+(3)): { A = E; }; break;
		case (0x40+(7<<3)+(4)): { A = H; }; break;
		case (0x40+(7<<3)+(5)): { A = L; }; break;
		case (0x40+(7<<3)+(7)): { A = A; }; break;
		case (0x06+(7<<3)): { A = ((decoderMemory[localPC++])); }; break;
		case (0x46+(7<<3)): { A = read(((H<<8)|L)); }; break;
		case (0x01+(0<<4)): { C = ((decoderMemory[localPC++])); B = (((decoderMemory[localPC++]))); } ; break;
		case (0x01+(1<<4)): { E = ((decoderMemory[localPC++])); D = (((decoderMemory[localPC++]))); } ; break;
		case (0x01+(2<<4)): { L = ((decoderMemory[localPC++])); H = (((decoderMemory[localPC++]))); } ; break;
		case (0x01+(3<<4)): { SP = ((decoderMemory[localPC++])|((decoderMemory[localPC++])<<8)); } ; break;
		case 0xcb:
			op = ((decoderMemory[localPC++]));
			cycles = Tables.cb_cycles[op];
			switch ( op ) {
			case (0x40)+(0<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(0<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 0)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(1<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(1<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 1)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(2<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(2<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 2)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(3<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(3<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 3)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(4<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(4<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 4)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(5<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(5<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 5)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(6<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(6<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 6)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x40)+(7<<3)+0: { F = (F & CF_Mask) | HC_Mask | ( ((B) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+1: { F = (F & CF_Mask) | HC_Mask | ( ((C) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+2: { F = (F & CF_Mask) | HC_Mask | ( ((D) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+3: { F = (F & CF_Mask) | HC_Mask | ( ((E) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+4: { F = (F & CF_Mask) | HC_Mask | ( ((H) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+5: { F = (F & CF_Mask) | HC_Mask | ( ((L) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+7: { F = (F & CF_Mask) | HC_Mask | ( ((A) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; break;
			case (0x40)+(7<<3)+6: { { F = (F & CF_Mask) | HC_Mask | ( ((read(((H<<8)|L))) & (1 << 7)) != 0 ? 0 : ZF_Mask ); }; }; break;
			case (0x80)+(0<<3)+0: { B &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+1: { C &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+2: { D &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+3: { E &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+4: { H &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+5: { L &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+7: { A &= ~(1 << 0); }; break;
			case (0x80)+(0<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 0)); }; break;
			case (0x80)+(1<<3)+0: { B &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+1: { C &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+2: { D &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+3: { E &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+4: { H &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+5: { L &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+7: { A &= ~(1 << 1); }; break;
			case (0x80)+(1<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 1)); }; break;
			case (0x80)+(2<<3)+0: { B &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+1: { C &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+2: { D &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+3: { E &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+4: { H &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+5: { L &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+7: { A &= ~(1 << 2); }; break;
			case (0x80)+(2<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 2)); }; break;
			case (0x80)+(3<<3)+0: { B &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+1: { C &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+2: { D &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+3: { E &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+4: { H &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+5: { L &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+7: { A &= ~(1 << 3); }; break;
			case (0x80)+(3<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 3)); }; break;
			case (0x80)+(4<<3)+0: { B &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+1: { C &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+2: { D &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+3: { E &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+4: { H &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+5: { L &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+7: { A &= ~(1 << 4); }; break;
			case (0x80)+(4<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 4)); }; break;
			case (0x80)+(5<<3)+0: { B &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+1: { C &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+2: { D &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+3: { E &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+4: { H &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+5: { L &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+7: { A &= ~(1 << 5); }; break;
			case (0x80)+(5<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 5)); }; break;
			case (0x80)+(6<<3)+0: { B &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+1: { C &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+2: { D &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+3: { E &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+4: { H &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+5: { L &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+7: { A &= ~(1 << 6); }; break;
			case (0x80)+(6<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 6)); }; break;
			case (0x80)+(7<<3)+0: { B &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+1: { C &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+2: { D &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+3: { E &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+4: { H &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+5: { L &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+7: { A &= ~(1 << 7); }; break;
			case (0x80)+(7<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) & ~(1 << 7)); }; break;
			case (0xc0)+(0<<3)+0: { B |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+1: { C |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+2: { D |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+3: { E |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+4: { H |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+5: { L |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+7: { A |= (1 << 0); }; break;
			case (0xc0)+(0<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 0)); }; break;
			case (0xc0)+(1<<3)+0: { B |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+1: { C |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+2: { D |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+3: { E |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+4: { H |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+5: { L |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+7: { A |= (1 << 1); }; break;
			case (0xc0)+(1<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 1)); }; break;
			case (0xc0)+(2<<3)+0: { B |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+1: { C |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+2: { D |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+3: { E |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+4: { H |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+5: { L |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+7: { A |= (1 << 2); }; break;
			case (0xc0)+(2<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 2)); }; break;
			case (0xc0)+(3<<3)+0: { B |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+1: { C |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+2: { D |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+3: { E |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+4: { H |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+5: { L |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+7: { A |= (1 << 3); }; break;
			case (0xc0)+(3<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 3)); }; break;
			case (0xc0)+(4<<3)+0: { B |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+1: { C |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+2: { D |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+3: { E |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+4: { H |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+5: { L |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+7: { A |= (1 << 4); }; break;
			case (0xc0)+(4<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 4)); }; break;
			case (0xc0)+(5<<3)+0: { B |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+1: { C |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+2: { D |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+3: { E |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+4: { H |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+5: { L |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+7: { A |= (1 << 5); }; break;
			case (0xc0)+(5<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 5)); }; break;
			case (0xc0)+(6<<3)+0: { B |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+1: { C |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+2: { D |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+3: { E |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+4: { H |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+5: { L |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+7: { A |= (1 << 6); }; break;
			case (0xc0)+(6<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 6)); }; break;
			case (0xc0)+(7<<3)+0: { B |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+1: { C |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+2: { D |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+3: { E |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+4: { H |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+5: { L |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+7: { A |= (1 << 7); }; break;
			case (0xc0)+(7<<3)+6: { write(((H<<8)|L), read(((H<<8)|L)) | (1 << 7)); }; break;
			case (0x00)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(B)]; B = ShTablesRLC .val[t_acc][(B)]; }; break;
			case (0x00)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(C)]; C = ShTablesRLC .val[t_acc][(C)]; }; break;
			case (0x00)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(D)]; D = ShTablesRLC .val[t_acc][(D)]; }; break;
			case (0x00)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(E)]; E = ShTablesRLC .val[t_acc][(E)]; }; break;
			case (0x00)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(H)]; H = ShTablesRLC .val[t_acc][(H)]; }; break;
			case (0x00)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(L)]; L = ShTablesRLC .val[t_acc][(L)]; }; break;
			case (0x00)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRLC .flag[t_acc][(A)]; A = ShTablesRLC .val[t_acc][(A)]; }; break;
			case (0x00)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesRLC .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesRLC .val[t_acc][t_vol]); }; break;
			case (0x08)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(B)]; B = ShTablesRRC .val[t_acc][(B)]; }; break;
			case (0x08)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(C)]; C = ShTablesRRC .val[t_acc][(C)]; }; break;
			case (0x08)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(D)]; D = ShTablesRRC .val[t_acc][(D)]; }; break;
			case (0x08)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(E)]; E = ShTablesRRC .val[t_acc][(E)]; }; break;
			case (0x08)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(H)]; H = ShTablesRRC .val[t_acc][(H)]; }; break;
			case (0x08)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(L)]; L = ShTablesRRC .val[t_acc][(L)]; }; break;
			case (0x08)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRRC .flag[t_acc][(A)]; A = ShTablesRRC .val[t_acc][(A)]; }; break;
			case (0x08)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesRRC .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesRRC .val[t_acc][t_vol]); }; break;
			case (0x10)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(B)]; B = ShTablesRL .val[t_acc][(B)]; }; break;
			case (0x10)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(C)]; C = ShTablesRL .val[t_acc][(C)]; }; break;
			case (0x10)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(D)]; D = ShTablesRL .val[t_acc][(D)]; }; break;
			case (0x10)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(E)]; E = ShTablesRL .val[t_acc][(E)]; }; break;
			case (0x10)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(H)]; H = ShTablesRL .val[t_acc][(H)]; }; break;
			case (0x10)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(L)]; L = ShTablesRL .val[t_acc][(L)]; }; break;
			case (0x10)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRL .flag[t_acc][(A)]; A = ShTablesRL .val[t_acc][(A)]; }; break;
			case (0x10)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesRL .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesRL .val[t_acc][t_vol]); }; break;
			case (0x18)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(B)]; B = ShTablesRR .val[t_acc][(B)]; }; break;
			case (0x18)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(C)]; C = ShTablesRR .val[t_acc][(C)]; }; break;
			case (0x18)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(D)]; D = ShTablesRR .val[t_acc][(D)]; }; break;
			case (0x18)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(E)]; E = ShTablesRR .val[t_acc][(E)]; }; break;
			case (0x18)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(H)]; H = ShTablesRR .val[t_acc][(H)]; }; break;
			case (0x18)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(L)]; L = ShTablesRR .val[t_acc][(L)]; }; break;
			case (0x18)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesRR .flag[t_acc][(A)]; A = ShTablesRR .val[t_acc][(A)]; }; break;
			case (0x18)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesRR .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesRR .val[t_acc][t_vol]); }; break;
			case (0x20)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(B)]; B = ShTablesSLA .val[t_acc][(B)]; }; break;
			case (0x20)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(C)]; C = ShTablesSLA .val[t_acc][(C)]; }; break;
			case (0x20)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(D)]; D = ShTablesSLA .val[t_acc][(D)]; }; break;
			case (0x20)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(E)]; E = ShTablesSLA .val[t_acc][(E)]; }; break;
			case (0x20)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(H)]; H = ShTablesSLA .val[t_acc][(H)]; }; break;
			case (0x20)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(L)]; L = ShTablesSLA .val[t_acc][(L)]; }; break;
			case (0x20)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSLA .flag[t_acc][(A)]; A = ShTablesSLA .val[t_acc][(A)]; }; break;
			case (0x20)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesSLA .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesSLA .val[t_acc][t_vol]); }; break;
			case (0x28)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(B)]; B = ShTablesSRA .val[t_acc][(B)]; }; break;
			case (0x28)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(C)]; C = ShTablesSRA .val[t_acc][(C)]; }; break;
			case (0x28)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(D)]; D = ShTablesSRA .val[t_acc][(D)]; }; break;
			case (0x28)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(E)]; E = ShTablesSRA .val[t_acc][(E)]; }; break;
			case (0x28)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(H)]; H = ShTablesSRA .val[t_acc][(H)]; }; break;
			case (0x28)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(L)]; L = ShTablesSRA .val[t_acc][(L)]; }; break;
			case (0x28)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRA .flag[t_acc][(A)]; A = ShTablesSRA .val[t_acc][(A)]; }; break;
			case (0x28)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesSRA .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesSRA .val[t_acc][t_vol]); }; break;
			case (0x38)+0: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(B)]; B = ShTablesSRL .val[t_acc][(B)]; }; break;
			case (0x38)+1: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(C)]; C = ShTablesSRL .val[t_acc][(C)]; }; break;
			case (0x38)+2: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(D)]; D = ShTablesSRL .val[t_acc][(D)]; }; break;
			case (0x38)+3: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(E)]; E = ShTablesSRL .val[t_acc][(E)]; }; break;
			case (0x38)+4: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(H)]; H = ShTablesSRL .val[t_acc][(H)]; }; break;
			case (0x38)+5: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(L)]; L = ShTablesSRL .val[t_acc][(L)]; }; break;
			case (0x38)+7: { t_acc = ((F&CF_Mask)>>4); F = ShTablesSRL .flag[t_acc][(A)]; A = ShTablesSRL .val[t_acc][(A)]; }; break;
			case (0x38)+6: { t_acc = ((F&CF_Mask)>>4); t_vol = read(((H<<8)|L)); F = ShTablesSRL .flag[t_acc][t_vol]; write(((H<<8)|L), ShTablesSRL .val[t_acc][t_vol]); }; break;
			case 0x30+0: B = Tables.swap[B]; F = ( (B) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+1: C = Tables.swap[C]; F = ( (C) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+2: D = Tables.swap[D]; F = ( (D) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+3: E = Tables.swap[E]; F = ( (E) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+4: H = Tables.swap[H]; F = ( (H) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+5: L = Tables.swap[L]; F = ( (L) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+7: A = Tables.swap[A]; F = ( (A) != 0 ? 0 : ZF_Mask ); break;
			case 0x30+6: { t_acc = Tables.swap[read(((H<<8)|L))]; write(((H<<8)|L), t_acc); F = ( (t_acc) != 0 ? 0 : ZF_Mask ); }; break;
			default:
				System.out.printf("UNKNOWN PREFIX INSTRUCTION: $%02x (" + op + ")\n" , op);
				localPC -= 2;
				return 0;
			}
			break;
		default:



		{
			System.out.printf("UNKNOWN INSTRUCTION: $%02x (" + op + ")\n" , op);
			localPC -= 1;
			return 0;
		}
		}
		return cycles;
	}

	static long lastns = 0;
	static long lastuf = 0;
	static int samplesLeft = 0;

	public static void elapseTime(int cycles) {
		TotalCycleCount += cycles;
	}

	public static boolean keeprunning;

	public static void runlooprun() {
		int cycles;
		do {
			if (TotalCycleCount >= NextEventCycleCount)
				handleEvents();

			if (playbackHistoryIndex == -1) {
				if (KeyStatus != GUIKeyStatus) {
					keyBounce = 1000;
					handleKeyBounceEvent();
				}
				if (KeyStatus != GUIKeyStatus || lastKeyChange > 0x3fffffff) {
					KeyStatus = GUIKeyStatus;
					keyHistory.add(lastKeyChange);
					keyHistory.add(KeyStatus);
					lastKeyChange = 0;
				}
			} else if (playbackHistoryIndex < keyHistory.size()) {
				int d = keyHistory.get(playbackHistoryIndex);
				if (d <= lastKeyChange) {

					lastKeyChange = 0;
					++playbackHistoryIndex;
					d = keyHistory.get(playbackHistoryIndex++);
					if (KeyStatus != d) {
						KeyStatus = d;
						keyBounce = 1000;
						handleKeyBounceEvent();
					}
				}
			} else {
				playbackHistoryIndex = -1;
				keyHistoryEnabled = true;
			}

			cycles = 4*execute();

			lastKeyChange += cycles;







			TotalCycleCount += cycles;

			if (cycles > 0) {
				if (doublespeed) {
					AC.render(cycles>>1);
				}
				else {
					AC.render(cycles);
				}
			}



			if (LinkCableStatus != 0) {
				LINKcntdwn -= cycles;
				if (LINKcntdwn < 0) {
					LINKcntdwn += (4096 / LINKmulti);


					try {
						int lstatus = IOP[0x02] | (IOP[0x01] << 8);
						int rstatus = -1;

						LINKbuf[LINKind++] = lstatus;

						lstatus |= RemoteKeyStatus << 16;

						LinkCableOut.writeInt(lstatus);
						LinkCableOut.flush();
						rstatus = LinkCableIn.readInt();

						if (useRemoteKeys) {
							int rkeys = (rstatus >> 16) & 0xff;
							if (GUIKeyStatus != rkeys) {
								GUIKeyStatus = rkeys;
							}
						}

						if (LINKind > LINKdelay) LINKind = 0;
						lstatus = LINKbuf[LINKind];

						int bstatus = (rstatus & 0xff) | ((lstatus & 0xff) << 8);
						bstatus &= 0x8181;

						switch (bstatus) {
						case 0x8080: case 0x8001: case 0x8000: {


						}; break;
						case 0x8101: case 0x8100: {


							if (++LINKtimeout > LINKdelay) {
								LINKtimeout = 0;
								IOP[0x01] = 0xff;
								IOP[0x02] &= ~0x80;
								triggerInterrupt(3);
							}
						}; break;
						case 0x8181: {

							System.out.println("Link: clock conflict");
						}; break;
						case 0x8180:
						case 0x8081: {

							IOP[0x01] = (rstatus >> 8) & 0xff;
							IOP[0x02] &= ~0x80;
							triggerInterrupt(3);
							LINKtimeout = 0;
						}; break;
						default: {


						}
						}

					}
					catch (IOException e) {
						System.out.println("Link exception");
						severLink();
					}

				}
			}


		} while (keeprunning && cycles > 0);
		if (cycles == 0) { throw new RuntimeException(); };
	};

	public static void runloop() {
		keeprunning = true;
		runlooprun();
	};

	public static void runlooponce() {
		keeprunning = false;
		runlooprun();
	};


	static private int PORTNO = 1989;
	static public UUID player;
	static public int serverN =-1;
	static public int clientN =-1;
	static private int LINKmulti = 1;
	static private int LINKdelay = 0;

	static private int LINKcntdwn = 0;
	static private int[] LINKbuf = new int[8];
	static private int LINKind = 0;
	static private int LINKtimeout = 0;

	static protected int LinkCableStatus = 0;



	static void setDelay(int ndelay) throws IOException {
		for (int i = 0; i < LINKdelay; ++i)
			LinkCableIn.readInt();
		LINKdelay = ndelay;
		for (int i = 0; i < LINKdelay; ++i)
			LinkCableOut.writeInt(0);
		LINKmulti = LINKdelay + 1;
	}

	static protected ServerSocket LinkCablesrvr = null;

	static protected Socket LinkCablesktOut = null;
	static protected Socket LinkCablesktIn = null;

	static protected DataInputStream LinkCableIn = null;
	static protected DataOutputStream LinkCableOut = null;

	static protected boolean LinkCableSendReceive=false;


	public static final void severLink() {
		try {
			if(LinkCablesrvr!=null) {
				LinkCablesrvr.close();
				LinkCablesrvr=null;
			}
			if(LinkCablesktOut!=null) {
				LinkCablesktOut.close();
				LinkCablesktOut=null;
			}
			if(LinkCablesktIn!=null) {
				LinkCablesktIn.close();
				LinkCablesktIn=null;
			}
			if(LinkCableIn!=null) {
				LinkCableIn.close();
				LinkCableIn=null;
			}
			if(LinkCableOut!=null) {
				LinkCableOut.close();
				LinkCableOut=null;
			}
		}
		catch(IOException e) {
			System.out.println("Error while closing socket(s)");
			e.printStackTrace();
		}
		finally {
			LinkCableStatus=0;
		}
	}
	public static boolean isConnected() {
		return LinkCableStatus!=0;
	}
	public static final void serveLink(Player p) {
		if(LinkCableStatus==0) {
			Bukkit.getScheduler().runTaskAsynchronously(swinggui.getInstance(), () -> {
				try {
					player = p.getUniqueId();
					PORTNO = 30000+Bukkit.getServer().getPort()-25580;
					serverN = Bukkit.getServer().getPort()-25580;
					LinkCablesrvr = new ServerSocket(PORTNO);
					LinkCablesktOut = LinkCablesrvr.accept();
					System.out.println("Connection established");
					clientN =LinkCablesktOut.getPort()-30000;
					LinkCablesktOut.setTcpNoDelay(true);
					LinkCableIn = new DataInputStream(LinkCablesktOut.getInputStream());
					LinkCableOut = new DataOutputStream(LinkCablesktOut.getOutputStream());
					LinkCableStatus=1;
					setDelay(0);
				} catch (IOException ex) {
					p.sendMessage("An error occured while attempting to host a server!");
				}
			});
		}
		else p.sendMessage("Unable to connect to host server while connected to another!");
	}

	public static final void clientLink(int server, Player p) {
		if(LinkCableStatus==0) {
			Bukkit.getScheduler().runTaskAsynchronously(swinggui.getInstance(), () -> {
				try {
					PORTNO = 30000+server;
					serverN  = server;
					clientN = Bukkit.getServer().getPort()-25580+30000;
					LinkCablesktIn = new Socket(InetAddress.getByName("127.0.0.1"), PORTNO);
					LinkCablesktIn.setTcpNoDelay(true);
					LinkCableIn = new DataInputStream(LinkCablesktIn.getInputStream());
					LinkCableOut = new DataOutputStream(LinkCablesktIn.getOutputStream());
					LinkCableStatus=2;
					setDelay(0);
				} catch (IOException ex) {
					p.sendMessage("An error occurred while attempting to connect to a server!!");
					LinkCableStatus=0;
				}
			});
		}
		else 
			p.sendMessage("Unable to connect to another server!");
	}

	public static boolean isServer() {
		return LinkCableStatus==1;
	}

}
