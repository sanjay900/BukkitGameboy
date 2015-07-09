package com.sanjay900.jgbe.converters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;
import com.sanjay900.jgbe.emu.CPU;

public class PokemonConverter extends Converter{
	String[] pokedex= {"#000 Missingno", "#001 Bulbasaur", "#002 Ivysaur", "#003 Venusaur", "#004 Charmander", "#005 Charmeleon", "#006 Charizard", "#007 Squirtle", "#008 Wartortle", "#009 Blastoise", "#010 Caterpie", "#011 Metapod", "#012 Butterfree", "#013 Weedle", "#014 Kakuna", "#015 Beedrill", "#016 Pidgey", "#017 Pidgeotto", "#018 Pidgeot", "#019 Rattata", "#020 Raticate", "#021 Spearow", "#022 Fearow", "#023 Ekans", "#024 Arbok", "#025 Pikachu", "#026 Raichu", "#027 Sandshrew", "#028 Sandslash", "#029 Nidoranh", "#030 Nidorina", "#031 Nidoqueen", "#032 Nidoranm", "#033 Nidorino", "#034 Nidoking", "#035 Clefairy", "#036 Clefable", "#037 Vulpix", "#038 Ninetales", "#039 Jigglypuff", "#040 Wigglytuff", "#041 Zubat", "#042 Golbat", "#043 Oddish", "#044 Gloom", "#045 Vileplume", "#046 Paras", "#047 Parasect", "#048 Venonat", "#049 Venomoth", "#050 Diglett", "#051 Dugtrio", "#052 Meowth", "#053 Persian", "#054 Psyduck", "#055 Golduck", "#056 Mankey", "#057 Primeape", "#058 Growlithe", "#059 Arcanine", "#060 Poliwag", "#061 Poliwhirl", "#062 Poliwrath", "#063 Abra", "#064 Kadabra", "#065 Alakazam", "#066 Machop", "#067 Machoke", "#068 Machamp", "#069 Bellsprout", "#070 Weepinbell", "#071 Victreebel", "#072 Tentacool", "#073 Tentacruel", "#074 Geodude", "#075 Graveler", "#076 Golem", "#077 Ponyta", "#078 Rapidash", "#079 Slowpoke", "#080 Slowbro", "#081 Magnemite", "#082 Magneton", "#083 Farfetch'd", "#084 Doduo", "#085 Dodrio", "#086 Seel", "#087 Dewgong", "#088 Grimer", "#089 Muk", "#090 Shellder", "#091 Cloyster", "#092 Gastly", "#093 Haunter", "#094 Gengar", "#095 Onix", "#096 Drowzee", "#097 Hypno", "#098 Krabby", "#099 Kingler", "#100 Voltorb", "#101 Electrode", "#102 Exeggcute", "#103 Exeggutor", "#104 Cubone", "#105 Marowak", "#106 Hitmonlee", "#107 Hitmonchan", "#108 Lickitung", "#109 Koffing", "#110 Weezing", "#111 Rhyhorn", "#112 Rhydon", "#113 Chansey", "#163 Tangela", "#115 Kangaskhan", "#116 Horsea", "#117 Seadra", "#118 Goldeen", "#119 Seaking", "#120 Staryu", "#121 Starmie", "#122 Mr.Mime", "#123 Scyther", "#124 Jynx", "#125 Electabuzz", "#126 Magmar", "#127 Pinsir", "#128 Tauros", "#129 Magikarp", "#130 Gyarados", "#131 Lapras", "#132 Ditto", "#133 Eevee", "#134 Vaporeon", "#135 Jolteon", "#136 Flareon", "#137 Porygon", "#138 Omanyte", "#139 Omastar", "#140 Kabuto", "#141 Kabutops", "#142 Aerodactyl", "#143 Snorlax", "#144 Articuno", "#145 Zapdos", "#146 Moltres", "#147 Dratini", "#148 Dragonair", "#149 Dragonite", "#150 Mewtwo", "#151 Mew", "#152", "#153", "#154", "#155", "#156", "#163", "#158", "#159", "#160", "#161", "#162", "#163", "#164", "#165", "#166", "#167", "#168", "#169", "#170", "#171", "#172", "#173", "#174", "#175", "#176", "#177", "#178", "#179", "#180", "#181", "#182", "#183", "#184", "#185", "#186", "#187", "#188", "#189", "#190", "#191", "#192", "#193", "#194", "#195", "#196", "#197", "#198", "#199", "#200", "#201", "#202", "#203", "#204", "#205", "#206", "#207", "#208", "#209", "#210", "#211", "#212", "#213", "#214", "#215", "#216", "#217", "#218", "#219", "#220", "#221", "#222", "#223", "#224", "#225", "#226", "#227", "#228", "#229", "#230", "#231", "#232", "#233", "#234", "#235", "#236", "#237", "#238", "#239", "#240", "#241", "#242", "#243", "#244", "#245", "#246", "#247", "#248", "#249", "#250", "#251", "#252", "#253", "#254", "#255"};
	String[] pokemon= {" (0)", "Rhydon (1)", "Kangaskhan (2)", "Nidoranm (3)", "Clefairy (4)", "Spearow (5)", "Voltorb (6)", "Nidoking (7)", "Slowbro (8)", "Ivysaur (9)", "Exeggutor (10)", "Lickitung (11)", "Exeggcute (12)", "Grimer (13)", "Gengar (14)", "Nidoranh (15)", "Nidoqueen (16)", "Cubone (17)", "Rhyhorn (18)", "Lapras (19)", "Arcanine (20)", "Mew (21)", "Gyarados (22)", "Shellder (23)", "Tentacool (24)", "Gastly (25)", "Scyther (26)", "Staryu (27)", "Blastoise (28)", "Pinsir (29)", "Tangela (30)", "Missingno (31)", "Missingno (32)", "Growlithe (33)", "Onix (34)", "Fearow (35)", "Pidgey (36)", "Slowpoke (37)", "Kadabra (38)", "Graveler (39)", "Chansey (40)", "Machoke (41)", "Mr.Mime (42)", "Hitmonlee (43)", "Hitmonchan (44)", "Arbok (45)", "Parasect (46)", "Psyduck (47)", "Drowzee (48)", "Golem (49)", "Missingno (50)", "Magmar (51)", "Missingno (52)", "Electabuzz (53)", "Magneton (54)", "Koffing (55)", "Missingno (56)", "Mankey (57)", "Seel (58)", "Diglett (59)", "Tauros (60)", "Missingno (61)", "Missingno (62)", "Missingno (63)", "Farfetch'd (64)", "Venonat (65)", "Dragonite (66)", "Missingno (67)", "Missingno (68)", "Missingno (69)", "Doduo (70)", "Poliwag (71)", "Jynx (72)", "Moltres (73)", "Articuno (74)", "Zapdos (75)", "Ditto (76)", "Meowth (77)", "Krabby (78)", "Missingno (79)", "Missingno (80)", "Missingno (81)", "Vulpix (82)", "Ninetales (83)", "Pikachu (84)", "Raichu (85)", "Missingno (86)", "Missingno (87)", "Dratini (88)", "Dragonair (89)", "Kabuto (90)", "Kabutops (91)", "Horsea (92)", "Seadra (93)", "Missingno (94)", "Missingno (95)", "Sandshrew (96)", "Sandslash (97)", "Omanyte (98)", "Omastar (99)", "Jigglypuff (100)", "Wigglytuff (101)", "Eevee (102)", "Flareon (103)", "Jolteon (104)", "Vaporeon (105)", "Machop (106)", "Zubat (107)", "Ekans (108)", "Paras (109)", "Poliwhirl (110)", "Poliwrath (111)", "Weedle (112)", "Kakuna (113)", "Beedrill (163)", "Missingno (115)", "Dodrio (116)", "Primeape (117)", "Dugtrio (118)", "Venomoth (119)", "Dewgong (120)", "Missingno (121)", "Missingno (122)", "Caterpie (123)", "Metapod (124)", "Butterfree (125)", "Machamp (126)", "Missingno (127)", "Golduck (128)", "Hypno (129)", "Golbat (130)", "Mewtwo (131)", "Snorlax (132)", "Magikarp (133)", "Missingno (134)", "Missingno (135)", "Muk (136)", "Missingno (137)", "Kingler (138)", "Cloyster (139)", "Missingno (140)", "Electrode (141)", "Clefable (142)", "Weezing (143)", "Persian (144)", "Marowak (145)", "Missingno (146)", "Haunter (147)", "Abra (148)", "Alakazam (149)", "Pidgeotto (150)", "Pidgeot (151)", "Starmie (152)", "Bulbasaur (153)", "Venusaur (154)", "Tentacruel (155)", "Missingno (156)", "Goldeen (163)", "Seaking (158)", "Missingno (159)", "Missingno (160)", "Missingno (161)", "Missingno (162)", "Ponyta (163)", "Rapidash (164)", "Rattata (165)", "Raticate (166)", "Nidorino (167)", "Nidorina (168)", "Geodude (169)", "Porygon (170)", "Aerodactyl (171)", "Missingno (172)", "Magnemite (173)", "Missingno (174)", "Missingno (175)", "Charmander (176)", "Squirtle (177)", "Charmeleon (178)", "Wartortle (179)", "Charizard (180)", "Missingno (181)", "Missingno (182)", "Missingno (183)", "Missingno (184)", "Oddish (185)", "Gloom (186)", "Vileplume (187)", "Bellsprout (188)", "Weepinbell (189)", "Victreebel (190)", "~191", "~192", "~193", "~194", "~195", "~196", "~197", "~198", "~199", "~200", "~201", "~202", "~203", "~204", "~205", "~206", "~207", "~208", "~209", "~210", "~211", "~212", "~213", "~214", "~215", "~216", "~217", "~218", "~219", "~220", "~221", "~222", "~223", "~224", "~225", "~226", "~227", "~228", "~229", "~230", "~231", "~232", "~233", "~234", "~235", "~236", "~237", "~238", "~239", "~240", "~241", "~242", "~243", "~244", "~245", "~246", "~247", "~248", "~249", "~250", "~251", "~252", "~253", "~254", "-- No Pokemon -- (255)"};
	HashMap<Integer,ByteBuffer> pkm2 = new HashMap<>();

	Objective o;
	private String[] table;
	public PokemonConverter(GameboyPlayer gp) {
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Name:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Rival:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Money:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Badges:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Play Time:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Owned Pokemon:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Seen Pokemon:");
		t.addEntry(t.getName());
		table = new String[256];
		for (int x =0; x < 26;x++) {
			table[128+x]=String.valueOf((char)(65+x));
			table[128+32+x]=String.valueOf((char)(65+32+x));
		}
		for (int x =0; x < 10;x++) {
			table[246+x]=String.valueOf(x);
		}
		table[154]="(";
		table[155]=")";
		table[156]=":";
		table[157]=";";
		table[158]="[";
		table[159]="]";
		table[224]="'";
		table[225]="PK";
		table[226]="MN";
		table[227]="-";
		table[228]="'r";
		table[229]="'m";
		table[230]="?";
		table[231]="!";
		table[232]=".";
		table[242]=".";
		table[243]="/";
		table[244]=",";
		for (int i = 0xD000; i< 0xDFFF;i++) {
			writtenMemory(i);
		}
	}
	public int getCapacity() {
		return CPU.read(0xD163);
	}
	private int[] pkm(int off_hex,int off_otname,int off_name,int off_data) {
		byte[] pkm = new byte[67];
		ByteBuffer bb = ByteBuffer.wrap(pkm);
		bb.put((byte) CPU.read(off_hex));
		for (int i = 0; i < 11; i++) {
			bb.put((byte) CPU.read(off_otname+i));
		}
		for (int i = 0; i < 11; i++) {
			bb.put((byte) CPU.read(off_name+i));
		}
		for (int i = 0; i < 44; i++) {
			bb.put((byte) CPU.read(off_data+i));
		}
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		return convertToIntArray(bb.array());
	}
	public static int[] convertToIntArray(byte[] input)
	{
	    int[] ret = new int[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        ret[i] = input[i];
	    }
	    return ret;
	}
	public Object get(int p, dataType var) {
		int[] pkm = pkm(0xD164+p,0xD273+11*p,0xD2B5+11*p,0xD16B+44*p);
		switch (var) {
		case sprite:
			return pkm[0];
		case num:
			return pkm[23];
		case otname:
			return decode(Arrays.copyOfRange(pkm, 1, 11));
		case name:
			return decode(Arrays.copyOfRange(pkm, 12, 22));
		case hp:
			return getShort(pkm[25],pkm[24]);
		case level:
			return pkm[26];
		case asleep:
			return pkm[27] & 7;
		case poisoned:
			return pkm[27] & 8;
		case burned:
			return pkm[27] & 16;
		case frozen:
			return pkm[27] & 32;
		case paralyzed:
			return pkm[27] & 64;
		case ok:
			return pkm[27] & 127;
		case type1:
			return pkm[28];
		case type2:
			return pkm[29];
		case catchrate:
			return pkm[30];
		case move1:
			return pkm[31];
		case move2:
			return pkm[32];
		case move3:
			return pkm[33];
		case move4:
			return pkm[34];
		case otnum:
			return getShort(pkm[36],pkm[35]);
		case exp:
			return pkm[39] << 16 | pkm[38] << 8 | pkm[37];
		case maxhpev:
			return getShort(pkm[41],pkm[40]);
		case attackev:
			return getShort(pkm[43],pkm[42]);
		case defenseev:
			return getShort(pkm[45],pkm[44]);
		case speedev:
			return getShort(pkm[47],pkm[46]);
		case specialev:
			return getShort(pkm[49],pkm[48]);
		case attackiv:
			return pkm[50] >> 4;
		case defenseiv:
			return pkm[50] & 15;
		case speediv:
			return pkm[51] >> 4;
		case specialiv:
			return pkm[51] & 15;
		case move1pp:
			return pkm[52]&63;
		case move1ppup:
			return (pkm[52])&192 >> 6;
		case move2pp:
			return pkm[53]&63;
		case move2ppup:
			return (pkm[53])&192 >> 6;
		case move3pp:
			return pkm[54]&63;
		case move3ppup:
			return (pkm[54])&192 >> 6;
		case move4pp:
			return pkm[55]&63;
		case move4ppup:
			return (pkm[55])&192 >> 6;
		case curlevel:
			return pkm[56];
		case maxhp:
			return getShort(pkm[58],pkm[57]);
		case attack:
			return getShort(pkm[60],pkm[59]);
		case defense:
			return getShort(pkm[62],pkm[61]);
		case speed:
			return getShort(pkm[64],pkm[63]);
		case special:
			return getShort(pkm[66],pkm[65]);
		default:
			return null;
		}

	}
	public String decode(int[] s) {

		String decoded = "";
		for (int b: s) {
			int dec = b;
			if (dec == 80 ||dec == 336 || table.length <= dec) {
				return decoded;
			} else if (table[dec] != null && table[dec] != "")
				decoded+=table[dec];
		}
		return decoded;
	}
	
	public String getSpecies(int index) {
		return pokemon[(int)get(index,dataType.num)];
	}
	public enum dataType {
		sprite,num,otname,name,hp,level,asleep,poisoned,burned,frozen,paralyzed,ok,type1,type2,catchrate,move1,move2,move3,move4,otnum,exp,maxhpev,attackev,defenseev,speedev,specialev,attackiv,defenseiv,speediv,specialiv,move1pp,move1ppup,move2pp,move2ppup,move3pp,move3ppup,move4pp,move4ppup,curlevel,maxhp,attack,defense,speed,special,
	}
	@Override
	public void writtenMemory(int address) {
		//Update pokemon, and trained data, thats stored here
		if (address >= 0xD163 && address <=0xD2F6 ){

		}
		//Name D158-D162
		if (address >= 0xD158 && address <=0xD162 ){
			String name =decode(Arrays.copyOfRange(CPU.WRAM[CPU.CurrentWRAMBank],  0x0158, 1+0x0162));
			o.getScore("Name:").setScore(0);
			o.getScoreboard().getTeam("Name:").setSuffix(" "+name);
		}
		//Rival D34A-D351
		if (address >= 0xD34A && address <=0xD351 ){
			String rival =decode(Arrays.copyOfRange(CPU.WRAM[CPU.CurrentWRAMBank],  0x034A, 1+0x0351));
			o.getScore("Rival:").setScore(1);
			o.getScoreboard().getTeam("Rival:").setSuffix(" "+rival);
		}
		//Money
		if (address >= 0xD347 && address <= 0xD349 ) {
			String amount = BCDtoString(Arrays.copyOfRange(CPU.WRAM[CPU.CurrentWRAMBank], 0x0347, 1+0x349));
			o.getScore("Money:").setScore(2);
			o.getScoreboard().getTeam("Money:").setSuffix(" $"+amount);
		}
		//Items
		if (address >= 0xD53A && address <= 0xD59F ) {

		}
		//Badges
		if (address == 0xD356) {
			int badges = getBitsSet((byte)CPU.read(0xD356));
			o.getScore("Badges:").setScore(3);
			o.getScoreboard().getTeam("Badges:").setSuffix(" "+badges);
		}
		//DA40 - Hours, two bytes
		//DA42 - Minutes, two bytes
		//DA44 - Seconds, one byte
		if (address >= 0xDA40 && address <= 0xDA44 ) {
			int hours = getShort((byte)CPU.read(0xDA41),(byte)CPU.read(0xDA40));
			int minutes = getShort((byte)CPU.read(0xDA43),(byte)CPU.read(0xDA42));
			int seconds = CPU.read(0xDA44);
			o.getScore("Play Time:").setScore(6);
			o.getScoreboard().getTeam("Play Time:").setSuffix(" "+String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+String.format("%02d",seconds));
		}
		//Owned Pokemon
		if (address >= 0xD2F7 && address <= 0xD309 ) {
			int amount = getBitsSet(Arrays.copyOfRange(CPU.WRAM[CPU.CurrentWRAMBank], 0x02F7, 1+0x0309));
			o.getScore("Owned Pokemon:").setScore(4);
			o.getScoreboard().getTeam("Owned Pokemon:").setSuffix(" "+amount);
		}
		//Seen pokemon
		if (address >= 0xD30A && address <= 0xD31C ) {
			int amount = getBitsSet(Arrays.copyOfRange(CPU.WRAM[CPU.CurrentWRAMBank], 0x030A, 1+0x031C));
			o.getScore("Seen Pokemon:").setScore(5);
			o.getScoreboard().getTeam("Seen Pokemon:").setSuffix(" "+amount);
		} 
	}
}

