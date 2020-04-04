package com.sanjay900.jgbe.converters;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class MarioLand2Converter extends Converter{
	Objective o;
	public MarioLand2Converter(GameboyPlayer gp) {
		super(gp);
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Enemies Killed:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Time Remaining:");
		t.addEntry(t.getName());
		for (int i = 0xA000; i< 0xAFFF;i++) {
			writtenMemory(i);
		}
	}
	@Override
	public void writeMemory(int address) {
		if (address == 0xA22C) {
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+cpu.read(address));
		}
		if (address == 0xA262||address == 0xA263) {
			o.getScore("Coins:").setScore(1);
			o.getScoreboard().getTeam("Coins:").setSuffix(" "+" "+Integer.toHexString(cpu.read(0xA263))+Integer.toHexString(cpu.read(0xA262)));
		}
		if (address == 0xA28D) {
			o.getScore("Enemies Killed:").setScore(0);
			o.getScoreboard().getTeam("Enemies Killed:").setSuffix(" "+cpu.read(address));
		}
		if (address == 0xA254||address == 0xA255) {
			o.getScore("Time Remaining:").setScore(4);
			o.getScoreboard().getTeam("Time Remaining:").setSuffix(" "+Integer.toHexString(cpu.read(0xA255))+Integer.toHexString(cpu.read(0xA254)));
		}
	}

}

