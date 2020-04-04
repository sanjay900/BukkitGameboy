package com.sanjay900.jgbe.bukkit;

import com.comphenix.packetwrapper.WrapperPlayServerNamedSoundEffect;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JavaBoyEventHandler implements Listener {
    GameboyPlugin plugin = GameboyPlugin.getInstance();
    KeyListener l;
    private Map<Inventory, Map<ItemStack, Runnable>> interactions = new HashMap<>();
    private HashMap<UUID, Runnable> interactionsText = new HashMap<>();

    public JavaBoyEventHandler() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        l = new KeyListener();
        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        WrapperPlayServerNamedSoundEffect wps = new WrapperPlayServerNamedSoundEffect(event.getPacket());
                        if (wps.getSoundCategory() != EnumWrappers.SoundCategory.MASTER) {
                            event.setCancelled(true);
                        }
                    }
                });
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent evt) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != evt.getPlayer()) {
                player.hidePlayer(plugin, evt.getPlayer());
                evt.getPlayer().hidePlayer(plugin, player);
            }
        }
//        GameboyPlayer gp = plugin.getPlayer(evt.getPlayer());
//        if (gp == null) {
        plugin.listRoms(evt.getPlayer());
        GameboyPlayer.giveItems(evt.getPlayer(), true);
//        }
//        TODO: Spectating could just be done from the arcade itself, and we could just share chat between the arcade and this server
//        if (gp != null) {
//            Bukkit.getOnlinePlayers().stream().forEach(player -> Bukkit.getOnlinePlayers().stream().forEach(player2 -> player.hidePlayer(player2)));
//            evt.getPlayer().setAllowFlight(true);
//            evt.getPlayer().setFlying(true);
//            evt.getPlayer().getInventory().clear();
//            evt.getPlayer().updateInventory();
//            Location l = new Location(Bukkit
//                    .getWorld("gba"), 76,
//                    46.5, 66);
//            l.setDirection(new Vector(0, 0, -1));
//            evt.getPlayer().teleport(l);
//        }
//        Use the bungee messaging system to handle starting a game automatically.
//        else if (!cart.equals("")) {
//            plugin.startPlaying(evt.getPlayer(), cart);
//            cart = "";
//        }

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
        ItemStack it = pl.getItemInHand();
        if (it.hasItemMeta()
                && it.getItemMeta().hasDisplayName()
                && it.getItemMeta().getDisplayName()
                .contains("Quit Game")) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("survival");
            pl.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            return;
        }
        GameboyPlayer gp = plugin.getPlayer(pl);
        if (gp == null) return;
        final Key buttonPressed;
        if (it.hasItemMeta()
                && it.getItemMeta().hasDisplayName()
                && it.getItemMeta().getDisplayName()
                .contains("Connect to another Gameboy")) {
            createLinkMenu(pl);
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR
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
        gp.keyToggled(buttonPressed, true);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                gp.keyToggled(buttonPressed, false);
            }
        }, 10l);

    }

    @EventHandler
    public void pushedKey(final PlayerPushedKeyEvent evt) {
        GameboyPlayer gp = plugin.getPlayer(evt.getPlayer());
        if (gp != null) {
            if (evt.getButtons().contains(Key.OPEN_INVENTORY)) {
                gp.keyToggled(Key.OPEN_INVENTORY, true);
                evt.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Cooldown.tryCooldown(evt.getPlayer(), "start", 200))
                        gp.keyToggled(Key.OPEN_INVENTORY, false);
                }, 4l);
                return;
            }
            if (evt.getButtons().contains(Key.DROP_ITEM)) {
                gp.keyToggled(Key.DROP_ITEM, true);
                evt.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                    @Override
                    public void run() {
                        if (Cooldown.tryCooldown(evt.getPlayer(), "select", 200))
                            gp.keyToggled(Key.DROP_ITEM, false);
                    }
                }, 4L);
                return;
            }
            for (Key b : Key.values()) {
                gp.keyToggled(b, evt.getButtons().contains(b));
            }
        }
    }


    @EventHandler
    public void leaveGameEvt(PlayerQuitEvent evt) {
        GameboyPlayer gp = plugin.getPlayer(evt.getPlayer());
        if (gp != null) gp.stopEmulation();
    }

    @EventHandler
    public void command(PlayerCommandPreprocessEvent evt) {
        if (evt.getMessage().startsWith("/textcallback")) {
            UUID uuid = UUID.fromString(evt.getMessage().split(" ")[1]);
            Runnable c = interactionsText.remove(uuid);
            if (c != null) {
                c.run();
                evt.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void inv(InventoryClickEvent evt) {
        if (interactions.containsKey(evt.getClickedInventory())) {
            evt.setCancelled(true);
        }
        Runnable r = interactions.get(evt.getClickedInventory()).get(evt.getCurrentItem());
        if (r != null) {
            r.run();
            evt.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void invClose(InventoryCloseEvent evt) {
        interactions.remove(evt.getInventory());
    }

    private void createLinkMenu(final Player player) {
        GameboyPlayer gp = plugin.getPlayer(player);
        if (gp == null || gp.getCpu().isConnected()) {
            return;
        }
        int size = this.plugin.listGames().size();
        size = size / 9;
        size = size * 9;
        size = Math.max(9, size);
        Inventory inventory = Bukkit.createInventory(player, size, "Gameboy link menu");
        Map<ItemStack, Runnable> map = new HashMap<>();
        interactions.put(inventory, map);
        boolean foundGame = false;
        for (GameboyPlayer game : this.plugin.listGames()) {
            if (game == gp) {
                continue;
            }
            if (game.canBeLinked() && game.getCurcartname().equals(gp.getCurcartname())) {
                foundGame = true;
                ItemStack it = new ItemStack(Material.DIAMOND, 1);
                ItemMeta meta = it.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Link to " + game.getPlayerName());
                it.setItemMeta(meta);
                inventory.addItem(it);
                map.put(it, () -> {
                    Player playerToConnect = Bukkit.getPlayer(game.player);
                    playerToConnect.sendMessage("The Player " + player.getDisplayName() + " has requested to link up with your game");
                    TextComponent t = new TextComponent(new ComponentBuilder("Click to accept").create());
                    t.setUnderlined(true);
                    t.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    UUID acceptID = UUID.randomUUID();
                    UUID denyID = UUID.randomUUID();
                    interactionsText.put(acceptID, () -> {
                        playerToConnect.sendMessage("You have accepted that request. Your games are now linked together.");
                        player.sendMessage(playerToConnect.getDisplayName() + " has accepted linking to your game. Your games are now linked together.");
                        gp.link(game);
                        interactionsText.remove(acceptID);
                        interactionsText.remove(denyID);
                    });
                    t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/textcallback "+acceptID));
                    t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept request").color(net.md_5.bungee.api.ChatColor.GREEN).create()));
                    playerToConnect.spigot().sendMessage(t);
                    t = new TextComponent(new ComponentBuilder("Click to deny").create());
                    t.setUnderlined(true);
                    t.setColor(net.md_5.bungee.api.ChatColor.RED);

                    interactionsText.put(denyID,()->{
                        playerToConnect.sendMessage("You have denied that request");
                        player.sendMessage(playerToConnect.getDisplayName() + " has denied linking to your game");
                        interactionsText.remove(acceptID);
                        interactionsText.remove(denyID);
                    });
                    t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/textcallback "+denyID));
                    t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny request").color(net.md_5.bungee.api.ChatColor.RED).create()));
                    playerToConnect.spigot().sendMessage(t);
                });
            }
        }
        if (!foundGame) {
            player.sendMessage("Unfortunately no players are playing the same game as you currently. Try again later.");
            return;
        }
        player.openInventory(inventory);
    }
}
