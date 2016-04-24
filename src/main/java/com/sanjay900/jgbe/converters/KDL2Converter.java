package com.sanjay900.jgbe.converters;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;
public class KDL2Converter extends Converter{
	Objective o;
	Player pl;
	public KDL2Converter(GameboyPlayer gp, Player pl) {
		this.pl = pl;
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNewTeam("Score:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Health:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Stars:");
		t.addEntry(t.getName());
		for (int i = 0xC000; i< 0xDFFF;i++) {
			writtenMemory(i);
		}
		writtenMemory(0xa084);
	}
	public String getString(int[] characters) {
		char[] charArr = new char[characters.length]; 
		for(int i=0; i < characters.length; i++) charArr[i] = (char) (characters[i]-1);
		return new String(charArr);
	}
	@Override
	public void writeMemory(int address) {
		if (address == 0xDEE3) {
			int health = (plugin.cpu.read(address)/2);
			if (health/6d*20d > 0) 
				pl.setHealth(health/6d*20d);
			else 
				pl.setHealth(0.5);
			o.getScore("Health:").setScore(0);
			o.getScoreboard().getTeam("Health:").setSuffix(" "+health);
		}
		if (address == 0xDEE1) {
			int star = (plugin.cpu.read(address));
			pl.setExp(star/7f);
			o.getScore("Stars:").setScore(0);
			o.getScoreboard().getTeam("Stars:").setSuffix(" "+star);
		}
		if (address == 0xa084) {
			pl.setTotalExperience(plugin.cpu.read(address));
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address >= 0xDedb && address <= 0xDedd) {
			String score = "";
			boolean nonzero = false;
			for (int i = 0; i < 3; i++) {
				if (plugin.cpu.read(0xDedb+i)!=0 || nonzero) {
					nonzero = true;
					score+=String.format("%02d",Integer.parseInt(Integer.toHexString(plugin.cpu.read(0xDedb+i))));
				}
			}
			score+="0";
			o.getScore("Score:").setScore(0);
			o.getScoreboard().getTeam("Score:").setSuffix(" "+score);
		}
	}

}

