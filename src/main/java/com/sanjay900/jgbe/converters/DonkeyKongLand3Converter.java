package com.sanjay900.jgbe.converters;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;
public class DonkeyKongLand3Converter extends Converter{
	Objective o;
	public DonkeyKongLand3Converter(GameboyPlayer gp) {
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Character:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Banannas:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Bear Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Bonus Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("DK Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Play Time:");
		t.addEntry(t.getName());
		for (int i = 0xFF00; i< 0xFFFF;i++) {
			writtenMemory(i);
		}
	}

	String[] characters = new String[]{"Kiddy Kong","Dixie Kong","Ellie","Squawks","Enguarde","Squitter","Rattly","Toboggan"};
	@Override
	public void writeMemory(int address) {
		if (address == 0xFFAC) {
			o.getScore("Character:").setScore(0);
			o.getScoreboard().getTeam("Character:").setSuffix(" "+characters[plugin.cpu.read(address)]);
		}
		if (address == 0xFFAA) {
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
		if (address == 0xFFAB) {
			o.getScore("Banannas:").setScore(0);
			o.getScoreboard().getTeam("Banannas:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
		if (address == 0xFFB3) {
			o.getScore("Bear Coins:").setScore(0);
			o.getScoreboard().getTeam("Bear Coins:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
		if (address == 0xFFB4) {
			o.getScore("Bonus Coins:").setScore(0);
			o.getScoreboard().getTeam("Bonus Coins:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
		if (address == 0xFFB5) {
			o.getScore("DK Coins:").setScore(0);
			o.getScoreboard().getTeam("DK Coins:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
		if (address == 0xFFB0 || address == 0xFFB1 ||address == 0xFFB2 ) {
			o.getScore("Play Time:").setScore(0);
			o.getScoreboard().getTeam("Play Time:").setSuffix(" "+String.format("%02d",Integer.parseInt(Integer.toHexString(plugin.cpu.read(0xFFB0))))+":"+String.format("%02d",Integer.parseInt(Integer.toHexString(plugin.cpu.read(0xFFB1))))+":"+String.format("%02d",Integer.parseInt(Integer.toHexString(plugin.cpu.read(0xFFB2)))));
		}
	}

}

