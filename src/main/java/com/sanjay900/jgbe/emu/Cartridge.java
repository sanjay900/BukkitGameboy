package com.sanjay900.jgbe.emu;
import java.io.*;

public final class Cartridge {

    private static final int MEMMAP_SIZE = 0x1000;

    private static final int MAX_ROM_MM = 512 << 2;
    private static final int MAX_RAM_MM = 32 << 1;

    protected int[][] MM_ROM = new int[MAX_ROM_MM][];
    protected int[][] MM_RAM = new int[MAX_RAM_MM][];
    protected int[] BIOS_ROM = new int[0x100];

    public void loadBios(String filename) {
        for (int i = 0; i < 0x100; ++i) BIOS_ROM[i] = (0);
        new Bios(filename, BIOS_ROM);
    }

    protected int rom_mm_size;
    protected int ram_mm_size;

    private String err_msg;
    private String title;

    protected int MBC;
    private boolean isGBC;

    private boolean ram_enabled = false;
    private boolean RTCRegisterEnabled = false;
    private int RomRamModeSelect = 0;
    protected int CurrentROMBank = 1;
    protected int CurrentRAMBank = 0;
    private int CurrentRTCRegister = 0;
    private boolean titleOnly;
    DataInputStream distream;

    private void handleIOException(IOException e) {
        System.out.println("error loading cartridge from file!: " + e.toString());
        status = STATUS_FATAL_ERROR;
        err_msg = e.getMessage();
        if (e instanceof EOFException) {
            err_msg = "This ROM image should have " + (rom_mm_size >> 2) + " banks of data,\nbut not all banks appear to be present in the ROM image.\nJGBE will try to emulate the ROM regardless, but beware\nthat this may cause the ROM to lockup or crash.";
            status = STATUS_NONFATAL_ERROR;
        }
        if (err_msg == null) err_msg = "Java Error messages are useless! (UNKNOWN_ERROR)";
    }

    public Cartridge(String file_name, boolean titleOnly) {
        this.titleOnly = titleOnly;
        distream = null;
        try {
            distream = FHandler.getDataInputStream(file_name);
            loadFromStream();
        } catch (java.io.IOException e) {
            handleIOException(e);
        }


    }

    protected void stateSaveLoad(boolean save, DataOutputStream dostream, DataInputStream distream) throws IOException {
        boolean isnull = false;
        for (int t = 0; t < MAX_RAM_MM; ++t) {
            if ((save))
                isnull = (MM_RAM[t] == null);
            {
                if ((save)) dostream.writeBoolean(isnull);
                else isnull = distream.readBoolean();
            }
            if (!isnull) {
                for (int sl_i = 0; sl_i < (MEMMAP_SIZE); ++sl_i) {
                    if ((save)) dostream.writeByte(((MM_RAM[t][sl_i]) & 0xff));
                    else MM_RAM[t][sl_i] = (distream.readUnsignedByte());
                }
            } else if ((!save))
                MM_RAM[t] = null;
        }

        {
            for (int sl_i = 0; sl_i < (0x100); ++sl_i) {
                if ((save)) dostream.writeByte(((BIOS_ROM[sl_i]) & 0xff));
                else BIOS_ROM[sl_i] = (distream.readUnsignedByte());
            }
        }


        {
            if ((save)) dostream.writeBoolean(ram_enabled);
            else ram_enabled = distream.readBoolean();
        }
        {
            if ((save)) dostream.writeBoolean(RTCRegisterEnabled);
            else RTCRegisterEnabled = distream.readBoolean();
        }
        {
            if ((save)) dostream.writeInt(RomRamModeSelect);
            else RomRamModeSelect = distream.readInt();
        }
        {
            if ((save)) dostream.writeInt(CurrentROMBank);
            else CurrentROMBank = distream.readInt();
        }
        {
            if ((save)) dostream.writeInt(CurrentRAMBank);
            else CurrentRAMBank = distream.readInt();
        }
        {
            if ((save)) dostream.writeInt(CurrentRTCRegister);
            else CurrentRTCRegister = distream.readInt();
        }
    }

    public static final int STATUS_OK = 0;
    public static final int STATUS_NONFATAL_ERROR = STATUS_OK + 1;
    public static final int STATUS_FATAL_ERROR = STATUS_NONFATAL_ERROR + 1;
    private int status = STATUS_OK;

    public int getStatus(String[] s) {


        if (s.length > 0) {
            s[0] = err_msg;
        }
        return status;
    }

    public String getTitle() {
        return title;
    }

    private void loadFromStream() throws java.io.IOException {
        MM_ROM[0] = new int[MEMMAP_SIZE];
        for (int i = 0; i < MEMMAP_SIZE; ++i)
            MM_ROM[0][i] = (distream.readUnsignedByte());

        switch ((MM_ROM[0][0x0147])) {
            case 0x00:
            case 0x09:
            case 0x08:
                MBC = 0;
                break;
            case 0x01:
            case 0x03:
            case 0x02:
                MBC = 1;
                break;
            case 0x05:
            case 0x06:
                MBC = 2;
                break;
            case 0x0b:
            case 0x0d:
            case 0x0c:
                MBC = -1;
                break;
            case 0x0f:
            case 0x13:
            case 0x12:
            case 0x11:
            case 0x10:
                MBC = 3;
                break;
            case 0x15:
            case 0x17:
            case 0x16:
                MBC = 4;
                break;
            case 0x19:
            case 0x1e:
            case 0x1d:
            case 0x1c:
            case 0x1b:
            case 0x1a:
                MBC = 5;
                break;
            case 0xfc:
                MBC = -2;
                break;
            case 0xfd:
                MBC = -5;
                break;
            case 0xfe:
                MBC = -42;
                break;
            case 0xff:
                MBC = -99;
                break;
            default:
                MBC = -666;
                throw new java.io.IOException("unknown MBC type");
        }
        int gbcreg = MM_ROM[0][0x0143];
        isGBC = gbcreg == 0xc0 || gbcreg == 0x80;


        rom_mm_size = 0;
        switch ((MM_ROM[0][0x0148])) {
            case 0x00:
                rom_mm_size = 2 << 2;
                break;
            case 0x01:
                rom_mm_size = 4 << 2;
                break;
            case 0x02:
                rom_mm_size = 8 << 2;
                break;
            case 0x03:
                rom_mm_size = 16 << 2;
                break;
            case 0x04:
                rom_mm_size = 32 << 2;
                break;
            case 0x05:
                rom_mm_size = 64 << 2;
                break;
            case 0x06:
                rom_mm_size = 128 << 2;
                break;
            case 0x07:
                rom_mm_size = 256 << 2;
                break;
            case 0x08:
                rom_mm_size = 512 << 2;
                break;
            case 0x52:
                rom_mm_size = 72 << 2;
                break;
            case 0x53:
                rom_mm_size = 80 << 2;
                break;
            case 0x54:
                rom_mm_size = 96 << 2;
                break;
            default:
                rom_mm_size = 1;
        }


        ram_mm_size = 0;
        switch ((MM_ROM[0][0x0149])) {
            case 0x00:
                break;
            case 0x01:
                ram_mm_size = 1;
                break;
            case 0x02:
                ram_mm_size = 2;
                break;
            case 0x03:
                ram_mm_size = 8;
                break;
            default:
                ram_mm_size = 32;
        }

        if ((MBC == 2) && (ram_mm_size == 0)) ram_mm_size = 1;

        title = "";
        int title_len = (((MM_ROM[0][0x0143])) == 0) ? 16 : 15;
        for (int i = 0; i < title_len; ++i) {
            if ((MM_ROM[0][0x0134 + i]) == 0) break;
            title += (char) (MM_ROM[0][0x0134 + i]);
        }
        if (titleOnly) {
            distream.close();
            distream = null;
            return;
        }

        for (int i = 1; i < rom_mm_size; ++i) {
            MM_ROM[i] = new int[MEMMAP_SIZE];
        }
        for (int i = 1; i < rom_mm_size; ++i) {
            for (int j = 0; j < MEMMAP_SIZE; ++j) {
                MM_ROM[i][j] = (distream.readUnsignedByte());
            }
        }

        distream.close();
        distream = null;


        for (int i = 0; i < ram_mm_size; ++i)
            MM_RAM[i] = new int[MEMMAP_SIZE];
    }

    public int read(int index) {
        switch (MBC) {
            case 1:
            case 2:
                if (ram_enabled) {
                    if ((index >= 0xa000) && (index < 0xa200)) {
                        return (MM_RAM[0][index & 0xfff]) & 0xf;
                    }
                } else {
                    System.out.println("Warning: Reading from disabled RAM!");
                    return 0;
                }
                System.out.printf("Warning: Reading from bogus address: $%04x\n", index);
                return 0xff;
            case 3:

                System.out.printf("Error: not using memmap, or reading from cartridge with a noncartridge address $%04x\n", index);
                System.out.printf("CurRombank: %d CurrentRAMBank: %d\n", CurrentROMBank, CurrentRAMBank);
                return 0xff;
            case 5:

                System.out.println("Error: not using memmap, or reading from cartridge with a non cartridge address!");
                return 0xff;
            default:
                System.out.println("Error: Cartridge memory bank controller type #" + MBC + " is not implemented!");
                return 0xff;
        }
    }

    public void write(int index, int value) {
        switch (MBC) {
            case 1:

                if ((index >= 0x0000) && (index < 0x2000)) {
                    ram_enabled = (value & 0x0f) == 0x0A;

                } else if (index < 0x4000) {
                    int i = Math.max(1, value & 0x1f);

                    CurrentROMBank &= ~0x1f;
                    CurrentROMBank |= i;
                    CurrentROMBank %= (rom_mm_size >> 2);

                } else if (index < 0x6000) {
                    if (RomRamModeSelect == 0) {
                        CurrentROMBank = (CurrentROMBank & 0x1f) | ((value & 0x03) << 5);
                        CurrentROMBank %= (rom_mm_size >> 2);

                    } else {
                        CurrentRAMBank = value & 0x03;
                        if (ram_mm_size == 0 && ((value * 64) | 64) < rom_mm_size) {
                            System.out.println("WARNING! 'Bomberman Collection (J) [S]' hack'" + value);
                            System.out.printf("setting rom banks 0-15 to banks %d-%d\n", value * 16, (value * 16) + 15);
                            for (int i = 0; i < 64; ++i)
                                MM_ROM[i] = MM_ROM[(value * 64) | i];
                        }
                    }
                } else if (index < 0x8000) {
                    RomRamModeSelect = value & 1;
                }
                break;
            case 2:


                if ((0x0000 <= index) && (index <= 0x1FFF)) {
                    if ((index & (1 << 8)) == 0) {
                        ram_enabled = !ram_enabled;
                    }
                } else if ((0x2000 <= index) && (index <= 0x3FFFF)) {
                    if ((index & (1 << 8)) != 0) {
                        value &= 0xf;
                        CurrentROMBank = (Math.max((value), (1)));
                    }
                }
                break;
            case 3:

                if ((index >= 0) && (index < 0x2000)) {
                    ram_enabled = (value & 0x0f) == 0x0A;
                }
                if ((index >= 0x2000) && (index < 0x4000)) {
                    CurrentROMBank = Math.max(value & 0x7f, 1);
                }
                if ((index >= 0x4000) && (index < 0x6000)) {
                    if ((value >= 0) && (value < 0x4)) {
                        RTCRegisterEnabled = false;
                        CurrentRAMBank = value;
                    }
                    if ((value >= 0x08) && (value < 0x0c)) {
                        RTCRegisterEnabled = true;
                        CurrentRTCRegister = value - 0x08;
                    }
                }
                if ((index >= 0xa000) && (index < 0xc000)) {
                    if (RTCRegisterEnabled) {
                        System.out.println("TODO: Cartridge.write(): writing to RAM in RTC mode");
                    } else {
                        System.out.println("Error: not using memmap!");
                    }
                }
                if (((index >= 0x8000) && (index < 0xa000)) || ((index > 0xc000)))
                    System.out.printf("WARNING: Cartridge.write(): Unsupported address for write ($%04x)\n", index);
                break;


            case 5:

                if ((index >= 0) && (index < 0x2000)) {
                    ram_enabled = (value & 0x0f) == 0x0A;
                }
                if ((index >= 0x2000) && (index < 0x3000)) {
                    CurrentROMBank &= 0x100;
                    CurrentROMBank |= value;
                }
                if ((index >= 0x3000) && (index < 0x4000)) {
                    CurrentROMBank &= 0xff;
                    CurrentROMBank |= (value & 1) << 8;
                }
                if ((index >= 0x4000) && (index < 0x6000)) {
                    if (value < 0x10)
                        CurrentRAMBank = value;
                }
                if ((index >= 0xa000) && (index < 0xc000)) {
                    System.out.println("Error: not using memmap!");
                }
                if (((index >= 0x6000) && (index < 0xa000)) || ((index > 0xc000)))
                    System.out.printf("WARNING: Cartridge.write(): Unsupported address for write ($%04x)\n", index);
                break;
            default:
                System.out.println("ERROR: Cartridge.write(): Cartridge memory bank controller type #" + MBC + " is not implemented");
        }
    }

    public boolean isGBC() {
        return isGBC;
    }
}
