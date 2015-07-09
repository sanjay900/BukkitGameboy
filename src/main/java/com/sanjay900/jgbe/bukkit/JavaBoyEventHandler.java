package com.sanjay900.jgbe.bukkit;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.FHandler;
import com.sanjay900.jgbe.emu.swinggui;
import com.sanjay900.menus.api.Icon;
import com.sanjay900.menus.api.Menu;
import com.sanjay900.nmsUtil.events.PlayerPushedKeyEvent;
import com.sanjay900.nmsUtil.util.Button;
import com.sanjay900.nmsUtil.util.Cooldown;

public class JavaBoyEventHandler implements Listener{
	swinggui jb = swinggui.getInstance();
	public Menu menu;
	int serverId;
	public String cart = "";
	public JavaBoyEventHandler() {
		serverId= Bukkit.getServer().getPort()-25580;
		menu = new Menu(this.jb, ChatColor.BLUE+"Gameboy Link Menu",9, null);
		Bukkit.getPluginManager().registerEvents(this, jb);
	}
	@EventHandler
	public void onJoin(final PlayerJoinEvent evt) {
		Bukkit.getOnlinePlayers().forEach(player -> Bukkit.getOnlinePlayers().forEach(player::hidePlayer));
		if (jb.gp != null) {
			evt.getPlayer().setAllowFlight(true);
			evt.getPlayer().setFlying(true);
			evt.getPlayer().getInventory().clear();
			evt.getPlayer().updateInventory();
			Location l = new Location(Bukkit
					.getWorld("gba"), 76,
					46.5, 66);
			l.setDirection(new Vector (0,0,-1));
			evt.getPlayer().teleport(l);
		}else if (!cart.equals("")) {
			Bukkit.getScheduler().runTaskLater(jb, new Runnable(){

				@Override
				public void run() {
					evt.getPlayer().setAllowFlight(true);
					evt.getPlayer().setFlying(true);
					jb.gp = new GameboyPlayer(evt.getPlayer(),cart);
					cart = "";

				}}, 20l);
		}

	}
	@EventHandler
	public void hurt (EntityDamageEvent evt) {
		evt.setCancelled(true);
	}
	@EventHandler
	public void dropItem(PlayerDropItemEvent evt) {
		evt.setCancelled(true);
	}
	@EventHandler
    public void mapAttack(HangingBreakByEntityEvent evt) {
		evt.setCancelled(true);
    	
    }
	@EventHandler
    public void mapclick(final PlayerInteractEntityEvent evt) {
		evt.setCancelled(true);
		onClick(evt.getPlayer(),Action.RIGHT_CLICK_AIR);
    	
    }
	@EventHandler 
	public void blockplace(final BlockPlaceEvent evt) {
		evt.setBuild(false);
	}
    @EventHandler
    public void mapclick(final EntityDamageByEntityEvent evt) {
		evt.setCancelled(true);
		if (evt.getDamager() instanceof Player && evt.getEntity() instanceof ItemFrame)
		onClick((Player) evt.getDamager(),Action.LEFT_CLICK_AIR);
    }
    @EventHandler
    public void mapclick(final PlayerInteractAtEntityEvent evt) {
		evt.setCancelled(true);
		onClick(evt.getPlayer(),Action.RIGHT_CLICK_AIR);
    }
    @EventHandler
	public void onClick(final PlayerInteractEvent evt) {
		evt.setCancelled(true);
		onClick(evt.getPlayer(),evt.getAction());
		
	}
    public void onClick(final Player pl, Action action) {
    	if (jb.gp == null) return;
    	ItemStack it = pl.getItemInHand();
		final Button buttonPressed;
		if (it.hasItemMeta()
				&& it.getItemMeta().hasDisplayName()
				&& it.getItemMeta().getDisplayName()
				.contains("Connect to another Gameboy")) {
			createLinkMenu(pl);
			return;
		}


		if (it.hasItemMeta()
				&& it.getItemMeta().hasDisplayName()
				&& it.getItemMeta().getDisplayName()
				.contains("Quit Game")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF("lobby");
			pl.sendPluginMessage(jb, "BungeeCord", out.toByteArray());

			return;
		}

		else if (action == Action.RIGHT_CLICK_AIR
				|| action == Action.RIGHT_CLICK_BLOCK) {

			if (it.hasItemMeta()
					&& it.getItemMeta().hasDisplayName()
					&& it.getItemMeta().getDisplayName()
					.contains("Start/Select")) {
				buttonPressed=Button.OPENINVENTORY;

			} else if (it.hasItemMeta()
					&& it.getItemMeta().hasDisplayName()
					&& it.getItemMeta().getDisplayName()
					.contains("A/B")) {
				buttonPressed=Button.UNMOUNT;
			} else {
				return;
			}

		} else if (action == Action.LEFT_CLICK_AIR
				|| action == Action.LEFT_CLICK_BLOCK) {

			if (it.hasItemMeta()
					&& it.getItemMeta().hasDisplayName()
					&& it.getItemMeta().getDisplayName()
					.contains("Start/Select")) {
				buttonPressed=Button.DROPITEM;

			} else if (it.hasItemMeta()
					&& it.getItemMeta().hasDisplayName()
					&& it.getItemMeta().getDisplayName()
					.contains("A/B")) {
				buttonPressed=Button.JUMP;

			}else {
				return;
			}
		} else {
			return;
		}
		jb.gp.keyToggled(buttonPressed,true);
		Bukkit.getScheduler().runTaskLater(jb, new Runnable(){

			@Override
			public void run() {
				jb.gp.keyToggled(buttonPressed,false);
			}}, 10l);

    }
	@EventHandler
	public void pushedKey(final PlayerPushedKeyEvent evt) {
		if (jb.gp != null) {
			if (evt.getButtonsPressed().contains(Button.OPENINVENTORY)) {
				if (jb.gp != null) {
					jb.gp.keyToggled(Button.OPENINVENTORY, true);
					evt.setCancelled(true);
					Bukkit.getScheduler().runTaskLater(jb, new Runnable(){
						@Override
						public void run() {
							if (Cooldown.tryCooldown(evt.getPlayer(), "start", 200))
							jb.gp.keyToggled(Button.OPENINVENTORY, false);
						}}, 4l);
				}
				return;
			}
			if (evt.getButtonsPressed().contains(Button.DROPITEM)) {
				jb.gp.keyToggled(Button.DROPITEM, true);
				evt.setCancelled(true);
				Bukkit.getScheduler().runTaskLater(jb, new Runnable(){

					@Override
					public void run() {
						if (Cooldown.tryCooldown(evt.getPlayer(), "select", 200))
						jb.gp.keyToggled(Button.DROPITEM, false);
					}}, 4l);
				return;
			}
			for (Button b: Button.values()) {
				jb.gp.keyToggled(b, evt.getButtonsPressed().contains(b));
			}				
		}
	}
	

	@EventHandler
	public void leaveGameEvt(PlayerQuitEvent evt) {
		leaveGame(evt.getPlayer());
	}
	@EventHandler
	public void leaveGameEvt(PlayerKickEvent evt) {
		leaveGame(evt.getPlayer());
	}
	private void leaveGame(Player p) {
		System.out.println(CPU.player);
		if (CPU.player == null || !CPU.player.equals(p.getUniqueId()) ) return;
		for (Player pl:Bukkit.getOnlinePlayers()) {
			if (!CPU.player.equals(pl.getUniqueId()))
			pl.sendMessage("You player that you were spectating has left their gameboy.");
		}
		if (!CPU.canRun()) {
			return;
		}
		swinggui.pauseEmulation(false);
		DataOutputStream dostream;
		try {
			dostream = FHandler.getDataOutputStream(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+CPU.player.toString()+"_"+swinggui.curcartname+".st");

			CPU.saveState(dostream);
			dostream.close();
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CPU.severLink();
		Bukkit.getScheduler().runTaskLater(jb, new Runnable(){

			@Override
			public void run() {
				Bukkit.getServer().shutdown();
			}}, 20l);
	}
	public void updateLinkMenu() {
		for (Entry<Integer, RemoteServerLink> e: this.jb.socketio.servers.entrySet()) {
			int serverId = e.getKey();
			String oplayer = e.getValue().player;
			String cart = e.getValue().cart;
			boolean isConnected = e.getValue().isConnected;
			if (Bukkit.getServer().getPort()-25580 == serverId) {
				menu.setSlot(serverId-1, new Icon(Material.WOOL, serverId, (byte)4,ChatColor.YELLOW + "Current Server"));
			} else if (isConnected) {
				menu.setSlot(serverId-1, new Icon(Material.WOOL, serverId, (byte)15,ChatColor.YELLOW + "Server Link in use"));
			}
			else if (cart.equals("null")||oplayer.equals("null")) {
				menu.setSlot(serverId-1, new Icon(Material.WOOL, serverId, (byte)15,ChatColor.YELLOW + "Empty server"));
			} else {
				Icon icon = new Icon(Material.WOOL, serverId, (byte)5,ChatColor.YELLOW + "Link to Server", new String[]{ChatColor.AQUA+"Server: "+ChatColor.YELLOW+String.valueOf(serverId),ChatColor.AQUA+"Game: "+ChatColor.YELLOW+cart,ChatColor.AQUA+"Player: "+ChatColor.YELLOW+oplayer});
				icon.setCallback(new ServerCallback(serverId));
				menu.setSlot(serverId-1, icon);
			}
		}
	}
	private void createLinkMenu(final Player player) {

		if (CPU.isConnected()) return;
		updateLinkMenu();

		menu.show(player);


	}
}
