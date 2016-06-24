package com.sanjay900.jgbe.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.sanjay900.jgbe.emu.FHandler;
import net.tangentmc.nmsUtils.events.PlayerPushedKeyEvent;
import net.tangentmc.nmsUtils.packets.Key;
import net.tangentmc.nmsUtils.utils.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

public class JavaBoyEventHandler implements Listener {
    GameboyPlugin plugin = GameboyPlugin.getInstance();
    //public Menu menu;
    int serverId;
    public String cart = "";

    public JavaBoyEventHandler() {
        serverId = Bukkit.getServer().getPort() - 25580;
        //	menu = new Menu(this.plugin, ChatColor.BLUE+"Gameboy Link Menu",9, null);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent evt) {
        if (plugin.gp != null) {
            Bukkit.getOnlinePlayers().stream().forEach(player -> Bukkit.getOnlinePlayers().stream().forEach(player2 -> player.hidePlayer(player2)));
            evt.getPlayer().setAllowFlight(true);
            evt.getPlayer().setFlying(true);
            evt.getPlayer().getInventory().clear();
            evt.getPlayer().updateInventory();
            Location l = new Location(Bukkit
                    .getWorld("gba"), 76,
                    46.5, 66);
            l.setDirection(new Vector(0, 0, -1));
            evt.getPlayer().teleport(l);
        } else if (!cart.equals("")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.gp != null) {
                    plugin.cpu.severLink();
                }
                plugin.gp = new GameboyPlayer(evt.getPlayer(), cart);
                cart = "";
            }, 40l);
        }

    }

    @EventHandler
    public void hurt(EntityDamageEvent evt) {
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
        onClick(evt.getPlayer(), Action.RIGHT_CLICK_AIR);

    }

    @EventHandler
    public void blockplace(final BlockPlaceEvent evt) {
        evt.setBuild(false);
    }

    @EventHandler
    public void mapclick(final EntityDamageByEntityEvent evt) {
        evt.setCancelled(true);
        if (evt.getDamager() instanceof Player && evt.getEntity() instanceof ItemFrame)
            onClick((Player) evt.getDamager(), Action.LEFT_CLICK_AIR);
    }

    @EventHandler
    public void mapclick(final PlayerInteractAtEntityEvent evt) {
        evt.setCancelled(true);
        onClick(evt.getPlayer(), Action.RIGHT_CLICK_AIR);
    }

    @EventHandler
    public void onClick(final PlayerInteractEvent evt) {
        evt.setCancelled(true);
        onClick(evt.getPlayer(), evt.getAction());

    }

    public void onClick(final Player pl, Action action) {
        if (plugin.gp == null) return;
        ItemStack it = pl.getItemInHand();
        final Key buttonPressed;
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
            pl.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            return;
        } else if (action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK) {

            if (it.hasItemMeta()
                    && it.getItemMeta().hasDisplayName()
                    && it.getItemMeta().getDisplayName()
                    .contains("Start/Select")) {
                buttonPressed = Key.OPEN_INVENTORY;

            } else if (it.hasItemMeta()
                    && it.getItemMeta().hasDisplayName()
                    && it.getItemMeta().getDisplayName()
                    .contains("A/B")) {
                buttonPressed = Key.UNMOUNT;
            } else {
                return;
            }

        } else if (action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK) {

            if (it.hasItemMeta()
                    && it.getItemMeta().hasDisplayName()
                    && it.getItemMeta().getDisplayName()
                    .contains("Start/Select")) {
                buttonPressed = Key.DROP_ITEM;

            } else if (it.hasItemMeta()
                    && it.getItemMeta().hasDisplayName()
                    && it.getItemMeta().getDisplayName()
                    .contains("A/B")) {
                buttonPressed = Key.JUMP;

            } else {
                return;
            }
        } else {
            return;
        }
        plugin.gp.keyToggled(buttonPressed, true);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                plugin.gp.keyToggled(buttonPressed, false);
            }
        }, 10l);

    }

    @EventHandler
    public void pushedKey(final PlayerPushedKeyEvent evt) {
        if (plugin.gp != null) {
            if (evt.getButtons().contains(Key.OPEN_INVENTORY)) {
                if (plugin.gp != null) {
                    plugin.gp.keyToggled(Key.OPEN_INVENTORY, true);
                    evt.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (Cooldown.tryCooldown(evt.getPlayer(), "start", 200))
                            plugin.gp.keyToggled(Key.OPEN_INVENTORY, false);
                    }, 4l);
                }
                return;
            }
            if (evt.getButtons().contains(Key.DROP_ITEM)) {
                plugin.gp.keyToggled(Key.DROP_ITEM, true);
                evt.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                    @Override
                    public void run() {
                        if (Cooldown.tryCooldown(evt.getPlayer(), "select", 200))
                            plugin.gp.keyToggled(Key.DROP_ITEM, false);
                    }
                }, 4l);
                return;
            }
            for (Key b : Key.values()) {
                plugin.gp.keyToggled(b, evt.getButtons().contains(b));
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
        if (plugin.cpu.player == null || !plugin.cpu.player.equals(p.getUniqueId())) return;
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!plugin.cpu.player.equals(pl.getUniqueId())) {
                pl.sendMessage("You player that you were spectating has left their gameboy.");
            }
        }
        if (!plugin.cpu.canRun()) {
            return;
        }
        //Kill the connection and pause emulation. Your not going to restore a connected snapshot
        plugin.cpu.severLink();
        GameboyPlugin.pauseEmulation(false);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                DataOutputStream dostream;
                try {
                    dostream = FHandler.getDataOutputStream(plugin.getDataFolder().getAbsolutePath() + "/../../../gameboy/saves/" + plugin.cpu.player.toString() + "_" + GameboyPlugin.curcartname + ".st");
                    plugin.cpu.saveState(dostream);
                    dostream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.getServer().shutdown();
                    }
                }, 100l);
            }
        }, 20l);
    }

    public void updateLinkMenu() {
        /*for (Entry<Integer, RemoteServerLink> e : this.plugin.socketio.servers.entrySet()) {
            int serverId = e.getKey();
            String oplayer = e.getValue().player;
            String cart = e.getValue().cart;
            boolean isConnected = e.getValue().isConnected;
            if (Bukkit.getServer().getPort() - 25580 == serverId) {
                menu.setSlot(serverId - 1, new Icon(Material.WOOL, serverId, (byte) 4, ChatColor.YELLOW + "Current Server"));
            } else if (isConnected) {
                menu.setSlot(serverId - 1, new Icon(Material.WOOL, serverId, (byte) 15, ChatColor.YELLOW + "Server Link in use"));
            } else if (cart.equals("null") || oplayer.equals("null")) {
                menu.setSlot(serverId - 1, new Icon(Material.WOOL, serverId, (byte) 15, ChatColor.YELLOW + "Empty server"));
            } else {
                Icon icon = new Icon(Material.WOOL, serverId, (byte) 5, ChatColor.YELLOW + "Link to Server", new String[]{ChatColor.AQUA + "Server: " + ChatColor.YELLOW + String.valueOf(serverId), ChatColor.AQUA + "Game: " + ChatColor.YELLOW + cart, ChatColor.AQUA + "Player: " + ChatColor.YELLOW + oplayer});
                icon.setCallback(new ServerCallback(serverId));
                menu.setSlot(serverId - 1, icon);
            }
        }*/
    }

    private void createLinkMenu(final Player player) {
        if (plugin.cpu.isConnected()) return;
        updateLinkMenu();
        //menu.show(player);
    }
}
