package com.sanjay900.jgbe.converters;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

import com.sanjay900.jgbe.bukkit.GameboyPlayer;

public class AsteroidsConverter extends Converter{
	Objective o;
	Player pl;
	//TOOD: new api for database
	public AsteroidsConverter(GameboyPlayer gp, Player pl) {
	/*	this.pl = pl;
		o = gp.board.registerNewObjective("data", "dummy");
		o.setDisplayName("Player Stats");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Team t = gp.board.registerNe .wTeam("Score:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("Lives:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("High Score:");
		t.addEntry(t.getName());
		t = gp.board.registerNewTeam("r High Score:");
		t.setPrefix("Serve");
		t.addEntry(t.getName());
		for (int i = 0xc000; i< 0xcFFF;i++) {
			writtenMemory(i);
		}
		c = EyrePlugin.getInstance().getDb().getConnection();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				PreparedStatement st = EyrePlugin.getInstance().getDb().getConnection().prepareStatement("select * from highscores where rom='asteroids.gb' ORDER BY score DESC LIMIT 1 ");
				ResultSet rs = st.executeQuery();
				rs.first();
				setHighScore(rs.getInt("score"));
				st = EyrePlugin.getInstance().getDb().getConnection().prepareStatement("select * from highscores where rom='asteroids.gb' AND name=? ORDER BY score DESC LIMIT 1 ");
				st.setString(1, pl.getName());
				rs = st.executeQuery();
				if (rs.first())
					pscore = rs.getInt("score");

				o.getScore("High Score:").setScore(2);
				o.getScoreboard().getTeam("High Score:").setSuffix(" "+pscore);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		EyrePlugin.getInstance().getHandler().client.on("asteroidsScore", obj -> {
			JSONObject score2 = (JSONObject)obj[0];
			try {
				if (score2.getInt("score") > highscore) {
					setHighScore(score2.getInt("score"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});*/
	}
	/*
	int highscore = -1;
	int pscore = 0;
	boolean updatingScore = false;
	int[] hdigits = new int[5];
	public void setHighScore(int score) {
		updatingScore= true;
		o.getScore("r High Score:").setScore(2);
		o.getScoreboard().getTeam("r High Score:").setSuffix(" "+score);
		if (highscore != score) {
			highscore = score;
			int i = 4;
			while (score > 0) {
				hdigits[i] = score%10;
				plugin.cpu.write(i+0xc04c, hdigits[i]);
				score/=10;
				i--;
			}
		}
		
		updatingScore = false;
	}
	private void updateHighScore() {
		updatingScore= true;
		if (!Arrays.equals(this.getWram(0xc04c, 0xc050), hdigits)) {
			for (int i =0; i <5;i++) {
				plugin.cpu.write(i+0xc04c, hdigits[i]);
			}
		}	
		updatingScore = false;	
	}
	Connection c;
	private void updateScore(int score) {
		if (this.highscore != -1) {
			if (score > highscore) {
				setHighScore(score);
			}
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

			if (score != pscore) {
				if (score > pscore) {
					pscore = score;
					o.getScore("High Score:").setScore(2);
					o.getScoreboard().getTeam("High Score:").setSuffix(" "+pscore);
					try {
						PreparedStatement st = c.prepareStatement(
								"INSERT INTO highscores (rom, name, score) VALUES (?, ?, ?) "+
										"ON DUPLICATE KEY UPDATE rom=VALUES(rom), name=VALUES(name), score=VALUES(score);"
								);
						st.setString(1, "asteroids.gb");
						st.setString(2, pl.getName());
						st.setInt(3, score);
						st.executeUpdate();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				pscore = score;
				try {
					JSONObject obj = new JSONObject();
					obj.put("name", pl.getName());
					obj.put("score", score);
					EyrePlugin.getInstance().getHandler().sendGlobalMessage("asteroidsScore", obj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
		
	}*/
	@Override
	public void writeMemory(int address) {
		/*
		if (plugin.cpu.read(0xDa39) == 1) return;
		if (address >= 0xC051&&address <= 0xC055) {
			o.getScore("Score:").setScore(3);
			String score = "";
			for (int i =0; i< 5; i++) {
				score+=Integer.toHexString(plugin.cpu.read(0xC051+i));
			}
			o.getScoreboard().getTeam("Score:").setSuffix(" "+score);
			updateScore(Integer.parseInt(score));
		}
		if (!updatingScore&&address >= 0xC04C&&address <= 0xc050) {
			updateHighScore();
		}
		if (address == 0xDC0B) {
			o.getScore("Lives:").setScore(3);
			o.getScoreboard().getTeam("Lives:").setSuffix(" "+Integer.toHexString(plugin.cpu.read(address)));
		}
	*/}

}

