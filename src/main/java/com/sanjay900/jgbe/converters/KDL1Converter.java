package com.sanjay900.jgbe.converters;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class KDL1Converter extends Converter{
	Objective o;
	Player pl;
	public KDL1Converter(GameboyPlayer gp, Player pl) {
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
		if (address == 0xD086) {
			if ((plugin.cpu.read(address))/6d*20d > 0) {
				pl.setHealth((plugin.cpu.read(address))/6d*20d);
			} else {
				pl.setHealth(0.5);
			}
			o.getScore("Health:").setScore(0);
			o.getScoreboard().getTeam("Health:").setSuffix(" "+plugin.cpu.read(address));
		}
		if (address == 0xD089) {
			o.getScore("Lives:").setScore(0);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+(plugin.cpu.read(address)-1));
		}
		if (address >= 0xD06f && address <= 0xD073) {
			String score = "";
			boolean nonzero = false;
			for (int i = 0; i < 5; i++) {
				if (plugin.cpu.read(0xD06f+i)!=0 || nonzero) {
					nonzero = true;
					score+=plugin.cpu.read(0xD06f+i);
				}
			}
			score+="0";
			o.getScore("Score:").setScore(0);
			o.getScoreboard().getTeam("Score:").setSuffix(" "+score);
		}
	}

}

