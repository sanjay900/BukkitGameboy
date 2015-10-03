package com.sanjay900.jgbe.converters;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;
//TODO: Make dkl2 converter
public class DonkeyKongLand2Converter extends Converter{
	Objective o;
	public DonkeyKongLand2Converter(GameboyPlayer gp) {
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Coins:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Score:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("World:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Time Remaining:");
		t.addEntry(t.getName());
	}
	@Override
	public void writeMemory(int address) {
		if (address == 0xDA15) {
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address == 0x9829||address == 0x982A) {
			o.getScore("Coins:").setScore(1);
			int coin = plugin.cpu.read(0x982A);
			if (coin ==44) coin=0;
			if (plugin.cpu.read(0x9829) !=44)
			coin +=(plugin.cpu.read(0x9829)*10);
			o.getScoreboard().getTeam("Coins:").setSuffix(" "+coin);
		}
		if (address == 0x9825||address == 0x9824||address == 0x9823||address == 0x9822||address == 0x9821) {
			o.getScore("Score:").setScore(2);
			int score = plugin.cpu.read(0x9825);
			if (plugin.cpu.read(0x9824) != 44)
			score +=(plugin.cpu.read(0x9824)*10);
			if (plugin.cpu.read(0x9823) != 44)
			score +=(plugin.cpu.read(0x9823)*100);
			if (plugin.cpu.read(0x9822) != 44)
			score +=(plugin.cpu.read(0x9822)*1000);
			if (plugin.cpu.read(0x9821) != 44)
			score +=(plugin.cpu.read(0x9821)*10000);
			o.getScoreboard().getTeam("Score:").setSuffix(" "+score);
		}
		if (address == 0x982c||address == 0x982e) {
			o.getScore("World:").setScore(3);
			if (plugin.cpu.read(0x982c) == 44) {
				o.getScoreboard().getTeam("World:").setSuffix(" Menu");
				return;
			}
			o.getScoreboard().getTeam("World:").setSuffix(" "+plugin.cpu.read(0x982c)+"-"+plugin.cpu.read(0x982e));
		}
		if (address == 0x9833||address == 0x9832||address == 0x9831) {
			o.getScore("Time Remaining:").setScore(4);
			int time = plugin.cpu.read(0x9833);
			time +=(plugin.cpu.read(0x9832)*10);
			time +=(plugin.cpu.read(0x9831)*100);
			if (time == 8184) time = 0;
			o.getScoreboard().getTeam("Time Remaining:").setSuffix(" "+time);
		}
	}

}

