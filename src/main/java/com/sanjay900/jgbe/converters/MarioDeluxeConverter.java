package com.sanjay900.jgbe.converters;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class MarioDeluxeConverter extends Converter{
	Objective o;
	Player pl;
	public MarioDeluxeConverter(GameboyPlayer gp, Player pl) {
		this.pl = pl;
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Score:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Time Remaining:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Level:");
		t.addEntry(t.getName());
		for (int i = 0xC000; i< 0xDFFF;i++) {
			writtenMemory(i);
		}
	}
	public String getString(int[] characters) {
		char[] charArr = new char[characters.length]; 
		for(int i=0; i < characters.length; i++) charArr[i] = (char) (characters[i]-1);
		return new String(charArr);
	}
	@Override
	public void writeMemory(int address) {
		if (address == 0xc17a || address == 0xc17b) {
			int score = plugin.cpu.read(0xc17b)*255;
			score+=plugin.cpu.read(0xc17a);
			o.getScore("Score:").setScore(0);
			o.getScoreboard().getTeam("Score:").setSuffix(" "+score+"0");
		}
		if (address == 0xc1f2) {
			o.getScore("Coins:").setScore(0);
			o.getScoreboard().getTeam("Coins:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address == 0xc17f) {
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address == 0xc17e||address == 0xc17d) {
			int time = plugin.cpu.read(0xc17e)*255;
			time+=plugin.cpu.read(0xc17d);
			o.getScore("Time Remaining:").setScore(0);
			o.getScoreboard().getTeam("Time Remaining:").setSuffix(" "+time);
		}
	}

}

