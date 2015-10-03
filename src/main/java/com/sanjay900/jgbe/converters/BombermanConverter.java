package com.sanjay900.jgbe.converters;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;
//TODO: Make bomberman converter
public class BombermanConverter extends Converter{
	Objective o;
	public BombermanConverter(GameboyPlayer gp) {
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Lives Left:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Level:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Enemies:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Max Bombs:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Time Remaining:");
		t.addEntry(t.getName());
	}
	@Override
	public void writeMemory(int address) {
		//TODO: work out lives
		if (address == 0xc106) {
			o.getScore("Lives Remaining:").setScore(0);
			int life = plugin.cpu.read(address)-1;
			life = life==-1?0:life;
			o.getScoreboard().getTeam("Lives Remaining:").setSuffix(" "+0);
		}
		if (address == 0xc107) {
			o.getScore("Enemies:").setScore(0);
			o.getScoreboard().getTeam("Enemies:").setSuffix(" "+plugin.cpu.read(address));
		}
		//TODO: Work out level
		if (address == 0xc058) {
			o.getScore("Level:").setScore(0);
			o.getScoreboard().getTeam("Level:").setSuffix(" "+plugin.cpu.read(address));
		}
		//TODO: Work out max bombs
		if (address == 0xc074) {
			o.getScore("Max Bombs:").setScore(0);
			o.getScoreboard().getTeam("Max Bombs:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address == 0xc032||address == 0xc033||address == 0xc034) {
			o.getScore("Time Remaining:").setScore(0);
			o.getScoreboard().getTeam("Time Remaining:").setSuffix(" "+plugin.cpu.read(0xc034)+":"+plugin.cpu.read(0xc033)+plugin.cpu.read(0xc032));
		}
		
	}

}

