package com.sanjay900.jgbe.converters;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class CrystalConverter extends Converter{
	String[] pokemon= {" (0)", "Rhydon (1)", "Kangaskhan (2)", "Nidoranm (3)", "Clefairy (4)", "Spearow (5)", "Voltorb (6)", "Nidoking (7)", "Slowbro (8)", "Ivysaur (9)", "Exeggutor (10)", "Lickitung (11)", "Exeggcute (12)", "Grimer (13)", "Gengar (14)", "Nidoranh (15)", "Nidoqueen (16)", "Cubone (17)", "Rhyhorn (18)", "Lapras (19)", "Arcanine (20)", "Mew (21)", "Gyarados (22)", "Shellder (23)", "Tentacool (24)", "Gastly (25)", "Scyther (26)", "Staryu (27)", "Blastoise (28)", "Pinsir (29)", "Tangela (30)", "Missingno (31)", "Missingno (32)", "Growlithe (33)", "Onix (34)", "Fearow (35)", "Pidgey (36)", "Slowpoke (37)", "Kadabra (38)", "Graveler (39)", "Chansey (40)", "Machoke (41)", "Mr.Mime (42)", "Hitmonlee (43)", "Hitmonchan (44)", "Arbok (45)", "Parasect (46)", "Psyduck (47)", "Drowzee (48)", "Golem (49)", "Missingno (50)", "Magmar (51)", "Missingno (52)", "Electabuzz (53)", "Magneton (54)", "Koffing (55)", "Missingno (56)", "Mankey (57)", "Seel (58)", "Diglett (59)", "Tauros (60)", "Missingno (61)", "Missingno (62)", "Missingno (63)", "Farfetch'd (64)", "Venonat (65)", "Dragonite (66)", "Missingno (67)", "Missingno (68)", "Missingno (69)", "Doduo (70)", "Poliwag (71)", "Jynx (72)", "Moltres (73)", "Articuno (74)", "Zapdos (75)", "Ditto (76)", "Meowth (77)", "Krabby (78)", "Missingno (79)", "Missingno (80)", "Missingno (81)", "Vulpix (82)", "Ninetales (83)", "Pikachu (84)", "Raichu (85)", "Missingno (86)", "Missingno (87)", "Dratini (88)", "Dragonair (89)", "Kabuto (90)", "Kabutops (91)", "Horsea (92)", "Seadra (93)", "Missingno (94)", "Missingno (95)", "Sandshrew (96)", "Sandslash (97)", "Omanyte (98)", "Omastar (99)", "Jigglypuff (100)", "Wigglytuff (101)", "Eevee (102)", "Flareon (103)", "Jolteon (104)", "Vaporeon (105)", "Machop (106)", "Zubat (107)", "Ekans (108)", "Paras (109)", "Poliwhirl (110)", "Poliwrath (111)", "Weedle (112)", "Kakuna (113)", "Beedrill (163)", "Missingno (115)", "Dodrio (116)", "Primeape (117)", "Dugtrio (118)", "Venomoth (119)", "Dewgong (120)", "Missingno (121)", "Missingno (122)", "Caterpie (123)", "Metapod (124)", "Butterfree (125)", "Machamp (126)", "Missingno (127)", "Golduck (128)", "Hypno (129)", "Golbat (130)", "Mewtwo (131)", "Snorlax (132)", "Magikarp (133)", "Missingno (134)", "Missingno (135)", "Muk (136)", "Missingno (137)", "Kingler (138)", "Cloyster (139)", "Missingno (140)", "Electrode (141)", "Clefable (142)", "Weezing (143)", "Persian (144)", "Marowak (145)", "Missingno (146)", "Haunter (147)", "Abra (148)", "Alakazam (149)", "Pidgeotto (150)", "Pidgeot (151)", "Starmie (152)", "Bulbasaur (153)", "Venusaur (154)", "Tentacruel (155)", "Missingno (156)", "Goldeen (163)", "Seaking (158)", "Missingno (159)", "Missingno (160)", "Missingno (161)", "Missingno (162)", "Ponyta (163)", "Rapidash (164)", "Rattata (165)", "Raticate (166)", "Nidorino (167)", "Nidorina (168)", "Geodude (169)", "Porygon (170)", "Aerodactyl (171)", "Missingno (172)", "Magnemite (173)", "Missingno (174)", "Missingno (175)", "Charmander (176)", "Squirtle (177)", "Charmeleon (178)", "Wartortle (179)", "Charizard (180)", "Missingno (181)", "Missingno (182)", "Missingno (183)", "Missingno (184)", "Oddish (185)", "Gloom (186)", "Vileplume (187)", "Bellsprout (188)", "Weepinbell (189)", "Victreebel (190)", "~191", "~192", "~193", "~194", "~195", "~196", "~197", "~198", "~199", "~200", "~201", "~202", "~203", "~204", "~205", "~206", "~207", "~208", "~209", "~210", "~211", "~212", "~213", "~214", "~215", "~216", "~217", "~218", "~219", "~220", "~221", "~222", "~223", "~224", "~225", "~226", "~227", "~228", "~229", "~230", "~231", "~232", "~233", "~234", "~235", "~236", "~237", "~238", "~239", "~240", "~241", "~242", "~243", "~244", "~245", "~246", "~247", "~248", "~249", "~250", "~251", "~252", "~253", "~254", "-- No Pokemon -- (255)"};
	HashMap<Integer,ByteBuffer> pkm2 = new HashMap<>();

	Objective o;
	private String[] table;
	public CrystalConverter(GameboyPlayer gp) {
		super(gp);
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
		return cpu.read(0xDA22);
	}
	public Object get(int p, dataType var) {
		int pk = 48*p;
		switch (var) {
		case sprite:
			return cpu.read(0xDA2A+pk);
		case num:
			return cpu.read(0xDA23+p);
		case otname:
			return decode(Arrays.copyOfRange(cpu.WRAM[0], 0x0B4A+11*p, 0x0B54+11*p));
		case name: 
			return decode(Arrays.copyOfRange(cpu.WRAM[0], 0x0B8C+11*p, 0x0B96+11*p));
		case hp:
			return getShort(cpu.read(0xDA4D+pk),cpu.read(0xDA4C+pk));
		case level:
			return cpu.read(0xDA49+pk);
		case maxhp:
			return getShort(cpu.read(0xDA4F+pk),cpu.read(0xDA4E+pk));
		case attack:
			return getShort(cpu.read(0xDA51+pk),cpu.read(0xDA50+pk));
		case defense:
			return getShort(cpu.read(0xDA53+pk),cpu.read(0xDA52+pk));
		case speed:
			return getShort(cpu.read(0xDA55+pk),cpu.read(0xDA54+pk));
		case special:
			return getShort(cpu.read(0xDA57+pk),cpu.read(0xDA56+pk));
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
		return pokemon[(int) get(index,dataType.num)];
	}
	public enum dataType {
		sprite,num,otname,name,hp,level,maxhp,attack,defense,speed,special
	}
	//http://webcache.googleusercontent.com/search?q=cache:F1hlD6deZ0gJ:forums.glitchcity.info/index.php%3Ftopic%3D1342.0+&cd=1&hl=en&ct=clnk&gl=nz
	@Override
	public void writeMemory(int address) {
		//Update pokemon, and trained data, thats stored here
		if (address >= 0xD163 && address <=0xD2F6 ){

		}
		//Name D1A3-D1AC
		if (address >= 0xD47D && address <=0xD484+4 ){
			int[] t = getWram(0xD47D, 0xD484+4);
			String name =decode(t);
			o.getScore("Name:").setScore(0);
			o.getScoreboard().getTeam("Name:").setSuffix(" "+name);
		}
		//Rival D1BC-D1B5
		if (address >= 0xD493 && address <=0xD49A ){
			String rival =decode(getWram(0xD493, 0xD49A));
			o.getScore("Rival:").setScore(1);
			o.getScoreboard().getTeam("Rival:").setSuffix(" "+rival);
		}
		//Money D573-D575
		if (address >= 0xD84E && address <= 0xD850  ) {
			int[] i = getWram(0xD84E, 0xD850);
			o.getScore("Money:").setScore(2);
			o.getScoreboard().getTeam("Money:").setSuffix(" $"+(i[2] + (i[1] << 8) + (i[0] << 16)));
		}
		//Badges
		if (address == 0xD857||address == 0xD858) {
			int badges = getBitsSet((byte)cpu.read(0xD857));
			badges += getBitsSet((byte)cpu.read(0xD858));
			o.getScore("Badges:").setScore(3);
			o.getScoreboard().getTeam("Badges:").setSuffix(" "+badges);
		}
		//d4c4 - Hours, two bytes
		//d4c6 - Minutes, two bytes
		//d4c7 - Seconds, one byte
		if (address >= 0xd4c4 && address <= 0xd4c7 ) {
			int hours = getShort(cpu.read(0xd4c5),cpu.read(0xd4c4));
			int minutes = cpu.read(0xd4c6);
			int seconds = cpu.read(0xd4c7);
			o.getScore("Play Time:").setScore(6);
			o.getScoreboard().getTeam("Play Time:").setSuffix(" "+String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+String.format("%02d",seconds));
		}
		//Owned Pokemon
		if (address >= 0xDE99  && address <= 0xDE99+33 ) {
			int amount = getBitsSet2(getWram(0xDE99, 0xDE99+32));
			o.getScore("Owned Pokemon:").setScore(4);
			o.getScoreboard().getTeam("Owned Pokemon:").setSuffix(" "+amount);
		}
		//Seen pokemon
		if (address >= 0xDEB9 && address <= 0xDEB9+33  ) {
			int amount = getBitsSet2(getWram(0xDEB9, 0xDEB9+32));
			o.getScore("Seen Pokemon:").setScore(5);
			o.getScoreboard().getTeam("Seen Pokemon:").setSuffix(" "+amount);
		} 


	}
	private int getBitsSet2(int[] wram) {
		int c =0;
		for (int i =0; i < 251;i++) {
			if ((wram[i >> 3] >> (i & 7) & 1)!= 0) {
				c++;
			}
		}
		return c;
	}


}

