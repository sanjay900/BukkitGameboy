package com.sanjay900.jgbe.bukkit;

import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.sanjay900.jgbe.converters.Converter;
import com.sanjay900.jgbe.converters.GoldConverter;
import com.sanjay900.jgbe.converters.PokemonConverter;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.FHandler;
import com.sanjay900.jgbe.emu.swinggui;
import com.sanjay900.nmsUtil.movementController.ControllerFactory;
import com.sanjay900.nmsUtil.util.Button;

public class GameboyPlayer {
	swinggui jb = swinggui.getInstance();
	UUID sender;
	public Converter conv;
	public Scoreboard board;
	GameboyPlayer(final Player p, final String rom) {
		
		
		final Location l = new Location(Bukkit
				.getWorld("gba"), 76,
				46.75, 66);
		l.getBlock().getRelative(BlockFace.DOWN).setType(Material.BARRIER);
		l.setDirection(new Vector (0,0,-1));
		p.teleport(l);
		p.setAllowFlight(false);
		p.setFlying(false);
		Bukkit.getScheduler().runTaskLater(jb, new Runnable(){

			@Override
			public void run() {
				if (CPU.canRun()) return;
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
				new ControllerFactory().withLocation(l).withPlayer(p).build();	
				Bukkit.getScheduler().runTaskLaterAsynchronously(jb, new Runnable(){

					@Override
					public void run() {
						try {
							String filename = jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/roms/"+rom;
							jb.tryToLoadROM(filename);
							jb.pauseEmulation(false);
							File f = new File(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+p.getUniqueId().toString()+"_"+swinggui.curcartname+".st");
							if (f.exists()) {
								DataInputStream distream = FHandler.getDataInputStream(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+p.getUniqueId().toString()+"_"+swinggui.curcartname+".st");
								CPU.loadState(distream);
								distream.close();
							}
							if (CPU.isConnected())
								CPU.severLink();

							CPU.serveLink(p);
							Bukkit.getScheduler().runTaskLater(swinggui.getInstance(),() -> {
								board = Bukkit.getScoreboardManager().getNewScoreboard();

								p.setScoreboard(board);
								if (rom.contains("red")||rom.contains("blue")||rom.contains("yellow")) 
									conv = new PokemonConverter(GameboyPlayer.this);
								if (rom.contains("gold")||rom.contains("silver")) 
									conv = new GoldConverter(GameboyPlayer.this);

							},5l);

						} catch (Exception ex) {
							ex.printStackTrace();
						}
						jb.resumeEmulation(false);
					}},7l);
			}}, 20l);


	}
	public void press(boolean b, int i) {
		if (b) {
			CPU.pressButton(keyMasks[i]);
		} else {
			CPU.releaseButton(keyMasks[i]);
		}
	}
	public void keyToggled(Button key, boolean pressed) {
		if (CPU.cartridge == null) return;
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
		case OPENINVENTORY:
			press(pressed,7);
			break;
		case DROPITEM:
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
