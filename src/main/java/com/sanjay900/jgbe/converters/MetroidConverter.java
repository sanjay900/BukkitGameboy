package com.sanjay900.jgbe.converters;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class MetroidConverter extends Converter{
	Objective o;
	public MetroidConverter(GameboyPlayer gp) {
		super(gp);
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Energy:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Missiles:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Metroids left:");
		t.addEntry(t.getName());
		for (int i = 0xD000; i< 0xDFFF;i++) {
			writtenMemory(i);
		}
	}
	@Override
	public void writeMemory(int address) {
		if (address == 0xD051) {
			o.getScore("Energy:").setScore(0);
			o.getScoreboard().getTeam("Energy:").setSuffix(" "+Integer.toHexString(cpu.read(address)));
		}
		if (address == 0xD053) {
			o.getScore("Missiles:").setScore(0);
			o.getScoreboard().getTeam("Missiles:").setSuffix(" "+Integer.toHexString(cpu.read(address)));
		}
		if (address == 0xD09A) {
			o.getScore("Metroids left:").setScore(0);
			o.getScoreboard().getTeam("Metroids left:").setSuffix(" "+Integer.toHexString(cpu.read(address)));
		}
	}

}

