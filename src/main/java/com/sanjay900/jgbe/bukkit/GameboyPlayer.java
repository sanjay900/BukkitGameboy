package com.sanjay900.jgbe.bukkit;

import java.io.DataInputStream;
import java.io.File;
import java.util.UUID;

import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.packets.Key;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import com.sanjay900.jgbe.converters.BombermanConverter;
import com.sanjay900.jgbe.converters.Converter;
import com.sanjay900.jgbe.converters.CrystalConverter;
import com.sanjay900.jgbe.converters.MarioLand1Converter;
import com.sanjay900.jgbe.converters.RedBlueConverter;
import com.sanjay900.jgbe.converters.YellowConverter;
import com.sanjay900.jgbe.emu.FHandler;

public class GameboyPlayer {
	GameboyPlugin plugin = GameboyPlugin.getInstance();
	UUID sender;
	public Converter conv;
	public Scoreboard board;
	GameboyPlayer(final Player p, final String rom) {
		
		
		/*final Location l = new Location(Bukkit
				.getWorld("gba"), 76,
				46.75, 66);
		l.getBlock().getRelative(BlockFace.DOWN).setType(Material.BARRIER);
		l.setDirection(new Vector (0,0,-1));
		p.teleport(l);*/
		p.setAllowFlight(false);
		p.setFlying(false);
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable(){

			@Override
			public void run() {
				if (plugin.cpu.canRun()) return;
				sender = p.getUniqueId();
				ItemStack item = new ItemStack(Material.WATCH,
						1);
				ItemMeta itemMeta = item.getItemMeta();
				itemMeta.setDisplayName(ChatColor.GOLD
						+ "Start/Select");
				item.setItemMeta(itemMeta);

				ItemStack stuck = new ItemStack(
						Material.REDSTONE, 1);
				ItemMeta stuckMeta = item.getItemMeta();
				stuckMeta.setDisplayName(ChatColor.GOLD
						+ "Connect to another Gameboy");
				stuck.setItemMeta(stuckMeta);
				ItemStack exit = new ItemStack(
						Material.BARRIER, 1);
				ItemMeta exitMeta = item.getItemMeta();
				exitMeta.setDisplayName(ChatColor.RED
						+ "Quit Game");
				exit.setItemMeta(exitMeta);
				p.getInventory().clear();
				p.getInventory().setItem(0, item);
				p.getInventory().setItem(2, stuck);
				p.getInventory().setItem(8, exit);
				p.getInventory().setHeldItemSlot(0);
				p.sendMessage(ChatColor.YELLOW+"Controlls: Jump for A, Sneak for B, Hold the clock and Mine for start, Interact for select, and move for directionals.");
				NMSUtils.getInstance().getUtil().stealPlayerControls(p.getLocation(),p);
				Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    try {
                        //TODO stop this being hardcoded.
                        String filename = plugin.getDataFolder().getAbsolutePath()+"/roms/"+rom;
                        plugin.tryToLoadROM(filename);
                        File f = new File(plugin.getDataFolder().getAbsolutePath()+"/saves/"+p.getUniqueId().toString()+"_"+GameboyPlugin.curcartname+".st");
                        if (f.exists()) {
                            DataInputStream distream = FHandler.getDataInputStream(plugin.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+p.getUniqueId().toString()+"_"+GameboyPlugin.curcartname+".st");
                            plugin.cpu.loadState(distream);
                            distream.close();
                        }
                        if (plugin.cpu.isConnected())
                            plugin.cpu.severLink();

                        plugin.cpu.serveLink(p);
                        Bukkit.getScheduler().runTaskLater(GameboyPlugin.getInstance(),() -> {
                            board = Bukkit.getScoreboardManager().getNewScoreboard();

                            p.setScoreboard(board);
                            if (rom.contains("red")||rom.contains("blue"))
                                conv = new RedBlueConverter(GameboyPlayer.this);
                            if(rom.contains("yellow"))
                                conv = new YellowConverter(GameboyPlayer.this);
                            if (rom.contains("crystal"))
                                conv = new CrystalConverter(GameboyPlayer.this);
                            if(rom.contains("land.gb"))
                                conv = new MarioLand1Converter(GameboyPlayer.this);
                            if(rom.contains("bomb"))
                                conv = new BombermanConverter(GameboyPlayer.this);
                        },5l);

                        GameboyPlugin.resumeEmulation(true);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                },7L);
			}}, 20L);


	}
	public void press(boolean b, int i) {
		if (b) {
			plugin.cpu.pressButton(keyMasks[i]);
		} else {
			plugin.cpu.releaseButton(keyMasks[i]);
		}
	}
	public void keyToggled(Key key, boolean pressed) {
		if (plugin.cpu.cartridge == null) return;
		switch (key) {
		case UP:
			press(pressed,0);
			break;
		case DOWN:
			press(pressed,1);
			break;
		case LEFT:
			press(pressed,2);
			break;
		case RIGHT:
			press(pressed,3);
			break;
		case JUMP:
			press(pressed,4);
			break;
		case UNMOUNT:
			press(pressed,5);
			break;
		case OPEN_INVENTORY:
			press(pressed,7);
			break;
		case DROP_ITEM:
			press(pressed,6);
			break;
		case BREAK:
			break;
		case PLACE:
			break;
		default:
			break;

		}
	}
	static final int[] keyMasks = {
		(1<<2),
		(1<<3),
		(1<<1),
		(1<<0),
		(1<<4),
		(1<<5),
		(1<<7),
		(1<<6),
	};
}
